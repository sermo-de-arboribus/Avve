package avve.extractor;

import avve.epubhandling.EbookContentData;
import avve.services.FileService;

import java.io.*;

import nu.xom.*;

public class XrffFile
{
	public XrffFile(final String filePath, final FileService fileService)
	{
		this.filePath = filePath;
		this.fileService = fileService;
	}
	
	public void saveEbookContentData(EbookContentData content)
	{
		FileOutputStream outputStream = null;
		try
		{
			outputStream = fileService.createFileOutputStream(filePath);
			OutputStreamWriter writer = new OutputStreamWriter(outputStream);
			Element root = new Element("dataset");
			root.addAttribute(new Attribute("name", "avve"));
			Document xmlOutputDocument = new Document(root);
			
			writeHeader(xmlOutputDocument);
			writeBody(root, content);
			
			// serialize XML Document and write it to the output stream
			String xmlOutputString = xmlOutputDocument.toXML();
			writer.write(xmlOutputString);
			writer.flush();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			fileService.safeClose(outputStream);
		}
	}
	
	private void writeBody(Element root, EbookContentData content)
	{
		Element bodyElement = new Element("body");
		root.appendChild(bodyElement);
		
		Element instancesElement = new Element("instances");
		bodyElement.appendChild(instancesElement);
		
		Element instanceElement = new Element("instance");
		instancesElement.appendChild(instanceElement);
		
		Element lemmasElement = new Element("value");
		lemmasElement.appendChild(String.join(" ", content.getLemmas()));
		instanceElement.appendChild(lemmasElement);
		
		Element partsOfSpeechElement = new Element("value");
		partsOfSpeechElement.appendChild(String.join(" ", content.getPartsOfSpeech()));
		instanceElement.appendChild(partsOfSpeechElement);
		
		Element wordsPerSentenceElement = new Element("value");
		wordsPerSentenceElement.appendChild("" + ((double)content.getTokens().length / (double)content.getSentences().length));
		instanceElement.appendChild(wordsPerSentenceElement);
		
		Element lemmasPerSentenceElement = new Element("value");
		lemmasPerSentenceElement.appendChild("" + ((double)content.getLemmas().length / (double)content.getSentences().length));
		instanceElement.appendChild(lemmasPerSentenceElement);
		
		Element uniquePartsOfSpeech = new Element("value");
		uniquePartsOfSpeech.appendChild("" + content.getPartsOfSpeech().length);
		instanceElement.appendChild(uniquePartsOfSpeech);
		
		Element uniqueNumberOfWords = new Element("value");
		uniqueNumberOfWords.appendChild("" + content.getPartsOfSpeech().length);
		instanceElement.appendChild(uniqueNumberOfWords);

		Element classElement = new Element("value");
		classElement.appendChild(content.getWarengruppe());
		instanceElement.appendChild(classElement);
	}

	private void writeHeader(Element root)
	{
		Element headerElement = new Element("header");
		root.appendChild(headerElement);
		
		Element attributes = new Element("attributes");
		headerElement.appendChild(attributes);
		
		Element lemmas = new Element("attribute");
		lemmas.addAttribute(new Attribute("name", "lemmas"));
		lemmas.addAttribute(new Attribute("type", "string"));
		attributes.appendChild(lemmas);
		
		Element partsOfSpeech = new Element("attribute");
		partsOfSpeech.addAttribute(new Attribute("name", "partsOfSpeech"));
		partsOfSpeech.addAttribute(new Attribute("type", "string"));
		attributes.appendChild(partsOfSpeech);
		
		Element wordsPerSentence = new Element("attribute");
		wordsPerSentence.addAttribute(new Attribute("name", "wordsPerSentence"));
		wordsPerSentence.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(wordsPerSentence);
		
		Element lemmasPerSentence = new Element("attribute");
		lemmasPerSentence.addAttribute(new Attribute("name", "lemmasPerSentence"));
		lemmasPerSentence.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(lemmasPerSentence);
		
		Element uniquePartsOfSpeech = new Element("attribute");
		uniquePartsOfSpeech.addAttribute(new Attribute("name", "uniquePartsOfSpeech"));
		uniquePartsOfSpeech.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(uniquePartsOfSpeech);
		
		Element uniqueNumberOfWords = new Element("attribute");
		uniqueNumberOfWords.addAttribute(new Attribute("name", "uniqueNumberOfWords"));
		uniqueNumberOfWords.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(uniqueNumberOfWords);

		Element classElement = new Element("attribute");
		classElement.addAttribute(new Attribute("class", "yes"));
		classElement.addAttribute(new Attribute("name", "class"));
		classElement.addAttribute(new Attribute("type", "nominal"));
		attributes.appendChild(classElement);
		
		// TODO: Add labels to classElement
		
	}

	private final String filePath;
	private final FileService fileService;
	
	private static final String dtd = "<!DOCTYPE dataset " + System.lineSeparator() + 
			"[" + System.lineSeparator() + 
			"<!ELEMENT dataset (header,body)>" + System.lineSeparator() + 
			"<!ATTLIST dataset name CDATA #REQUIRED>" + System.lineSeparator() + 
			"<!ATTLIST dataset version CDATA \"3.5.4\">" + System.lineSeparator() + 
			System.lineSeparator() + 
			"<!ELEMENT header (notes?,attributes)>" + System.lineSeparator() + 
			"<!ELEMENT body (instances)>" + System.lineSeparator() + 
			"<!ELEMENT notes ANY>   <!--  comments, information, copyright, etc. -->" + System.lineSeparator() + 
			System.lineSeparator() + 
			"<!ELEMENT attributes (attribute+)>" + System.lineSeparator() + 
			"<!ELEMENT attribute (labels?,metadata?,attributes?)>" + System.lineSeparator() + 
			"<!ATTLIST attribute name CDATA #REQUIRED>" + System.lineSeparator() + 
			"<!ATTLIST attribute type (numeric|date|nominal|string|relational) #REQUIRED>" + System.lineSeparator() + 
			"<!ATTLIST attribute format CDATA #IMPLIED>" + System.lineSeparator() + 
			"<!ATTLIST attribute class (yes|no) \"no\">" + System.lineSeparator() + 
			"<!ELEMENT labels (label*)>   <!-- only for type \"nominal\" -->" + System.lineSeparator() + 
			"<!ELEMENT label ANY>" + System.lineSeparator() + 
			"<!ELEMENT metadata (property*)>" + System.lineSeparator() + 
			"<!ELEMENT property ANY>" + System.lineSeparator() + 
			"<!ATTLIST property name CDATA #REQUIRED>" + System.lineSeparator() + 
			System.lineSeparator() + 
			" <!ELEMENT instances (instance*)>" + System.lineSeparator() + 
			"<!ELEMENT instance (value*)>" + System.lineSeparator() + 
			"<!ATTLIST instance type (normal|sparse) \"normal\">" + System.lineSeparator() + 
			"<!ATTLIST instance weight CDATA #IMPLIED>" + System.lineSeparator() + 
			"<!ELEMENT value (#PCDATA|instances)*>" + System.lineSeparator() + 
			"<!ATTLIST value index CDATA #IMPLIED>   <!-- 1-based index (only used for instance format \"sparse\") -->" + System.lineSeparator() + 
			"<!ATTLIST value missing (yes|no) \"no\">" + System.lineSeparator() + 
			"]" + System.lineSeparator() + 
			">";
}