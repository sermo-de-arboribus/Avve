package avve.epubhandling;

import java.io.Serializable;
import java.util.*;

import org.apache.logging.log4j.Logger;

public class EbookContentData implements Serializable
{
	/**
	 * Constructor
	 * @param plainText The input text
	 * @param warengruppe The Warengruppe class
	 * @param logger The logger to be used
	 */
	public EbookContentData(EpubFile epubFile, String plainText, String warengruppe, Logger logger)
	{
		this.epubFile = epubFile;
		this.logger = logger;
		this.plainText = plainText;
		this.warengruppe = warengruppe;
		setHyperonymFrequencies(new TreeMap<String, Integer>());
		lemmaFrequencies = new TreeMap<String, Integer>();
		wordFrequencies = new TreeMap<String, Integer>();
		partsOfSpeechFrequencies = new TreeMap<String, Integer>();
		warengruppenMap = new HashMap<String, String>();
		
		initializeWarengruppenMap();
	}
	
	/**
	 * Word counts are maintained to retrieve several statistics, such as word frequency or average word length
	 * For this purpose every word encountered in a document should be passed to the countWord() function exactly once
	 * to initialize the data structures for the aforementioned statistics.
	 * @param word The word to be added to the statistics
	 */
	public void countWord(String word)
	{
		// add new words to the word frequency counter
		if(wordFrequencies.containsKey(word))
		{
			wordFrequencies.put(word, wordFrequencies.get(word) + 1);
		}
		else
		{
			wordFrequencies.put(word, 1);
		}
		
		// add new words to word length variables, to be able to calculate average word length
		numberOfWords++;
		wordLength += word.length();
	}

	/**
	 * Get the number of adjectives, divided by the number of tokens in this text.
	 * @return The adjectives-to-tokens ratio
	 */
	public double getAdjectiveRatio()
	{
		return calculatePosTokenRatio(new String[]{"ADJA", "ADJD"});
	}
	
	/**
	 * Get the number of adverbs, divided by the number of tokens in this ebook text.
	 * @return The adverbs-to-tokens ratio
	 */
	public double getAdverbRatio()
	{
		return calculatePosTokenRatio(new String[]{"ADV", "ADV|FM"});
	}
	
	/**
	 * Get the number of answer particles, divided by the number of tokens in this ebook text.
	 * @return The particles-to-tokens ratio
	 */
	public double getAnswerParticlesRatio()
	{
		return calculatePosTokenRatio("PTKANT");
	}
	
	/**
	 * Get the attributive demonstrative pronouns, divided by the number of tokens in this ebook text.
	 * @return The pronoun-to-tokens ratio
	 */
	public double getAttributiveDemonstrativePronounRatio()
	{
		return calculatePosTokenRatio(new String[]{"PDAT", "PDAT|PDS"});
	}
	
	/**
	 * Get the attributive indefinite pronouns, divided by the number of tokens in this ebook text.
	 * @return The pronoun-to-tokens ratio
	 */
	public double getAttributiveIndefinitePronounRatio()
	{
		return calculatePosTokenRatio(new String[]{"PIS", "PIAT|PIS"});
	}

	/**
	 * Get the attributive possessive pronouns, divided by the number of tokens in this ebook text.
	 * @return The pronoun-to-tokens ratio
	 */
	public double getAttributivePossessivePronounRatio()
	{
		return calculatePosTokenRatio("PPOSAT");
	}
	
	/**
	 * Get the attributive relative pronouns, divided by the number of tokens in this ebook text.
	 * @return The pronoun-to-tokens ratio
	 */
	public double getAttributiveRelativePronounRatio()
	{
		return calculatePosTokenRatio("PRELS");
	}
	
	/**
	 * Get the auxiliar verbs, divided by the number of tokens in this ebook text.
	 * @return The verb-to-tokens ratio
	 */
	public double getAuxiliarVerbsRatio()
	{
		return calculatePosTokenRatio(new String[]{"VAFIN", "VAIMP", "VAINF", "VAPP", "VAFIN|VAINF"});
	}
	
