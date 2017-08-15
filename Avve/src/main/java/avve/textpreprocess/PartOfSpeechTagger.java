package avve.textpreprocess;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.SortedMap;

import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

public class PartOfSpeechTagger implements TextPreprocessor
{
	private static final ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	POSTaggerME tagger;
	Logger logger;
	
	public PartOfSpeechTagger(Logger logger)
	{
		this.logger = logger;
		
		try (InputStream modelIn = this.getClass().getClassLoader().getResourceAsStream("opennlp/de-pos-maxent.bin"))
		{
			POSModel model = new POSModel(modelIn);
			tagger = new POSTaggerME(model);
		}
		catch (IOException exc)
		{
			this.logger.error(errorMessageBundle.getString("PartOfSpeechTaggerModelInitError"));
			exc.printStackTrace();
		}
	}
	
	@Override
	public void process(EbookContentData ebookContentData)
	{
		if(ebookContentData.getTokens() == null || ebookContentData.getTokens().length == 0)
		{
			this.logger.error(String.format(errorMessageBundle.getString("avve.textpreprocess.noTokensAvailable"), "PartOfSpeechTagger.process()"));
		}
		else
		{
			String[][] partsOfSpeech = new String[ebookContentData.getTokens().length][];
			
			for(int i = 0; i < partsOfSpeech.length; i++)
			{
				String[] currentSentence = ebookContentData.getTokens()[i];
				partsOfSpeech[i] = tagger.tag(currentSentence);	
			}
			ebookContentData.setPartsOfSpeech(partsOfSpeech);
			
			SortedMap<String, Integer> posMap = ebookContentData.getPartsOfSpeechFrequencies();
			
			for(String[] sentence : ebookContentData.getPartsOfSpeech())
			{
				for(String tag : sentence)
				{
					if(posMap.containsKey(tag))
					{
						posMap.put(tag, posMap.get(tag) + 1);
					}
					else
					{
						posMap.put(tag, 1);
					}	
				}
			}
			
			logger.trace(String.format(infoMessagesBundle.getString("avve.textpreprocess.partOfSpeechTaggerDifferentPos"), posMap.keySet().size()));
		}
	}
}