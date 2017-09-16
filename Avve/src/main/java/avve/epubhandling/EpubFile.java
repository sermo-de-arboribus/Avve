package avve.epubhandling;

import avve.services.FileService;
import avve.services.XmlService;
import net.sf.saxon.s9api.XPathCompiler;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nu.xom.*;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;


public class EpubFile implements Serializable
{
	private static final long serialVersionUID = 3022250504362756894L;
	
	// constants
	private static final int BUFFER_SIZE = 8192;
	private static final XPathContext epubContainerNamespace = new XPathContext("cnt", "urn:oasis:names:tc:opendocument:xmlns:container");
	private static final XPathContext opfNamespace = new XPathContext("opf", "http://www.idpf.org/2007/opf");
	private static final XPathContext ncxNamespace = new XPathContext("ncx", "http://www.daisy.org/z3986/2005/ncx/");
	private static final ResourceBundle errorMessagesBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	// private instance fields
	private String documentId;
	private String filePath;
	transient private FileService fileService;
	private long fileSize;
	private String language;
	transient private Logger logger;
	private int numberOfChapters;
	private int numberOfTocItems;
	private String pathToTocFile;
	private int depthOfToc;
	transient private XmlService xmlService;
	
	/**
	 * Constructor, checks if file exists and therefore might throw an IOException
	 * @param filePath The filepath where the EPUB file is to be found
	 * @param fileService The file service to use for file system access (may be mocked in unit testing)
	 * @throws IOException If the EPUB file doesn't exist, an IOException is thrown
	 */
	public EpubFile(String filePath, FileService fileService, Logger logger) throws IOException
	{
		// ensure that file exists
		if(!fileService.fileExists(filePath) || fileService.isDirectory(filePath))
		{
		    throw new FileNotFoundException(errorMessagesBundle.getString("EpubFileNotFound"));
		}
		File file = fileService.newFileObject(filePath);
		fileSize = file.length();
		this.filePath = filePath;
		this.fileService = fileService;
		this.logger = logger;
		this.xmlService = new XmlService(fileService, logger);
		opfNamespace.addNamespace("dc", "http://purl.org/dc/elements/1.1/");
	}
	
	public String extractPlainText()
	{
		StringBuffer sb = new StringBuffer();
		
		try
		{
		    // unzip to temp folder
		    String tempDir = FilenameUtils.concat(System.getProperty("java.io.tmpdir"), "avve");
		    fileService.createDirectory(tempDir);
		    unzipEpubToTempFolder(tempDir);
		    
		    // read text from Epub
			String pathToOebpsFile = getOebpsFilePath(tempDir);
			List<String> pathsToContentFiles = getPlainTextFromContentFiles(pathToOebpsFile);
			for(String chapter : pathsToContentFiles)
			{
				sb.append(chapter);
				sb.append(System.lineSeparator());
			}
			
			// determine table of contents (TOC) structure (NOTE: the pathToTocFile instance variable is set by getPlainTextFromContentFiles(), so we can only work on pathToTocFile afterwards)
			String absolutePathToTocFile = FilenameUtils.concat(new File(pathToOebpsFile).getParent(), pathToTocFile);
			determineTocStructure(absolutePathToTocFile);
			
			// clear temp folder
			fileService.clearFolder(tempDir);
		}
		catch (IOException exc)
		{
			logger.error(exc.getLocalizedMessage(), exc);
		}
		catch (ParsingException exc)
		{
			logger.error(exc.getLocalizedMessage(), exc);
		}
		catch (SAXException exc)
		{
			logger.error(exc.getLocalizedMessage(), exc);
		}
		
		return sb.toString();
	}

	public int getDepthOfToc()
	{
		return depthOfToc;
	}
	
	public String getDocumentId()
	{
		return documentId;
	}
	
	public long getFileSize()
	{
		return fileSize;
	}
	
	public String getLanguageCode()
	{
		if(language == null)
		{
			return "";
		}
		return language;
	}
	
	public int getNumberOfChapters()
	{
		return numberOfChapters;
	}

	public int getNumberOfTocItems()
	{
		return numberOfTocItems;
	}

