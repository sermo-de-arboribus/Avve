package avve.textpreprocess;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.logging.log4j.Logger;

public class WordFrequencyPreprocessor implements TextPreprocessor
{
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	private Logger logger;
	private Hashtable<String, Integer> wordCount;
	
	public WordFrequencyPreprocessor(Logger logger)
	{
		this.logger = logger;
		wordCount = new Hashtable<String, Integer>();
	}

	@Override
	public String process(String inputText)
	{
		logger.trace(infoMessagesBundle.getString("avve.textpreprocess.wordFrequencyProcessorStart"));
		
		String[] tokens = inputText.split("[.!?\\-\\s]+");
		
		for(String token : tokens)
		{
			if(wordCount.containsKey(token))
			{
				wordCount.put(token, wordCount.get(token) + 1);
			}
			else
			{
				wordCount.put(token, 1);
			}
		}
		
		Enumeration<String> keys = wordCount.keys();
		StringBuilder sb = new StringBuilder();
		int numberOfKeys = 0;
		
		while(keys.hasMoreElements())
		{
			String key = keys.nextElement();
			sb.append(key.hashCode() + " : ");
			sb.append(wordCount.get(key));
			sb.append(" [ " + key + " ]");
			sb.append(System.lineSeparator());
			numberOfKeys++;
		}
		
		logger.trace(String.format(infoMessagesBundle.getString("avve.textpreprocess.wordFrequencyCounted"), numberOfKeys));

		return sb.toString();
	}
}