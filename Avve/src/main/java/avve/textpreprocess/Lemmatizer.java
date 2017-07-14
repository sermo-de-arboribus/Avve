package avve.textpreprocess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

	public Lemmatizer(Logger logger)
	{
		this.logger = logger;
	}
	
	@Override
	public void process(EbookContentData ebookContentData)
	{
		if(ebookContentData.getTokens() == null || ebookContentData.getTokens().length == 0)
		{
			this.logger.error(errorMessageBundle.getString("avve.textpreprocess.noTokensAvailable"));
		}
		else
		{
			logger.info(infoMessagesBundle.getString("avve.textpreprocess.lemmatizerStarted"));
		    List<String> lemmas = new ArrayList<String>();
			System.setProperty("treetagger.home", "/home/kai/TreeTagger");
			TreeTaggerWrapper<String> treeTagger = new TreeTaggerWrapper<String>();
			
			try
			{
				treeTagger.setModel("german-utf8.par");
				treeTagger.setHandler((token, pos, lemma) -> lemmas.add(lemma));
				treeTagger.process(ebookContentData.getTokens());
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
			}
			
			SortedMap<String, Integer> lemmaFrequencies = ebookContentData.getLemmaFrequencies();
			String[] lemmaArray = new String[lemmas.size()];
			
			for(int i = 0; i < lemmaArray.length; i++)
			{
				lemmaArray[i] = lemmas.get(i);
				String key = lemmas.get(i);
				
				if(lemmaFrequencies.containsKey(key))
				{
					lemmaFrequencies.put(key, lemmaFrequencies.get(key) + 1);
				}
				else
				{
					lemmaFrequencies.put(key, 1);
				}
			}
			
			ebookContentData.setLemmas(lemmaArray);
		}
	}
}
