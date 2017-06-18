package avve.epubhandling;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;

import avve.services.FileService;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.XPathContext;

public class EpubFile
{
	// constants
	private static final XPathContext epubContainerNamespace = new XPathContext("cnt", "urn:oasis:names:tc:opendocument:xmlns:container");
	private static final XPathContext opfNamespace = new XPathContext("opf", "http://www.idpf.org/2007/opf");
	private static final ResourceBundle errorMessagesBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	// private instance fields
	private String filePath;
	private FileService fileService;
	private Logger logger;
	
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
	}
	
	public String extractPlainText()
	{
		StringBuffer sb = new StringBuffer();
		
		ZipFile zippedEpub = null;
		try
		{
			// retrieve the root file of this EPUB container
			zippedEpub = new ZipFile(filePath);
			String pathToOebpsFile = getOebpsFilePath(zippedEpub);
			List<String> pathsToContentFiles = getContentFilePaths(zippedEpub, pathToOebpsFile);
			for(String chapter : pathsToContentFiles)
			{
				sb.append(chapter);
			}
		}
		catch (IOException exc)
		{
			logger.error(exc.getLocalizedMessage(), exc);
		}
		catch (ParsingException exc)
		{
			logger.error(exc.getLocalizedMessage(), exc);
		}
		finally
		{
			if(null != zippedEpub)
			{
				fileService.safeClose(zippedEpub);
			}
		}
		
		return sb.toString();
	}

	private List<String> getContentFilePaths(ZipFile zippedEpub, String pathToOebpsFile) throws IOException, ParsingException
	{
		logger.trace(infoMessagesBundle.getString("startReadingOebpsSpine") + " " + pathToOebpsFile);
		
		List<String> resultList = new ArrayList<String>();
		
		ZipEntry oebpsFile = zippedEpub.getEntry(pathToOebpsFile);
		String oebpsDirectoryPath = FilenameUtils.getFullPath(pathToOebpsFile);
		
		Builder xmlParser = new Builder();
		
		Document parsedOebpsFile = xmlParser.build(zippedEpub.getInputStream(oebpsFile));
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
		
			ZipEntry contentFile = zippedEpub.getEntry(contentFilepath);
			InputStream inputStream = zippedEpub.getInputStream(contentFile);
			Document contentDocument = xmlParser.build(inputStream);
			// simply get the text value of the HTML file, so HTML tags get automatically stripped.
			resultList.add(contentDocument.getValue());
		}
		
		return resultList;
	}

	private String getOebpsFilePath(ZipFile zippedEpub) throws IOException, ParsingException
	{
		ZipEntry zippedContainerFile = zippedEpub.getEntry("META-INF/container.xml");
		Builder xmlParser = new Builder();
		Document parsedContainerFile = xmlParser.build(zippedEpub.getInputStream(zippedContainerFile));
		Nodes rootfileNodes = parsedContainerFile.query("/cnt:container/cnt:rootfiles/cnt:rootfile[@media-type='application/oebps-package+xml']", epubContainerNamespace);
		if(rootfileNodes.size() == 0)
		{
			throw new IOException(errorMessagesBundle.getString("RootfileEntryNotFoundInEpubContainer"));
		}
		Node rootFileNode = rootfileNodes.get(0);
		
		return ((Element)rootFileNode).getAttribute("full-path").getValue();
	}
}