	/**
	 * Get the average word length; note that this call only provides accurate values, if all words have been passed to the countWord() function
	 * @return The average word length of this e-book.
	 */
	public double getAverageWordLength()
	{
		return (double)wordLength / (double)numberOfWords;
	}
	
	/**
	 * Get the number of cardinals, divided by the number of tokens in this ebook text.
	 * @return The cardinals-to-tokens ratio
	 */
	public double getCardinalsRatio()
	{
		return calculatePosTokenRatio("CARD");
	}
	
	/**
	 * Get the number of compound words, divided by the number of tokens in this ebook text.
	 * @return The compounds-to-tokens ratio
	 */
	public double getCompoundWords()
	{
		return calculatePosTokenRatio("TRUNC");
	}
	
	/**
	 * Get the coordinating conjunctions (e.g. "and", "und", "or", "oder"), divided by the number of tokens in this ebook text.
	 * @return The conjunction-to-tokens ratio
	 */
	public double getCoordinatingConjunctions()
	{
		return calculatePosTokenRatio("KON");
	}
	
	/**
	 * Determine the depth of the ebook's table of contents. A table of content with no sub-chapters has depth 1, a TOC with main chapters (1, 2, ...) and one level of subchapters (1.1, 1.2, 1.3) has depth 2 etc. 
	 * @return The depth of the ebook's table of contents as an integer. If no TOC can be found, -1 will be returned
	 */
	public int getDepthOfToc()
	{
		return epubFile.getDepthOfToc();
	}

	/**
	 * Retrieve the unique ID of the EPUB document that this object represents
	 * @return A unique ID for the underlying EPUB document
	 */
	public String getDocumentId()
	{
		return epubFile.getDocumentId();
	}
	
	/**
	 * Get the file size of the underlying EPUB file (in zipped form)
	 * @return The file size in bytes
	 */
	public long getFileSize()
	{
		return epubFile.getFileSize();
	}
	
	/**
	 * Get the number of finite main verbs, divided by the number of tokens in this ebook text.
	 * @return The verbs-to-tokens ratio
	 */
	public double getFiniteMainVerbsRatio()
	{
		return calculatePosTokenRatio("VVFIN");
	}
	
	/**
	 * Get the number of words in foreign language(s), divided by the number of tokens in this ebook text.
	 * @return The foreign-words-to-tokens ratio
	 */
	public double getForeignLanguageWordsRatio()
	{
		return calculatePosTokenRatio("FM");
	}
	
	/**
	 * Get the number of imperative main verbs, divided by the number of tokens in this ebook text.
	 * @return The verbs-to-tokens ratio
	 */
	public double getImperativeMainVerbsRatio()
	{
		return calculatePosTokenRatio("VVIMP");
	}
	
	/**
	 * Get the number of infinitive main verbs, divided by the number of tokens in this ebook text.
	 * @return The verbs-to-tokens ratio
	 */
	public double getInfinitiveMainVerbsRatio()
	{
		return calculatePosTokenRatio(new String[]{"VVINF", "VVIZU", "VVFIN|VVINF"});
	}
	
	/**
	 * Get the number of interjections, divided by the number of tokens in this ebook text.
	 * @return The foreign-words-to-tokens ratio
	 */
	public double getInterjectionRatio()
	{
		return calculatePosTokenRatio("ITJ");
	}
	
	/**
	 * Get the number of interrogative pronouns, divided by the number of tokens in this ebook text.
	 * @return The pronoun-to-tokens ratio
	 */
	public double getInterrogativePronounRatio()
	{
		return calculatePosTokenRatio(new String[] {"PWS", "PWAT", "PWAV"});
	}
	
	public String getLanguage()
	{
		if(null != epubFile)
		{
			return epubFile.getLanguageCode();
		}
		else
		{
			return "";
		}
	}
	
	public SortedMap<String, Integer> getLemmaFrequencies()
	{
		return lemmaFrequencies;
	}
	
	public String[][] getLemmas()
	{
		return lemmatizedSentences;
	}
	
	/**
	 * Get the number of main verbs in perfect participle form, divided by the number of tokens in this ebook text.
	 * @return The verbs-to-tokens ratio
	 */
	public double 	getMainVerbPerfectParticiplesRatio()
	{
		return calculatePosTokenRatio("VVPP");
	}
	
