package avve.textpreprocess;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.SortedMap;

import org.annolab.tt4j.TreeTaggerException;
import org.annolab.tt4j.TreeTaggerWrapper;
import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;

public class Lemmatizer implements TextPreprocessor
{
	private Logger logger;
	
	private static final ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());

	private static int sentencePointer = 0;
	private static int tokenPointer = 0;
	
	public Lemmatizer(Logger logger)
	{
		this.logger = logger;
	}
	
	@Override
	public void process(EbookContentData ebookContentData)
	{
		// check if we already have a tokenized text as input for the lemmatizer
		if(ebookContentData.getTokens() == null || ebookContentData.getTokens().length == 0)
		{
			this.logger.error(errorMessageBundle.getString("avve.textpreprocess.noTokensAvailable"));
		}
		else
		{
			logger.info(infoMessagesBundle.getString("avve.textpreprocess.lemmatizerStarted"));
		    // unfortunately this is a system-dependent external tool
		    if(System.getProperty("treetagger.home") == null || System.getProperty("treetagger.home").equals(""))
		    {
				System.setProperty("treetagger.home", "/home/kai/TreeTagger");	
		    }
			TreeTaggerWrapper<String> treeTagger = new TreeTaggerWrapper<String>();
			String[][] lemmaArray = new String[ebookContentData.getTokens().length][];
			
			try
			{
				treeTagger.setModel("german-utf8.par");
				treeTagger.setHandler((token, pos, lemma) -> 
				{
					lemmaArray[sentencePointer][tokenPointer] = lemma;
					tokenPointer++;
				});
				for(String[] tokenizedSentence : ebookContentData.getTokens())
				{
					lemmaArray[sentencePointer] = new String[tokenizedSentence.length];
					treeTagger.process(tokenizedSentence);
					sentencePointer++;
					tokenPointer = 0;
				}
			}
			catch (IOException exc)
			{
				// TODO Auto-generated catch block
				exc.printStackTrace();
			}
			catch (TreeTaggerException exc)
			{
				// TODO Auto-generated catch block
				exc.printStackTrace();
			}
			finally
			{
				treeTagger.destroy();
				sentencePointer = 0;
				tokenPointer = 0;
			}
			
			SortedMap<String, Integer> lemmaFrequencies = ebookContentData.getLemmaFrequencies();
			
			for(int i = 0; i < lemmaArray.length; i++)
			{
				for(int j = 0; j < lemmaArray[i].length; j++)
				{
					String key = lemmaArray[i][j];
					if(null != key)
					{
						if(lemmaFrequencies.containsKey(key))
						{
							lemmaFrequencies.put(key, lemmaFrequencies.get(key) + 1);
						}
						else
						{
							lemmaFrequencies.put(key, 1);
						}	
					}
				}
			}
			
			ebookContentData.setLemmas(lemmaArray);
			
			logger.info(String.format(infoMessagesBundle.getString("avve.textpreprocess.numberOfLemmasDetected"), lemmaFrequencies.size()));
		}
	}
}
