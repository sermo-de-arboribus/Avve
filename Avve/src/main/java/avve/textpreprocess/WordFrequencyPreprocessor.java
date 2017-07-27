package avve.textpreprocess;

import java.util.*;

import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;

public class WordFrequencyPreprocessor implements TextPreprocessor
{
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	private Logger logger;
	
	public WordFrequencyPreprocessor(Logger logger)
	{
		this.logger = logger;
	}

	@Override
	public void process(EbookContentData contentData)
	{
		logger.trace(infoMessagesBundle.getString("avve.textpreprocess.wordFrequencyProcessorStart"));
		
		SortedMap<String, Integer> sortedWordCount = contentData.getWordFrequencies();
		
		for(String[] sentence : contentData.getTokens())
		{
			for(String token : sentence)
			{
				if(sortedWordCount.containsKey(token))
				{
					sortedWordCount.put(token, sortedWordCount.get(token) + 1);
				}
				else
				{
					sortedWordCount.put(token, 1);
				}
			}
		}
		
		logger.trace(String.format(infoMessagesBundle.getString("avve.textpreprocess.wordFrequencyCounted"), contentData.getWordFrequencies().size()));
	}
}