	/**
	 * Get the number of modal verbs, divided by the number of tokens in this ebook text.
	 * @return The modal-verb-to-tokens ratio
	 */
	public double getModalVerbRatio()
	{
		return calculatePosTokenRatio(new String[] {"VMFIN", "VMINF", "VMPP"});
	}
	
	/**
	 * Get the number of named entity nouns, divided by the number of tokens in this ebook text.
	 * @return The named-entities-to-tokens ratio
	 */
	public double getNamedEntityRatio()
	{
		return calculatePosTokenRatio("NE");
	}
	
	/**
	 * Get the number of negation particles, divided by the number of tokens in this ebook text.
	 * @return The negation-particles-to-tokens ratio
	 */
	public double getNegationParticleRatio()
	{
		return calculatePosTokenRatio("PTKNEG");
	}
	
	public double getNormalizedLemmaFrequency(String lemma)
	{
		int lemmaFrequency = null != getLemmaFrequencies().get(lemma) ? getLemmaFrequencies().get(lemma) : 0;
		return lemmaFrequency / (double)lemmaFrequencies.size();
	}
	
	/**
	 * Get the number of nouns, divided by the number of tokens in this ebook text.
	 * The noun count includes general nouns as well as named entities
	 * @return The nouns-to-tokens ratio
	 */
	public double getNounRatio()
	{
		return calculatePosTokenRatio("NN");
	}
	
	public int getNumberOfImages()
	{
		return epubFile.getNumberOfImages();
	}
	
	/**
	 * This method returns a rough estimation of sentence that contain passive tenses. The method is expected to over-estimate the actual number
	 * of passive tense sentences, because it doesn't disambiguate homonymous forms, e.g.
	 * "Sie werden vergessen" can either mean "They will forget" (active sentence in future tense) as well as "They are forgotten" (passive sentence in present tense)
	 * 
	 * @return The rough (over-)estimate of passive constructs in the input text
	 */
	public int getNumberOfPassiveConstructions()
	{
		int counter = 0; 
		// iterate through sentences
		for(int i = 0; i < lemmatizedSentences.length; i++)
		{
			boolean hasAuxiliarVerb = false;
			boolean hasParticiple = false;
			
			String auxiliarVerbMessage = "";
			String participleMessage = "";
			
			// iterate through lemmas in the sentence
			for(int j = 0; j < lemmatizedSentences[i].length; j++)
			{	
				try
				{
					// search for tokens "werden" ("Vorgangspassiv") and "sein" ("Zustandspassiv")
					if((lemmatizedSentences[i][j].equals("werden") || lemmatizedSentences[i][j].equals("sein"))
							&& (  partsOfSpeech[i][j].equals("VAFIN") 
								|| partsOfSpeech[i][j].equals("VAIMP")
								|| partsOfSpeech[i][j].equals("VAINF")
							   )
						)
					{
						hasAuxiliarVerb = true;
						auxiliarVerbMessage = String.format(infoMessagesBundle.getString("avve.epubhandling.auxiliarVerbMessage"), lemmatizedSentences[i][j],
								j, i, partsOfSpeech[i][j]);
					}
					if(partsOfSpeech[i][j].equals("VAPP") || partsOfSpeech[i][j].equals("VMPP") || partsOfSpeech[i][j].equals("VVPP"))
					{
						hasParticiple = true;
						participleMessage = String.format(infoMessagesBundle.getString("avve.epubhandling.participleMessage"), lemmatizedSentences[i][j],
								j, i, partsOfSpeech[i][j]);
					}
					
					if(hasAuxiliarVerb && hasParticiple)
					{
						break;
					}
				}
				catch(NullPointerException exc)
				{
					logger.error(String.format(errorMessagesBundle.getString("avve.epubhandling.nullPointerException"), i, j));
				}
			} // end of lemma loop
			
			if(hasAuxiliarVerb && hasParticiple)
			{
				counter++;
				logger.trace(auxiliarVerbMessage);
				logger.trace(participleMessage);
			}
		} // end of sentence loop
		
		return counter;
	}
	
