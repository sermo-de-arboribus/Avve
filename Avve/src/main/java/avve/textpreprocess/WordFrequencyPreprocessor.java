package avve.textpreprocess;

import java.util.*;

import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;

/**
 * Helper class for counting word frequencies in the EbookContentData object. Should be always used as a preprocessor before 
 * word count statistics are used
 * 
 * @author Kai Weber
 *
 */
public class WordFrequencyPreprocessor implements TextPreprocessor
{
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	private Logger logger;
	
	public WordFrequencyPreprocessor(Logger logger)
	{
		this.logger = logger;
	}

	@Override
	public String getName()
	{
		return "WordFrequencyPreprocessor";
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