	private void determineTocStructure(String pathToTocFile)
	{
		if(null != pathToTocFile && pathToTocFile.length() > 0)
		{
			String extension = FilenameUtils.getExtension(pathToTocFile);
			switch(extension)
			{
				case "ncx":
					try(InputStream instream = fileService.createFileInputStream(pathToTocFile))
					{
						Document parsedTocFile = xmlService.build(instream);
						
						numberOfTocItems = parsedTocFile.query("/ncx:ncx/ncx:navMap//ncx:navPoint", ncxNamespace).size();
						depthOfToc = Integer.parseInt(xmlService.evaluateXpath(parsedTocFile, "max(//ncx:navPoint/count(ancestor::*))", "ncx", ncxNamespace.lookup("ncx"))) - 1;
					}
					catch (FileNotFoundException exc)
					{
						logger.error(exc.getLocalizedMessage(), exc);
					}
					catch (IOException exc)
					{
						logger.error(exc.getLocalizedMessage(), exc);
					}
					catch (NumberFormatException exc)
					{
						logger.error(exc.getLocalizedMessage(), exc);
					}
					break;
					
				case "html":
				case "xhtml":
				case "htm":
					try(InputStream instream = fileService.createFileInputStream(pathToTocFile))
					{
						Document parsedTocFile = xmlService.build(instream);
						numberOfTocItems = parsedTocFile.query("/*[local-name() = 'nav']//*[local-name() = 'li']", null).size();
						depthOfToc = Integer.parseInt(xmlService.evaluateXpath(parsedTocFile, "max(//*[local-name() = 'li']/count(ancestor::*[local-name() = 'li']))", "", "")) + 1;					
					}
					catch (FileNotFoundException exc)
					{
						logger.error(exc.getLocalizedMessage(), exc);
					}
					catch (IOException exc)
					{
						logger.error(exc.getLocalizedMessage(), exc);
					}
					catch (NumberFormatException exc)
					{
						logger.error(exc.getLocalizedMessage(), exc);
					}
					break;
					
				default:
					numberOfTocItems = -1;
					depthOfToc = -1;					
			}
		}
		else
		{
			logger.error(errorMessagesBundle.getString("avve.epubhandling.pathToTocFileUnknown"));
		}
	}
	
	private List<String> getPlainTextFromContentFiles(String pathToOebpsFile) throws IOException, ParsingException, SAXException
	{
		logger.trace(infoMessagesBundle.getString("startReadingOebpsSpine") + " " + pathToOebpsFile);
		
		List<String> resultList = new ArrayList<String>();
		
		String oebpsDirectoryPath = FilenameUtils.getFullPath(pathToOebpsFile);
		
		try(InputStream instream = fileService.createFileInputStream(pathToOebpsFile))
		{
			Document parsedOebpsFile = xmlService.build(instream);
			determineLanguage(parsedOebpsFile);
			determineDocumentId(parsedOebpsFile);
			Nodes spine = parsedOebpsFile.query("/opf:package/opf:spine/opf:itemref/@idref", opfNamespace);
			numberOfChapters = spine.size();

			logger.trace(String.format(infoMessagesBundle.getString("numberOfSpineItemsFound"), spine.size()));
			
			// iterate over all spine items in document order
			for(int i = 0; i < spine.size(); i++)
			{
				Attribute spineEntry = (Attribute)spine.get(i);
				Nodes contentFilepathNodes = parsedOebpsFile.query("/opf:package/opf:manifest/opf:item[@id = '" + spineEntry.getValue() + "']", opfNamespace);
				// check if nodes contains exactly one item, if not: log a debug message
				if(contentFilepathNodes.size() != 1)
				{
					logger.debug(errorMessagesBundle.getString("CantDetermineOebpsEntryForItemId"));
				}
				
				// open the current content (HTML) document
				String relativeContentFilepath = ((Element)contentFilepathNodes.get(0)).getAttributeValue("href");
				String contentFilepath = FilenameUtils.concat(oebpsDirectoryPath, relativeContentFilepath);
				
				logger.trace(String.format(infoMessagesBundle.getString("workingOnContentItem"), contentFilepath));
			
				try(InputStream inputStream = fileService.createFileInputStream(contentFilepath))
				{
					Document contentDocument = xmlService.build(inputStream);
					String plainText = xmlService.extractTextFromXhtml(contentDocument);
					resultList.add(plainText);
				}
			}
			
			// in EPUB 3 the nav-property must be used, in EPUB 2 we need to look for the item that is declared toc item by the spine's @toc attribute
			Nodes tocItems = parsedOebpsFile.query("/opf:package/opf:manifest/opf:item[@properties = 'nav']/@href | "
					+ "/opf:package/opf:manifest/opf:item[@id = /opf:package/opf:spine/@toc]/@href", opfNamespace);
			
			if(tocItems.size() > 0)
			{
				pathToTocFile = tocItems.get(0).getValue();	
			}
			else
			{
				pathToTocFile = "";
			}
		}
		return resultList;
	}

