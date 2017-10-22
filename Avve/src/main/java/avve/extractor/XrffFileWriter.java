package avve.extractor;

import avve.epubhandling.EbookContentData;
import avve.services.ControlledVocabularyService;
import avve.services.FileService;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import nu.xom.*;

import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.QueryBuilder;

public class XrffFileWriter
{
	public XrffFileWriter(final String filePath, final FileService fileService, final Directory luceneIndexDirectory, final Logger logger,
			final ControlledVocabularyService controlledVocabularyService)
	{
		this.controlledVocabularyService = controlledVocabularyService;
		this.filePath = filePath;
		this.fileService = fileService;
		this.logger = logger;
		this.luceneIndexDirectory = luceneIndexDirectory;
	}
	
	public void saveEbookContentData(final EbookContentData content, final int wordVectorSize)
	{
		FileOutputStream outputStream = null;
		try
		{
			outputStream = fileService.createFileOutputStream(filePath);
			Element root = new Element("dataset");
			root.addAttribute(new Attribute("name", "avve"));
			Document xmlOutputDocument = new Document(root);
			
			writeHeader(root);
			writeBody(root, content);
			addTfIdfStatistics(root, content, wordVectorSize);
			
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
		catch (IOException exc)
		{
			logger.error(exc.getLocalizedMessage(), exc);
			
		}
		catch (ValidityException exc)
		{
			logger.error(exc.getLocalizedMessage(), exc);
		}
		catch (ParsingException exc)
		{
			logger.error(exc.getLocalizedMessage(), exc);
		}
		finally
		{
			fileService.safeClose(outputStream);
		}
	}
	
	private void writeBody(final Element root, final EbookContentData content)
	{
		Element bodyElement = new Element("body");
		root.appendChild(bodyElement);
		
		Element instancesElement = new Element("instances");
		bodyElement.appendChild(instancesElement);
		
		Element instanceElement = new Element("instance");
		instanceElement.addAttribute(new Attribute("documentId", content.getDocumentId()));
		instancesElement.appendChild(instanceElement);
		
		// print all lemmas and part of speech tags
		Element lemmasElement = new Element("value");
		Comment lemmasComment = new Comment("lemmas");
		lemmasElement.appendChild(lemmasComment);
		StringBuffer lemmaSerializer = new StringBuffer();
		// iterate through all lemmatized sentences
		for(int i = 0; i < content.getLemmas().length; i++)
		{
			lemmaSerializer.append("[" + i + "] "); // print number of sentences
			// iterate through all lemmas in the current sentence
			for(int j = 0; j < content.getLemmas()[i].length; j++)
			{
				String posTag = "";
				try
				{
					posTag = content.getPartsOfSpeech()[i][j];
				}
				catch(StringIndexOutOfBoundsException exc)
				{
					// ignore
				}
				lemmaSerializer.append(content.getLemmas()[i][j] + "_" + posTag + " ");
			}
			lemmaSerializer.append(System.lineSeparator());
		}
		lemmasElement.appendChild(lemmaSerializer.toString());
		instanceElement.appendChild(lemmasElement);
		
		// print file size
		Element fileSizeElement = new Element("value");
		Comment fileSizeComment = new Comment("file size in bytes");
		fileSizeElement.appendChild(fileSizeComment);
		fileSizeElement.appendChild("" + content.getFileSize());
		instanceElement.appendChild(fileSizeElement);
		
		// print number of top-level chapters
		Element chaptersElement = new Element("value");
		Comment chaptersComment = new Comment("number of top-level chapters");
		chaptersElement.appendChild(chaptersComment);
		chaptersElement.appendChild("" + content.getNumberOfChapters());
		instanceElement.appendChild(chaptersElement);
		
		// print number of table-of-contents items
		Element tocCountElement = new Element("value");
		Comment tocCountComment = new Comment("number of table-of-contents items");
		tocCountElement.appendChild(tocCountComment);
		tocCountElement.appendChild("" + content.getNumberOfTocItems());
		instanceElement.appendChild(tocCountElement);
		
		// print number of table-of-contents depth
		Element tocDepthElement = new Element("value");
		Comment tocDepthComment = new Comment("depth of table of contents");
		tocDepthElement.appendChild(tocDepthComment);
		tocDepthElement.appendChild("" + content.getDepthOfToc());
		instanceElement.appendChild(tocDepthElement);
		
		// print ratio of words per sentence
		Element wordsPerSentenceElement = new Element("value");
		Comment wordsPerSentenceComment = new Comment("words per sentence");
		wordsPerSentenceElement.appendChild(wordsPerSentenceComment);
		wordsPerSentenceElement.appendChild("" + ((double)content.getNumberOfTokens() / (double)content.getSentences().length));
		instanceElement.appendChild(wordsPerSentenceElement);
		
		// print ratio of lemmas divided by number of tokens
		Element lemmasPerTokensElement = new Element("value");
		Comment lemmasPerTokensElementComment = new Comment("lemma to token ratio");
		lemmasPerTokensElement.appendChild(lemmasPerTokensElementComment);
		lemmasPerTokensElement.appendChild("" + ((double)content.getLemmaFrequencies().size() / (double)content.getNumberOfTokens()));
		instanceElement.appendChild(lemmasPerTokensElement);
		
		// print number of unique parts of speech
		Element uniquePartsOfSpeech = new Element("value");
		Comment uniquePartsOfSpeechComment = new Comment("unique parts of speech");
		uniquePartsOfSpeech.appendChild(uniquePartsOfSpeechComment);
		uniquePartsOfSpeech.appendChild("" + content.getPartsOfSpeechFrequencies().size());
		instanceElement.appendChild(uniquePartsOfSpeech);
		
		// print number of unique words
		Element uniqueNumberOfWords = new Element("value");
		Comment uniqueNumberOfWordsComment = new Comment("unique number of words");
		uniqueNumberOfWords.appendChild(uniqueNumberOfWordsComment);
		uniqueNumberOfWords.appendChild("" + content.getWordFrequencies().size());
		instanceElement.appendChild(uniqueNumberOfWords);

		// print unique-to-total-words-ratio
		Element uniquetoTotalNumberOfWords = new Element("value");
		Comment uniquetoTotalNumberOfWordsComment = new Comment("ratio of unique to total number of words");
		uniquetoTotalNumberOfWords.appendChild(uniquetoTotalNumberOfWordsComment);
		uniquetoTotalNumberOfWords.appendChild("" + ((double)content.getWordFrequencies().size() / (double)content.getNumberOfTokens()));
		instanceElement.appendChild(uniquetoTotalNumberOfWords);
		
		// print ratio of passive sentences
		Element ratioOfPassiveSentences = new Element("value");
		Comment ratioOfPassiveSentencesComment = new Comment("ratio of passive sentences");
		ratioOfPassiveSentences.appendChild(ratioOfPassiveSentencesComment);
		ratioOfPassiveSentences.appendChild("" + ((double)content.getNumberOfPassiveConstructions() / (double)content.getSentences().length));
		instanceElement.appendChild(ratioOfPassiveSentences);
		
		// print ratio of adjectives
		Element ratioOfAdjectives = new Element("value");
		Comment ratioOfAdjectivesComment = new Comment("ratio of adjectives");
		ratioOfAdjectives.appendChild(ratioOfAdjectivesComment);
		ratioOfAdjectives.appendChild("" + content.getAdjectiveRatio());
		instanceElement.appendChild(ratioOfAdjectives);
		
		// print ratio of adverbs
		Element ratioOfAdverbs = new Element("value");
		Comment ratioOfAdverbsComment = new Comment("ratio of adverbs");
		ratioOfAdverbs.appendChild(ratioOfAdverbsComment);
		ratioOfAdverbs.appendChild("" + content.getAdverbRatio());
		instanceElement.appendChild(ratioOfAdverbs);
		
		// print ratio of cardinals
		Element ratioOfCardinals = new Element("value");
		Comment ratioOfCardinalsComment = new Comment("ratio of cardinal numbers");
		ratioOfCardinals.appendChild(ratioOfCardinalsComment);
		ratioOfCardinals.appendChild("" + content.getCardinalsRatio());
		instanceElement.appendChild(ratioOfCardinals);
		
		// print ratio of foreign language material
		Element ratioOfForeignLanguageWords = new Element("value");
		Comment ratioOfForeignLanguageWordsComment = new Comment("ratio of foreign language words");
		ratioOfForeignLanguageWords.appendChild(ratioOfForeignLanguageWordsComment);
		ratioOfForeignLanguageWords.appendChild("" + content.getForeignLanguageWordsRatio());
		instanceElement.appendChild(ratioOfForeignLanguageWords);
				
		// print ratio of interjections
		Element ratioOfInterjectionsElement = new Element("value");
		Comment ratioOfInterjectionsElementComment = new Comment("ratio of interjections");
		ratioOfInterjectionsElement.appendChild(ratioOfInterjectionsElementComment);
		ratioOfInterjectionsElement.appendChild("" + content.getInterjectionRatio());
		instanceElement.appendChild(ratioOfInterjectionsElement);

		// print ratio of nouns
		Element nounsElement = new Element("value");
		Comment nounsElementComment = new Comment("ratio of nouns");
		nounsElement.appendChild(nounsElementComment);
		nounsElement.appendChild("" + content.getNounRatio());
		instanceElement.appendChild(nounsElement);

		// print ratio of named entities
		Element namedEntitiesElement = new Element("value");
		Comment namedEntitiesElementComment = new Comment("ratio of named entities");
		namedEntitiesElement.appendChild(namedEntitiesElementComment);
		namedEntitiesElement.appendChild("" + content.getNamedEntityRatio());
		instanceElement.appendChild(namedEntitiesElement);
		
		// print ratio of substituting demonstrative pronouns
		Element subDemPronElement = new Element("value");
		Comment subDemPronElementComment = new Comment("ratio of substitutive demonstrative pronouns");
		subDemPronElement.appendChild(subDemPronElementComment);
		subDemPronElement.appendChild("" + content.getSubstitutingDemonstrativePronounRatio());
		instanceElement.appendChild(subDemPronElement);
		
		// print ratio of attributive demonstrative pronouns
		Element attrDemPronElement = new Element("value");
		Comment attrDemPronElementComment = new Comment("ratio of attributive demonstrative pronouns");
		attrDemPronElement.appendChild(attrDemPronElementComment);
		attrDemPronElement.appendChild("" + content.getAttributiveDemonstrativePronounRatio());
		instanceElement.appendChild(attrDemPronElement);
		
		// print ratio of substituting indefinite pronouns
		Element subIndefPronElement = new Element("value");
		Comment subIndefPronElementComment = new Comment("ratio of substitutive indefinite pronouns");
		subIndefPronElement.appendChild(subIndefPronElementComment);
		subIndefPronElement.appendChild("" + content.getSubstitutingIndefinitePronounRatio());
		instanceElement.appendChild(subIndefPronElement);
		
		// print ratio of attributive indefinite pronouns
		Element attrIndefPronElement = new Element("value");
		Comment attrIndefPronElementComment = new Comment("ratio of attributive indefinite pronouns");
		attrIndefPronElement.appendChild(attrIndefPronElementComment);
		attrIndefPronElement.appendChild("" + content.getAttributiveIndefinitePronounRatio());
		instanceElement.appendChild(attrIndefPronElement);

		// print ratio of personal pronouns
		Element personalPronounElement = new Element("value");
		Comment personalPronounElementComment = new Comment("ratio of personal pronouns");
		personalPronounElement.appendChild(personalPronounElementComment);
		personalPronounElement.appendChild("" + content.getPersonalPronounRatio());
		instanceElement.appendChild(personalPronounElement);
		
		// print ratio of substituting possessive pronouns
		Element subPossPronElement = new Element("value");
		Comment subPossPronElementComment = new Comment("ratio of substitutive possessive pronouns");
		subPossPronElement.appendChild(subPossPronElementComment);
		subPossPronElement.appendChild("" + content.getSubstitutivePossessivePronounRatio());
		instanceElement.appendChild(subPossPronElement);
		
		// print ratio of attributive possessive pronouns
		Element attrPossPronElement = new Element("value");
		Comment attrPossPronElementComment = new Comment("ratio of attributive possessive pronouns");
		attrPossPronElement.appendChild(attrPossPronElementComment);
		attrPossPronElement.appendChild("" + content.getAttributivePossessivePronounRatio());
		instanceElement.appendChild(attrPossPronElement);
	
		// print ratio of substituting relative pronouns
		Element subRelPronElement = new Element("value");
		Comment subRelPronElementComment = new Comment("ratio of substitutive relative pronouns");
		subRelPronElement.appendChild(subRelPronElementComment);
		subRelPronElement.appendChild("" + content.getSubstitutiveRelativePronounRatio());
		instanceElement.appendChild(subRelPronElement);
		
		// print ratio of attributive relative pronouns
		Element attrRelPronElement = new Element("value");
		Comment attrRelPronElementComment = new Comment("ratio of attributive relative pronouns");
		attrRelPronElement.appendChild(attrRelPronElementComment);
		attrRelPronElement.appendChild("" + content.getAttributivePossessivePronounRatio());
		instanceElement.appendChild(attrRelPronElement);
		
		// print ratio of pronominal adverbs
		Element pronominalAdverbElement = new Element("value");
		Comment pronominalAdverbElementComment = new Comment("ratio of pronominal adverbs");
		pronominalAdverbElement.appendChild(pronominalAdverbElementComment);
		pronominalAdverbElement.appendChild("" + content.getPronominalAdverbRatio());
		instanceElement.appendChild(pronominalAdverbElement);
		
		// print ratio of interrogative pronouns
		Element interrogativePronounElement = new Element("value");
		Comment interrogativePronounElementComment = new Comment("ratio of interrogative pronouns");
		interrogativePronounElement.appendChild(interrogativePronounElementComment);
		interrogativePronounElement.appendChild("" + content.getInterrogativePronounRatio());
		instanceElement.appendChild(interrogativePronounElement);
		
		// print ratio of negation particles
		Element negationParticlesElement = new Element("value");
		Comment negationParticlesElementComment = new Comment("ratio of negation particles");
		negationParticlesElement.appendChild(negationParticlesElementComment);
		negationParticlesElement.appendChild("" + content.getNegationParticleRatio());
		instanceElement.appendChild(negationParticlesElement);
		
		// print ratio of answer particles
		Element answerParticlesElement = new Element("value");
		Comment answerParticlesElementComment = new Comment("ratio of answer particles");
		answerParticlesElement.appendChild(answerParticlesElementComment);
		answerParticlesElement.appendChild("" + content.getAnswerParticlesRatio());
		instanceElement.appendChild(answerParticlesElement);
		
		// print ratio of compound parts
		Element compoundElement = new Element("value");
		Comment compoundElementComment = new Comment("ratio of compound word parts");
		compoundElement.appendChild(compoundElementComment);
		compoundElement.appendChild("" + content.getCompoundWords());
		instanceElement.appendChild(compoundElement);
		
		// print ratio of finite main verbs
		Element finiteMainVerbsElement = new Element("value");
		Comment finiteMainVerbsElementComment = new Comment("ratio of finite main verbs");
		finiteMainVerbsElement.appendChild(finiteMainVerbsElementComment);
		finiteMainVerbsElement.appendChild("" + content.getFiniteMainVerbsRatio());
		instanceElement.appendChild(finiteMainVerbsElement);
	
		// print ratio of imperative main verbs
		Element imperativeMainVerbsElement = new Element("value");
		Comment imperativeMainVerbsElementComment = new Comment("ratio of imperative main verbs");
		imperativeMainVerbsElement.appendChild(imperativeMainVerbsElementComment);
		imperativeMainVerbsElement.appendChild("" + content.getImperativeMainVerbsRatio());
		instanceElement.appendChild(imperativeMainVerbsElement);
		
		// print ratio of infinitive main verbs
		Element infinitiveMainVerbsElement = new Element("value");
		Comment infinitiveMainVerbsElementComment = new Comment("ratio of infinitive main verbs");
		infinitiveMainVerbsElement.appendChild(infinitiveMainVerbsElementComment);
		infinitiveMainVerbsElement.appendChild("" + content.getInfinitiveMainVerbsRatio());
		instanceElement.appendChild(infinitiveMainVerbsElement);
		
		// print ratio of perfect participle main verbs
		Element perfPartMainVerbsElement = new Element("value");
		Comment perfPartMainVerbsElementComment = new Comment("ratio of perfect participle main verbs");
		perfPartMainVerbsElement.appendChild(perfPartMainVerbsElementComment);
		perfPartMainVerbsElement.appendChild("" + content.getMainVerbPerfectParticiplesRatio());
		instanceElement.appendChild(perfPartMainVerbsElement);
		
		// print ratio of auxiliar verbs
		Element auxiliarVerbsElement = new Element("value");
		Comment auxiliarVerbsElementComment = new Comment("ratio of auxiliar verbs");
		auxiliarVerbsElement.appendChild(auxiliarVerbsElementComment);
		auxiliarVerbsElement.appendChild("" + content.getAuxiliarVerbsRatio());
		instanceElement.appendChild(auxiliarVerbsElement);
		
		// print ratio of modal verbs
		Element modalVerbsElement = new Element("value");
		Comment modalVerbsElementComment = new Comment("ratio of modal verbs");
		modalVerbsElement.appendChild(modalVerbsElementComment);
		modalVerbsElement.appendChild("" + content.getModalVerbRatio());
		instanceElement.appendChild(modalVerbsElement);
		
		// handle controlled vocabulary terms, if an appropriate service is defined
		if(null != controlledVocabularyService)
		{
			Iterator<String> cvIterator = controlledVocabularyService.getControlledVocabularyIterator();
			while(cvIterator.hasNext())
			{
				String term = cvIterator.next();
				int numberOfOccurrences = null != content.getLemmaFrequencies().get(term) ? content.getLemmaFrequencies().get(term) : 0;
				Element termElement = new Element("value");
				Comment termElementComment = new Comment("number of term occurrances for " + term);
				termElement.appendChild(termElementComment);
				termElement.appendChild("" + numberOfOccurrences);
				instanceElement.appendChild(termElement);
			}
		}
		
		// print class
		Element classElement = new Element("value");
		Comment classComment = new Comment("class");
		classElement.appendChild(classComment);
		classElement.appendChild(content.getWarengruppe());
		instanceElement.appendChild(classElement);
	}

	private void writeHeader(final Element root)
	{
		Element headerElement = new Element("header");
		root.appendChild(headerElement);
		
		Element attributes = new Element("attributes");
		headerElement.appendChild(attributes);
		
		// lemmas and part-of-speech-tags
		Element lemmas = new Element("attribute");
		lemmas.addAttribute(new Attribute("name", "lemmas"));
		lemmas.addAttribute(new Attribute("type", "string"));
		attributes.appendChild(lemmas);
		
		// file size in bytes
		Element fileSizeElement = new Element("attribute");
		fileSizeElement.addAttribute(new Attribute("name", "fileSizeInBytes"));
		fileSizeElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(fileSizeElement);
		
		// top-level chapters
		Element chaptersElement = new Element("attribute");
		chaptersElement.addAttribute(new Attribute("name", "numberOfTopLevelChapters"));
		chaptersElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(chaptersElement);
		
		// print number of table-of-contents items
		Element tocCountElement = new Element("attribute");
		tocCountElement.addAttribute(new Attribute("name", "numberOfTocElements"));
		tocCountElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(tocCountElement);
		
		// print number of table-of-contents depth
		Element tocDepthElement = new Element("attribute");
		tocDepthElement.addAttribute(new Attribute("name", "depthOfToc"));
		tocDepthElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(tocDepthElement);
		
		// word per sentence
		Element wordsPerSentence = new Element("attribute");
		wordsPerSentence.addAttribute(new Attribute("name", "wordsPerSentence"));
		wordsPerSentence.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(wordsPerSentence);
		
		Element lemmasPerSentence = new Element("attribute");
		lemmasPerSentence.addAttribute(new Attribute("name", "lemmasToTokensRatio"));
		lemmasPerSentence.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(lemmasPerSentence);
		
		Element uniquePartsOfSpeech = new Element("attribute");
		uniquePartsOfSpeech.addAttribute(new Attribute("name", "uniquePartsOfSpeech"));
		uniquePartsOfSpeech.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(uniquePartsOfSpeech);
		
		// unique number of words
		Element uniqueNumberOfWords = new Element("attribute");
		uniqueNumberOfWords.addAttribute(new Attribute("name", "uniqueNumberOfWords"));
		uniqueNumberOfWords.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(uniqueNumberOfWords);

		// ratio of unique to total number of words
		Element uniqueToTotalNumberOfWords = new Element("attribute");
		uniqueToTotalNumberOfWords.addAttribute(new Attribute("name", "uniqueToTotalNumberOfWordsRatio"));
		uniqueToTotalNumberOfWords.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(uniqueToTotalNumberOfWords);
		
		// ratio of passive sentences
		Element ratioOfPassiveSentences = new Element("attribute");
		ratioOfPassiveSentences.addAttribute(new Attribute("name", "ratioOfPassiveSentences"));
		ratioOfPassiveSentences.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(ratioOfPassiveSentences);
		
		// ratio of adjectives element
		Element adjectivesElement = new Element("attribute");
		adjectivesElement.addAttribute(new Attribute("name", "ratioOfAdjectives"));
		adjectivesElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(adjectivesElement);
		
		// ratio of adverbs element
		Element adverbsElement = new Element("attribute");
		adverbsElement.addAttribute(new Attribute("name", "ratioOfAdverbs"));
		adverbsElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(adverbsElement);
		
		// ratio of cardinals element
		Element cardinalsElement = new Element("attribute");
		cardinalsElement.addAttribute(new Attribute("name", "ratioOfCardinals"));
		cardinalsElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(cardinalsElement);

		// ratio of foreign language words element
		Element foreignLanguageWordsElement = new Element("attribute");
		foreignLanguageWordsElement.addAttribute(new Attribute("name", "ratioOfForeignLanguageWords"));
		foreignLanguageWordsElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(foreignLanguageWordsElement);
		
		// ratio of interjections
		Element interjectionsElement = new Element("attribute");
		interjectionsElement.addAttribute(new Attribute("name", "ratioOfInterjections"));
		interjectionsElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(interjectionsElement);
		
		// ratio of nouns
		Element nounsElement = new Element("attribute");
		nounsElement.addAttribute(new Attribute("name", "ratioOfNouns"));
		nounsElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(nounsElement);
		
		// ratio of named entities
		Element namedEntityElement = new Element("attribute");
		namedEntityElement.addAttribute(new Attribute("name", "ratioOfNamedEntities"));
		namedEntityElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(namedEntityElement);
		
		// ratio of substitutive demonstrative pronouns
		Element subDemPronElement = new Element("attribute");
		subDemPronElement.addAttribute(new Attribute("name", "subDemPronRatio"));
		subDemPronElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(subDemPronElement);
		
		// ratio of attributive demonstrative pronouns
		Element attrDemPronElement = new Element("attribute");
		attrDemPronElement.addAttribute(new Attribute("name", "attrDemPronRatio"));
		attrDemPronElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(attrDemPronElement);
		
		// ratio of substitutive indefinite pronouns
		Element subIndefPronElement = new Element("attribute");
		subIndefPronElement.addAttribute(new Attribute("name", "subIndefPronRatio"));
		subIndefPronElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(subIndefPronElement);
		
		// ratio of attributive indefinite pronouns
		Element attrIndefPronElement = new Element("attribute");
		attrIndefPronElement.addAttribute(new Attribute("name", "attrIndefPronRatio"));
		attrIndefPronElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(attrIndefPronElement);

		// ratio of personal pronouns
		Element personalPronounElement = new Element("attribute");
		personalPronounElement.addAttribute(new Attribute("name", "personalPronounRatio"));
		personalPronounElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(personalPronounElement);

		// ratio of substitutive possessive pronouns
		Element subPossPronElement = new Element("attribute");
		subPossPronElement.addAttribute(new Attribute("name", "subPossPronRatio"));
		subPossPronElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(subPossPronElement);
		
		// ratio of attributive possessive pronouns
		Element attrPossPronElement = new Element("attribute");
		attrPossPronElement.addAttribute(new Attribute("name", "attrPossPronRatio"));
		attrPossPronElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(attrPossPronElement);
		
		// ratio of substitutive relative pronouns
		Element subRelPronElement = new Element("attribute");
		subRelPronElement.addAttribute(new Attribute("name", "subRelPronRatio"));
		subRelPronElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(subRelPronElement);
		
		// ratio of attributive relative pronouns
		Element attrRelPronElement = new Element("attribute");
		attrRelPronElement.addAttribute(new Attribute("name", "attrRelPronRatio"));
		attrRelPronElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(attrRelPronElement);

		// ratio of pronominal adverbs
		Element pronominalAdverbsElement = new Element("attribute");
		pronominalAdverbsElement.addAttribute(new Attribute("name", "pronominalAdverbRatio"));
		pronominalAdverbsElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(pronominalAdverbsElement);

		// ratio of interrogative pronoun
		Element interrogativePronounElement = new Element("attribute");
		interrogativePronounElement.addAttribute(new Attribute("name", "interrogativePronounRatio"));
		interrogativePronounElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(interrogativePronounElement);
		
		// ratio of negation particles
		Element negationParticleElement = new Element("attribute");
		negationParticleElement.addAttribute(new Attribute("name", "negationParticleRatio"));
		negationParticleElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(negationParticleElement);
		
		// ratio of answer particles
		Element answerParticleElement = new Element("attribute");
		answerParticleElement.addAttribute(new Attribute("name", "answerParticleRatio"));
		answerParticleElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(answerParticleElement);
		
		// ratio of compound parts
		Element compoundElement = new Element("attribute");
		compoundElement.addAttribute(new Attribute("name", "compoundPartRatio"));
		compoundElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(compoundElement);
			
		// ratio of finite main verbs
		Element finiteMainVerbsElement = new Element("attribute");
		finiteMainVerbsElement.addAttribute(new Attribute("name", "finiteMainVerbsRatio"));
		finiteMainVerbsElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(finiteMainVerbsElement);
		
		// ratio of imperative main verbs
		Element imperativeMainVerbsElement = new Element("attribute");
		imperativeMainVerbsElement.addAttribute(new Attribute("name", "imperativeMainVerbsRatio"));
		imperativeMainVerbsElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(imperativeMainVerbsElement);
		
		// ratio of infinitive main verbs
		Element infinitiveMainVerbsElement = new Element("attribute");
		infinitiveMainVerbsElement.addAttribute(new Attribute("name", "infinitiveMainVerbsRatio"));
		infinitiveMainVerbsElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(infinitiveMainVerbsElement);
		
		// ratio of perfect participle main verbs
		Element perfPartMainVerbsElement = new Element("attribute");
		perfPartMainVerbsElement.addAttribute(new Attribute("name", "perfectParticipleMainVerbsRatio"));
		perfPartMainVerbsElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(perfPartMainVerbsElement);
		
		// ratio of auxiliar verbs
		Element auxiliarVerbsElement = new Element("attribute");
		auxiliarVerbsElement.addAttribute(new Attribute("name", "auxiliarVerbsRatio"));
		auxiliarVerbsElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(auxiliarVerbsElement);

		// ratio of modal verbs
		Element modalVerbsElement = new Element("attribute");
		modalVerbsElement.addAttribute(new Attribute("name", "modalVerbsRatio"));
		modalVerbsElement.addAttribute(new Attribute("type", "numeric"));
		attributes.appendChild(modalVerbsElement);
		
		// handle controlled vocabulary terms, if an appropriate service is defned
		if(null != controlledVocabularyService)
		{
			Iterator<String> cvIterator = controlledVocabularyService.getControlledVocabularyIterator();
			while(cvIterator.hasNext())
			{
				String term = cvIterator.next();
				Element termElement = new Element("attribute");
				termElement.addAttribute(new Attribute("name", "cv_" + term));
				termElement.addAttribute(new Attribute("type", "numeric"));
				attributes.appendChild(termElement);
			}
		}
		
		// class element
		Element classElement = new Element("attribute");
		classElement.addAttribute(new Attribute("class", "yes"));
		classElement.addAttribute(new Attribute("name", "class"));
		classElement.addAttribute(new Attribute("type", "nominal"));
		attributes.appendChild(classElement);
	}

	public void addTfIdfStatistics(final Element root, final EbookContentData content, int wordVectorSize)
	{
		try
		{
			IndexReader luceneIndexReader = DirectoryReader.open(luceneIndexDirectory);
			String documentId = content.getDocumentId();
			int totalNumberOfDocumentsInLuceneIndex = luceneIndexReader.numDocs();
			// termThreshold is a value to prevent very rare words to be used in the output. The threshold is at least 2 and grows moderately (i.e. logarithmically) with the number of indexed documents 
			int termThreshold = 2 + (int)Math.log10(totalNumberOfDocumentsInLuceneIndex);
			
			logger.info(String.format(infoMessagesBundle.getString("avve.extractor.retrievingTfIdfForDocument"), documentId));
			
		    IndexSearcher isearcher = new IndexSearcher(luceneIndexReader);
		    // Parse a simple query that searches for "text":
		    QueryBuilder queryBuilder = new QueryBuilder(new KeywordAnalyzer());
		    Query query = queryBuilder.createPhraseQuery("docId", documentId);
		    
		    // expecting one hit per document ID
		    ScoreDoc[] hits = isearcher.search(query, 1).scoreDocs;
		    if(hits.length > 0)
		    {
			    Terms terms = luceneIndexReader.getTermVector(hits[0].doc, "plaintext");
			    // Beware that there may be differences between the tokenization in EbookContentData and in the Lucene index. The programmer should 
			    // ensure that tokenization is done in a similar way on both sides, so that the number of tokens in EbookContentData is similar with the
			    // number of tokens in the Lucene index
		    	int numberOfTermsInPlainTextField = content.getNumberOfTokens();
		    	
			    long numberOfDocuments = luceneIndexReader.getDocCount("plaintext");
			    TermsEnum termsEnum = terms.iterator();
			    BytesRef bytesRefToTerm = null;
			    SortedMap<String, TfIdfTuple> tdIdfTuples = new TreeMap<String, TfIdfTuple>();
			    
			    // iterate through all terms in the current document's "plaintext" field
			    while ((bytesRefToTerm = termsEnum.next()) != null)
			    {
			    	String term = bytesRefToTerm.utf8ToString();
			    	
			    	// try to get the same term from EbookContentData
			    	if(content.getLemmaFrequencies().containsKey(term))
			    	{
			    		int termFrequencyInDocumentField = content.getLemmaFrequencies().get(term);	
			    		
				    	// calculate term frequency
				    	if(tdIdfTuples.containsKey(term))
				    	{
				    		// increment term frequency of existing entry
				    		tdIdfTuples.get(term).incrementTermFrequency(termFrequencyInDocumentField);
				    	}
				    	else
				    	{
				    		// only use words that don't appear in (nearly) all documents and that appear at least in three documents
				    		int docFreq = luceneIndexReader.docFreq(new Term("plaintext", bytesRefToTerm));
				    		if(docFreq > termThreshold && docFreq < (numberOfDocuments * 0.9))
				    		{
					    		// initialize term frequency and calculate inverse document frequency (only need to do that once per term)
					    		double idf = 1 + Math.log(numberOfDocuments / docFreq + 1.0);
					    		tdIdfTuples.put(term, new TfIdfTuple(termFrequencyInDocumentField, idf, 1.0 / Math.sqrt(numberOfTermsInPlainTextField)));
				    		}
				    	}
			    	}
			    	else
			    	{
			    		logger.debug(String.format(infoMessagesBundle.getString("avve.extractor.couldNotFindLuceneTermInEbookContentData"), term));
			    	}
			    }
			    
			    List<Entry<String, TfIdfTuple>> sortedByIdfDescending = new ArrayList<Entry<String, TfIdfTuple>>(tdIdfTuples.entrySet());
			    
			    Collections.sort(sortedByIdfDescending, new Comparator<Entry<String, TfIdfTuple>>()
			    {
			    	@Override
			    	public int compare(Entry<String, TfIdfTuple> e1, Entry<String, TfIdfTuple> e2)
			    	{
			    		return e2.getValue().compareTo(e1.getValue());
			    	}
			    });
			    
			    Nodes attributesNode = root.query("/dataset/header/attributes");
			    Element attributesElement = (Element)attributesNode.get(0);
			    Element attributeElement = new Element("attribute");
			    attributeElement.addAttribute(new Attribute("name", "top-idf"));
			    attributeElement.addAttribute(new Attribute("type", "string"));
			    attributesElement.insertChild(attributeElement, attributesElement.getChildCount() - 1);
			    
			    Nodes instanceNode = root.query("/dataset/body/instances/instance");
			    Element instanceElement = (Element)instanceNode.get(0);
			    int numberOfIdfValuesToInclude = sortedByIdfDescending.size() > wordVectorSize ? wordVectorSize : sortedByIdfDescending.size();
			    
			    Element newValueElement = new Element("value");
			    Comment newValueComment = new Comment("[index] term - normalizedTfIdfValue - idf - term frequency");
			    newValueElement.appendChild(newValueComment);
			    newValueElement.appendChild(new Text(System.lineSeparator()));
			    for(int i = 0; i < numberOfIdfValuesToInclude; i++ )
			    {
			    	String term = sortedByIdfDescending.get(i).getKey();
			    	Node termText = new Text(term);
			    	Node termComment = new Comment("[" + i + "] - "
			    			+ sortedByIdfDescending.get(i).getValue().getNormalizedTfIdfValue() + " - "
			    			+ sortedByIdfDescending.get(i).getValue().getInverseDocumentFrequency() + " - "
			    			+ sortedByIdfDescending.get(i).getValue().getTermFrequency()
			    			);
			    	newValueElement.appendChild(termComment);
			    	newValueElement.appendChild(termText);
			    	newValueElement.appendChild(new Text(System.lineSeparator()));
			    }
			    instanceElement.insertChild(newValueElement, instanceElement.getChildCount() - 1);
		    }
		    else
		    {
		    	logger.debug(String.format(infoMessagesBundle.getString("avve.extractor.couldNotFindDocumentInLuceneIndex"), documentId));
		    }
	    }
		catch (IOException exc)
		{
			logger.error(errorMessageBundle.getString("avve.extractor.luceneIndexAccessError"), exc);
	    }
		catch (NullPointerException exc)
		{
			logger.error(errorMessageBundle.getString("avve.extractor.luceneIndexAccessError"), exc);
		}
	}
	
	private final ControlledVocabularyService controlledVocabularyService;
	private final String filePath;
	private final FileService fileService;
	private final Logger logger;
	private final Directory luceneIndexDirectory;
	
	private static final ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
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
			"<!ATTLIST instance documentId CDATA #IMPLIED>" + System.lineSeparator() +
			"<!ELEMENT value (#PCDATA|instances)*>" + System.lineSeparator() + 
			"<!ATTLIST value index CDATA #IMPLIED>   <!-- 1-based index (only used for instance format \"sparse\") -->" + System.lineSeparator() + 
			"<!ATTLIST value missing (yes|no) \"no\">" + System.lineSeparator() + 
			"]>" + System.lineSeparator() +
			"<root />";
}