package avve.services;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.Logger;
import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;

public class XmlService
{
	private static final ResourceBundle errorMessagesBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	private Builder xomXmlParser;
	private CatalogResolver xmlCatalogResolver;
	private FileService fileService;
	private Logger logger;

	public XmlService(FileService fileService, Logger logservice)
	{
		this.fileService = fileService;
		this.logger = logservice;
		
		// configure XML resolver with local OASIS catalog handling
		CatalogManager xmlCatalogManager = new CatalogManager();
		this.xmlCatalogResolver = new CatalogResolver(xmlCatalogManager);
		XMLReader xmlReader = null;
		try
		{
			xmlReader = XMLReaderFactory.createXMLReader();
		}
		catch (SAXException e)
		{
			logger.error(errorMessagesBundle.getString("XmlReaderInstantiationError"));
			e.printStackTrace();
		}
		xmlReader.setEntityResolver(xmlCatalogResolver);
		xomXmlParser = new Builder(xmlReader);
	}
	
	public Document build(InputStream inStream)
	{
		try
		{
			Document xomDoc = xomXmlParser.build(inStream);
			return xomDoc;
		}
		catch (ParsingException | IOException | NullPointerException exc)
		{
			logger.error(exc.getLocalizedMessage());
			exc.printStackTrace();
			return null;
		}
	}
	
	public Document build(String filepath)
	{
		InputStream inStream = null;
		try
		{
			inStream = fileService.createFileInputStream(filepath);
		}
		catch (FileNotFoundException exc)
		{
			logger.error(exc.getLocalizedMessage());
			exc.printStackTrace();
		}
		return build(inStream);
	}

	public void combineXrffFiles()
	{
		logger.info(infoMessagesBundle.getString("avve.services.combiningXrffFiles"));
		
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		InputStream xsltStream = this.getClass().getClassLoader().getResourceAsStream("xml/Merge_statistics_files.xsl");
		InputStream xmlDummyInputStream = this.getClass().getClassLoader().getResourceAsStream("xml/dummy.xml");
		
		try
		{
			Path directory = Paths.get("output/stats");
			Transformer transformer = transformerFactory.newTransformer(new StreamSource(xsltStream));
			transformer.setParameter("sourceFolder", directory.toAbsolutePath().toUri());
			transformer.transform(new StreamSource(xmlDummyInputStream), new StreamResult(new File("output/result_" + System.currentTimeMillis())));
		}
		catch(TransformerConfigurationException exc)
		{
			logger.error(exc.getLocalizedMessage(), exc);
		}
		catch(TransformerException exc)
		{
			logger.error(exc.getLocalizedMessage(), exc);
		}
	}
}