	// Try to determine the document ID from the EPUB's metadata and store it in the documentId instance variable. If no ID is found, a timestamp is used as the ID instead
	private void determineDocumentId(final Document parsedOebpsFile)
	{
		Nodes uniqueIdentifierNodes = parsedOebpsFile.query("/opf:package/opf:metadata/*[@id = /opf:package/@unique-identifier]", opfNamespace);
		if((uniqueIdentifierNodes != null) && (uniqueIdentifierNodes.size() > 0) && (uniqueIdentifierNodes.get(0).getValue().length() > 1));
		{
			try
			{
				String uniqueIdentifier = uniqueIdentifierNodes.get(0).getValue();
				logger.trace(String.format(infoMessagesBundle.getString("avve.epubhandling.uniqueIdentifierFound"), uniqueIdentifier));
				setDocumentId(uniqueIdentifier);
			}
			catch(Exception exc)
			{
				logger.error(errorMessagesBundle.getString("avve.epubhandling.documentIdDeterminationException"));
				setDocumentId("" + (System.currentTimeMillis() / 100));
			}
		}
	}
	
	private void determineLanguage(final Document parsedOebpsFile)
	{
		Nodes dublinCoreLanguage = parsedOebpsFile.query("/opf:package/opf:metadata/dc:language", opfNamespace);
		if((dublinCoreLanguage != null) && (dublinCoreLanguage.size() > 0) && (dublinCoreLanguage.get(0).getValue().length() > 1));
		{
			logger.trace(String.format(infoMessagesBundle.getString("avve.epubhandling.languageDetermination"), dublinCoreLanguage.get(0).getValue()));
			try
			{
				language = dublinCoreLanguage.get(0).getValue().substring(0,2).toLowerCase();	
			}
			catch(StringIndexOutOfBoundsException exc)
			{
				logger.error(String.format(errorMessagesBundle.getString("avve.epubhandling.languageDeterminationException"), dublinCoreLanguage.get(0).getValue().length()));
			}
		}
	}

	private String getOebpsFilePath(final String epubDir) throws IOException, ParsingException, SAXException
	{
		Document parsedContainerFile = xmlService.build(FilenameUtils.concat(epubDir, "META-INF/container.xml"));
		if(null == parsedContainerFile)
		{
			throw new IOException(errorMessagesBundle.getString("RootfileEntryNotFoundInEpubContainer"));
		}
		Nodes rootfileNodes = parsedContainerFile.query("/cnt:container/cnt:rootfiles/cnt:rootfile[@media-type='application/oebps-package+xml']", epubContainerNamespace);
		if(rootfileNodes.size() == 0)
		{
			throw new IOException(errorMessagesBundle.getString("RootfileEntryNotFoundInEpubContainer"));
		}
		Node rootFileNode = rootfileNodes.get(0);
		
		// the absolute filepath to the OEBPS file is the tempDir plus the full-path given in the container-file
		return FilenameUtils.concat(epubDir, ((Element)rootFileNode).getAttribute("full-path").getValue());
	}
	
	private void setDocumentId(String documentId)
	{
		this.documentId = documentId;
	}
	
	private void unzipEpubToTempFolder(final String tempDir) throws IOException
	{
        if (!fileService.fileExists(tempDir))
        {
        	fileService.createDirectory(tempDir);
        }
        ZipInputStream zippedInputStream = new ZipInputStream(new FileInputStream(filePath));
        ZipEntry nextZipEntry = zippedInputStream.getNextEntry();

        while (nextZipEntry != null)
        {
            String filePath = FilenameUtils.concat(tempDir, nextZipEntry.getName());
            if (nextZipEntry.isDirectory())
            {
                // handle directories
                fileService.createDirectory(filePath);
            }
            else 
            {
                // handle files
                unzipSingleFile(zippedInputStream, filePath);
            }
            zippedInputStream.closeEntry();
            nextZipEntry = zippedInputStream.getNextEntry();
        }
        zippedInputStream.close();
	}

	private void unzipSingleFile(final ZipInputStream zippedInputStream, final String destinationPath) throws IOException
	{
		try(BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileService.createFileOutputStream(destinationPath)))
		{
			byte[] bytesIn = new byte[BUFFER_SIZE];
			int read = 0;
	        while ((read = zippedInputStream.read(bytesIn)) != -1)
	        {
	        	bufferedOutputStream.write(bytesIn, 0, read);
	        }
		}
	}
}