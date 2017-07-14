package avve.epubhandling;

import avve.services.FileService;
import avve.services.XmlService;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.XPathContext;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;


public class EpubFile
{
	// constants
	private static final int BUFFER_SIZE = 8192;
	private static final XPathContext epubContainerNamespace = new XPathContext("cnt", "urn:oasis:names:tc:opendocument:xmlns:container");
	private static final XPathContext opfNamespace = new XPathContext("opf", "http://www.idpf.org/2007/opf");
	private static final ResourceBundle errorMessagesBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	// private instance fields
	private String filePath;
	private String language;
	private FileService fileService;
	private Logger logger;
	private XmlService xmlService;
	
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
			}
			
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
	
	public String getLanguageCode()
	{
		if(language == null)
		{
			return "";
		}
		return language;
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
			Nodes spine = parsedOebpsFile.query("/opf:package/opf:spine/opf:itemref/@idref", opfNamespace);

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
					// simply get the text value of the HTML file, so HTML tags get automatically stripped.
					resultList.add(contentDocument.getValue());
				}
			}
		}
		return resultList;
	}

	private void determineLanguage(final Document parsedOebpsFile)
	{
		Nodes dublinCoreLanguage = parsedOebpsFile.query("/opf:package/opf:metadata/dc:language", opfNamespace);
		if(dublinCoreLanguage != null && dublinCoreLanguage.size() > 0)
		{
			language = dublinCoreLanguage.get(0).getValue().substring(0,2).toLowerCase();
		}
	}

	private String getOebpsFilePath(final String epubDir) throws IOException, ParsingException, SAXException
	{		
		// use a non-validating reader that ignores external dtds... 
		// TODO: may lead to problems with parsing of HTML entities like &auml; Better to provide a local version for HTML DTDs

		Document parsedContainerFile = xmlService.build(FilenameUtils.concat(epubDir, "META-INF/container.xml"));
		Nodes rootfileNodes = parsedContainerFile.query("/cnt:container/cnt:rootfiles/cnt:rootfile[@media-type='application/oebps-package+xml']", epubContainerNamespace);
		if(rootfileNodes.size() == 0)
		{
			throw new IOException(errorMessagesBundle.getString("RootfileEntryNotFoundInEpubContainer"));
		}
		Node rootFileNode = rootfileNodes.get(0);
		
		// the absolute filepath to the OEBPS file is the tempDir plus the full-path given in the container-file
		return FilenameUtils.concat(epubDir, ((Element)rootFileNode).getAttribute("full-path").getValue());
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