	public int getNumberOfTokens()
	{
		if(0 == numberOfTokens)
		{
			for(int i = 0; i < getTokens().length; i++)
			{
				numberOfTokens += getTokens()[i].length;
			}
		}
		return numberOfTokens;
	}
	
	/**
	 * The overall number of words of this e-book. Note that returned values are only accurate, if all words have been passed to the countWords() function before
	 * @return The overall number of words (not normalized)
	 */
	public int getNumberOfWords()
	{
		return numberOfWords;
	}
	
	public String[][] getPartsOfSpeech()
	{
		return partsOfSpeech;
	}	
	
	/**
	 * Get the number of personal pronouns, divided by the number of tokens in this ebook text.
	 * @return The named-entities-to-tokens ratio
	 */
	public double getPersonalPronounRatio()
	{
		return calculatePosTokenRatio(new String[]{"PPER", "PRF"});
	}
	
	public String getPlainText()
	{
		return plainText;
	}
	
	public String getLemmatizedText()
	{
		return lemmasToString(true);
	}
	
	public String getLemmatizedTextWithoutForeignWords()
	{
		return lemmasToString(false);
	}
	
	public int getNumberOfChapters()
	{
		return epubFile.getNumberOfChapters();
	}

	public int getNumberOfTocItems()
	{
		return epubFile.getNumberOfTocItems();
	}

	/**
	 * Get the number of pronominal adverbs, divided by the number of tokens in this ebook text.
	 * @return The pronoun-to-tokens ratio
	 */
	public double getPronominalAdverbRatio()
	{
		return calculatePosTokenRatio(new String[] { "PROAV", "PAV" });
	}
	
	public String[] getSentences()
	{
		return sentences;
	}
	
	public String[][] getTokens()
	{
		return tokenizedSentences;
	}
	
	/**
	 * Get the subordinating conjunctions (e.g. "um ... [zu]", "dass", "weil", "obwohl", ...), divided by the number of tokens in this ebook text.
	 * @return
	 */
	public double getSubordinatingConjunctions()
	{
		return calculatePosTokenRatio(new String[]{"KOUI", "KOUS"});
	}
	
	/**
	 * Get the substituting demonstrative pronouns, divided by the number of tokens in this ebook text.
	 * @return The pronoun-to-tokens ratio
	 */
	public double getSubstitutingDemonstrativePronounRatio()
	{
		return calculatePosTokenRatio(new String[]{"PDS", "PDAT|PDS"});
	}
	
	/**
	 * Get the substituting indefinite pronouns, divided by the number of tokens in this ebook text.
	 * @return The pronoun-to-tokens ratio
	 */
	public double getSubstitutingIndefinitePronounRatio()
	{
		return calculatePosTokenRatio(new String[]{"PIAT", "PIDAT"});
	}

	/**
	 * Get the substitutive possessive pronouns, divided by the number of tokens in this ebook text.
	 * @return The pronoun-to-tokens ratio
	 */
	public double getSubstitutivePossessivePronounRatio()
	{
		return calculatePosTokenRatio("PPOSS");
	}
	
	/**
	 * Get the substitutive relative pronouns, divided by the number of tokens in this ebook text.
	 * @return The pronoun-to-tokens ratio
	 */
	public double getSubstitutiveRelativePronounRatio()
	{
		return calculatePosTokenRatio("PRELAT");
	}
	
	/**
	 * Get the unique number of non-lemmatized words. Note that this function only returns accurate values after all words have been passed to the countWord() function
	 * @return The unique number of words.
	 */
	public int getUniqueNumberOfWords()
	{
		return wordFrequencies.size();
	}

	/**
	 * Returns the ratio of number of words N divided by unique number of words V.
	 * Note that longer texts usually have a smaller vocabulary richness than shorter ones and consider a normalization, if necessary.
	 * @return Number of words divided by unique number of words.
	 */
	public double getVocabularyRichness()
	{
		return (double)numberOfWords / (double)getUniqueNumberOfWords();
	}
	
