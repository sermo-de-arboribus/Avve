package avve.services;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.Logger;
import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.w3c.dom.DOMImplementation;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.converters.DOMConverter;

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
		System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
		
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

	public void combineXrffFiles(Collection<String> classes)
	{
		logger.info(infoMessagesBundle.getString("avve.services.combiningXrffFiles"));
		
		LocalDateTime timePoint = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");
		
		runXsltAndWriteOutputFile(classes, this.getClass().getClassLoader().getResourceAsStream("xml/Merge_statistics_files.xsl"), new File("output/result_" + timePoint.format(formatter) + ".xrff"));
	}

	public void createCombinedMultiClassFile(Collection<String> collectionOfClassNames)
	{
		logger.info(infoMessagesBundle.getString("avve.services.generatingMultiClassArffFile"));
		
		// build a set of class labels (incoming "collectionOfClassNames" contains duplicates)
		Set<String> uniqueClassLabels = new HashSet<String>();
		
		for(String commaSeparatedClassString : collectionOfClassNames)
		{
			String[] classLabels = commaSeparatedClassString.split(",");
			
			for(String classLabel : classLabels)
			{
				uniqueClassLabels.add(classLabel);
			}
		}
		
		LocalDateTime timePoint = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");
		
		runXsltAndWriteOutputFile(uniqueClassLabels, this.getClass().getClassLoader().getResourceAsStream("xml/Merge_multiclass_files.xsl"), new File("output/result_" + timePoint.format(formatter) + ".arff"));
	}
	
	public String extractTextFromXhtml(Document contentDocument)
	{
		StringWriter stringWriter = new StringWriter();
		StreamResult result = new StreamResult(stringWriter);
		
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		
		synchronized(this)
		{
			// we cache the xslt stylesheet in a static variable, so we only need to compile it once
			if(null == stylesheet)
			{
					Source xsltSource = new StreamSource(new ByteArrayInputStream(textExtractionFromHtml.getBytes(StandardCharsets.UTF_8)));
					try
					{
						stylesheet = transformerFactory.newTemplates(xsltSource);
					}
					catch (TransformerConfigurationException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
		}
		
		Transformer transformer;
		try
		{
			transformer = stylesheet.newTransformer();
			Source xmlSource = new DOMSource(DOMConverter.convert(contentDocument, getDefaultDOMImplementation()).getDocumentElement());
			transformer.transform(xmlSource, result);
		}
		catch (NullPointerException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (TransformerConfigurationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (TransformerException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return stringWriter.toString();
	}
	
    private synchronized DOMImplementation getDefaultDOMImplementation()
    {
        if (domImplementation == null)
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            try
            {
            	domImplementation = factory.newDocumentBuilder().getDOMImplementation();

            }
            catch (ParserConfigurationException exc)
            {
            	// TODO: get message from resource bundle
                logger.error("Unable to get default DOM implementation", exc);
            }
        }

        return domImplementation;
    }
    
	private void runXsltAndWriteOutputFile(Collection<String> classes, InputStream xsltStream, File outputFile)
	{
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		InputStream xmlDummyInputStream = this.getClass().getClassLoader().getResourceAsStream("xml/dummy.xml");
		
		try
		{	
			Path directory = Paths.get("output/stats");
			Transformer transformer = transformerFactory.newTransformer(new StreamSource(xsltStream));
			transformer.setParameter("sourceFolder", directory.toAbsolutePath().toUri());
			transformer.setParameter("classLabels", String.join(",", classes));
			transformer.transform(new StreamSource(xmlDummyInputStream), new StreamResult(outputFile));
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
	
	private static String textExtractionFromHtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + 
			"<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + 
			"<xsl:output method=\"text\" encoding=\"UTF-8\"/>" +
			"	<xsl:template match=\"/\">" +
			"		<xsl:apply-templates /> " + 
			"	</xsl:template>" +
			"	<xsl:template match=\"*[local-name() = 'p'] | *[local-name() = 'div']\">" +
			"		<xsl:text> </xsl:text><xsl:apply-templates/><xsl:text> </xsl:text>"+
			"	</xsl:template>"+
	
			"	<xsl:template match=\"*[local-name() = 'br']\">"+
			"		<xsl:text>&#xD;&#xA; </xsl:text>" +
			"	</xsl:template>"+
	
			"	<xsl:template match=\"text()\">" +
			"		<xsl:value-of select=\".\"/>" +
			"	</xsl:template>" +
			"</xsl:stylesheet>";
	
	private static Templates stylesheet = null;
	private static DOMImplementation domImplementation = null;
}