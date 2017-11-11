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
		logger.debug(infoMessagesBundle.getString("avve.textpreprocess.wordFrequencyProcessorStart"));
		
		for(String[] sentence : contentData.getTokens())
		{
			for(String token : sentence)
			{
				contentData.countWord(token);
			}
		}
		
		logger.info(String.format(infoMessagesBundle.getString("avve.textpreprocess.wordFrequencyCounted"), contentData.getUniqueNumberOfWords()));
	}
}