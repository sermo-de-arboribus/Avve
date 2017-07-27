package avve.epubhandling;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.Logger;

public class EbookContentData
{
	private static final ResourceBundle errorMessagesBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	private Logger logger;
	
	private String plainText;
	private String warengruppe;
	private String[] sentences;
	private String[][] tokenizedSentences;
	private String[][] lemmatizedSentences;
	private String[][] partsOfSpeech;
	private SortedMap<String, Integer> lemmaFrequencies;
	private SortedMap<String, Integer> wordFrequencies;
	private SortedMap<String, Integer> partsOfSpeechFrequencies;
	private int numberOfTokens;
	
	public EbookContentData(String plainText, String warengruppe, Logger logger)
	{
		this.logger = logger;
		this.plainText = plainText;
		this.warengruppe = warengruppe;
		lemmaFrequencies = new TreeMap<String, Integer>();
		wordFrequencies = new TreeMap<String, Integer>();
		partsOfSpeechFrequencies = new TreeMap<String, Integer>();
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
	
	public String getPlainText()
	{
		return plainText;
	}
	
	public String[] getSentences()
	{
		return sentences;
	}
	
	public String[][] getTokens()
	{
		return tokenizedSentences;
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
					if(partsOfSpeech[i][j].equals("VAPP") || partsOfSpeech[i][j].equals("VMPP"))
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
	
	public String[][] getPartsOfSpeech()
	{
		return partsOfSpeech;
	}

	public String getWarengruppe()
	{
		return warengruppe;
	}
	
	public SortedMap<String, Integer> getWordFrequencies()
	{
		return wordFrequencies;
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
}