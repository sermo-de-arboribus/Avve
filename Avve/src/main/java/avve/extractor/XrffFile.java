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
			//OutputStreamWriter writer = new OutputStreamWriter(outputStream);
			Element root = new Element("dataset");
			root.addAttribute(new Attribute("name", "avve"));
			Document xmlOutputDocument = new Document(root);
			
			writeHeader(root);
			writeBody(root, content);
			
			// workaround for internal DTD subset, see http://www.xom.nu/tutorial.xhtml
			Builder builder = new Builder();
			Document tempDoc = builder.build(dtd, null);
			DocType doctype = tempDoc.getDocType();
			doctype.detach();
			xmlOutputDocument.setDocType(doctype);
			
			// serialize XML Document and write it to the output stream
			
			Serializer serializer = new Serializer(outputStream, "UTF-8");
			serializer.setIndent(4);
		 	serializer.setMaxLength(256);
		 	serializer.write(xmlOutputDocument);  
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (ValidityException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (ParsingException e)
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
		Comment lemmasComment = new Comment("lemmas");
		lemmasElement.appendChild(lemmasComment);
		StringBuffer lemmaSerializer = new StringBuffer();
		for(int i = 0; i < content.getLemmas().length; i++)
		{
			lemmaSerializer.append("[" + i + "] ");
			lemmaSerializer.append(String.join(" ", content.getLemmas()[i]));
			lemmaSerializer.append(System.lineSeparator());
		}
		lemmasElement.appendChild(lemmaSerializer.toString());
		instanceElement.appendChild(lemmasElement);
		
		Element partsOfSpeechElement = new Element("value");
		Comment partsOfSpeechComment = new Comment("parts of speech tags");
		partsOfSpeechElement.appendChild(partsOfSpeechComment);
		StringBuffer posSerializer = new StringBuffer();
		for(int i = 0; i < content.getPartsOfSpeech().length; i++)
		{
			posSerializer.append("[" + i + "] ");
			posSerializer.append(String.join(" ", content.getPartsOfSpeech()[i]));
			posSerializer.append(System.lineSeparator());
		}
		partsOfSpeechElement.appendChild(posSerializer.toString());
		instanceElement.appendChild(partsOfSpeechElement);
		
		Element wordsPerSentenceElement = new Element("value");
		Comment wordsPerSentenceComment = new Comment("words per sentence");
		wordsPerSentenceElement.appendChild(wordsPerSentenceComment);
		wordsPerSentenceElement.appendChild("" + ((double)content.getNumberOfTokens() / (double)content.getSentences().length));
		instanceElement.appendChild(wordsPerSentenceElement);
		
		Element lemmasPerSentenceElement = new Element("value");
		Comment lemmasPerSentenceComment = new Comment("lemmas per sentence");
		lemmasPerSentenceElement.appendChild(lemmasPerSentenceComment);
		lemmasPerSentenceElement.appendChild("" + ((double)content.getLemmaFrequencies().size() / (double)content.getSentences().length));
		instanceElement.appendChild(lemmasPerSentenceElement);
		
		Element uniquePartsOfSpeech = new Element("value");
		Comment uniquePartsOfSpeechComment = new Comment("unique parts of speech");
		uniquePartsOfSpeech.appendChild(uniquePartsOfSpeechComment);
		uniquePartsOfSpeech.appendChild("" + content.getPartsOfSpeechFrequencies().size());
		instanceElement.appendChild(uniquePartsOfSpeech);
		
		Element uniqueNumberOfWords = new Element("value");
		Comment uniqueNumberOfWordsComment = new Comment("unique number of words");
		uniqueNumberOfWords.appendChild(uniqueNumberOfWordsComment);
		uniqueNumberOfWords.appendChild("" + content.getWordFrequencies().size());
		instanceElement.appendChild(uniqueNumberOfWords);

		Element ratioOfPassiveSentences = new Element("value");
		Comment ratioOfPassiveSentencesComment = new Comment("ratio of passive sentences");
		ratioOfPassiveSentences.appendChild(ratioOfPassiveSentencesComment);
		ratioOfPassiveSentences.appendChild("" + ((double)content.getNumberOfPassiveConstructions() / (double)content.getSentences().length));
		instanceElement.appendChild(ratioOfPassiveSentences);
		
		Element classElement = new Element("value");
		Comment classComment = new Comment("class");
		classElement.appendChild(classComment);
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

		Element ratioOfPassiveSentences = new Element("attribute");
		ratioOfPassiveSentences.addAttribute(new Attribute("name", "ratioOfPassiveSentences"));
		ratioOfPassiveSentences.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(ratioOfPassiveSentences);
		
		Element classElement = new Element("attribute");
		classElement.addAttribute(new Attribute("class", "yes"));
		classElement.addAttribute(new Attribute("name", "class"));
		classElement.addAttribute(new Attribute("type", "nominal"));
		attributes.appendChild(classElement);
		
		// TODO: Add labels to classElement
		
	}

	private final String filePath;
	private final FileService fileService;
	
	private static final String dtd = "<!DOCTYPE dataset [" + System.lineSeparator() + 
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
			"]>" + System.lineSeparator() +
			"<root />";
}