	public String getWarengruppe()
	{
		// we might want to map some Warengruppe codes onto a common code, so we don't return the actual code, but go through a map
		if(warengruppenMap.containsKey(warengruppe))
		{
			return warengruppenMap.get(warengruppe);
		}
		else
		{
			return warengruppe;
		}
	}
	
	public SortedMap<String, Integer> getPartsOfSpeechFrequencies()
	{
		return partsOfSpeechFrequencies;
	}
	
	public void setNumberOfTokens(final int tokenCount)
	{
		numberOfTokens = tokenCount;
	}
	
	public void setSentences(final String[] sentences)
	{
		this.sentences = sentences;
	}

	public void setTokens(final String[][] tokens)
	{
		this.tokenizedSentences = tokens;
	}

	public void setLemmas(final String[][] lemmas)
	{
		this.lemmatizedSentences = lemmas;
	}

	public void setPartsOfSpeech(final String[][] partsOfSpeech)
	{
		this.partsOfSpeech = partsOfSpeech;
	}

	public void setPlainText(final String plainText)
	{
		this.plainText = plainText;
	}
	
	private double calculatePosTokenRatio(final String posToken)
	{
		if(null == posToken || !partsOfSpeechFrequencies.containsKey(posToken))
		{
			return 0.0;
		}
		else
		{
			int posTokenCount = partsOfSpeechFrequencies.get(posToken);
			return calculateRatio(posTokenCount, getNumberOfTokens());
		}
	}
	
	private double calculatePosTokenRatio(final String[] posTokens)
	{
		int posTokenCount = 0;
		for(int i = 0; i < posTokens.length; i++)
		{
			if(!partsOfSpeechFrequencies.containsKey(posTokens[i]))
			{
				// nothing to do, assuming posTokenCount for posTokens[i] is 0 
			}
			else
			{
				posTokenCount += partsOfSpeechFrequencies.get(posTokens[i]);
			}
		}
		
		return calculateRatio(posTokenCount, getNumberOfTokens());
	}
	
	private double calculateRatio(final int numerator, final int denominator)
	{
		if(denominator != 0)
		{
			return (double)numerator / (double)denominator;
		}
		else // avoid division by zero
		{
			return (double)numerator / Double.MAX_VALUE;
		}
	}
	
	private void initializeWarengruppenMap()
	{
		//warengruppenMap.put("111", "110");
		//warengruppenMap.put("112", "110");
	}

	private String lemmasToString(boolean withForeignWords)
	{
		StringBuilder stringbuilder = new StringBuilder();
			
		for(int i = 0; i < lemmatizedSentences.length; i++)
		{
			for(int j = 0; j < lemmatizedSentences[i].length; j++)
			{
				if(withForeignWords)
				{
					stringbuilder.append(lemmatizedSentences[i][j]);
					stringbuilder.append(" ");
				}
				else
				{
					String lemma = partsOfSpeech[i][j].equals("FM") ? "" : lemmatizedSentences[i][j] + " ";
					stringbuilder.append(lemma);
				}

			}
			stringbuilder.append(System.lineSeparator());
		}
		
		return stringbuilder.toString();
	}
	
	public SortedMap<String, Integer> getHyperonymFrequencies()
	{
		return hyperonymFrequencies;
	}

	public void setHyperonymFrequencies(SortedMap<String, Integer> hyperonymFrequencies)
	{
		this.hyperonymFrequencies = hyperonymFrequencies;
	}

	private static final long serialVersionUID = 3022250504362756894L;
	private static final ResourceBundle errorMessagesBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	private Logger logger;
	
	private EpubFile epubFile;

	private String plainText;
	private String[] sentences;
	private String warengruppe;
	private String[][] tokenizedSentences;
	private String[][] lemmatizedSentences;
	private String[][] partsOfSpeech;
	private SortedMap<String, Integer> lemmaFrequencies;
	private SortedMap<String, Integer> hyperonymFrequencies;
	private SortedMap<String, Integer> wordFrequencies;
	private SortedMap<String, Integer> partsOfSpeechFrequencies;
	private int numberOfTokens;
	private int numberOfWords;
	private final Map<String, String> warengruppenMap;
	private long wordLength;
}