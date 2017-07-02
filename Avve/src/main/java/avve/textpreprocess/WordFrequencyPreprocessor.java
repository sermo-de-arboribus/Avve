package avve.textpreprocess;

import java.util.*;
import java.util.Map.Entry;

import org.apache.logging.log4j.Logger;

public class WordFrequencyPreprocessor implements TextPreprocessor
{
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	private Logger logger;
	private HashMap<String, Integer> wordCount;
	
	public WordFrequencyPreprocessor(Logger logger)
	{
		this.logger = logger;
		wordCount = new HashMap<String, Integer>();
	}

	@Override
	public String[] process(String inputText, TextStatistics statistics)
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
		
		List<Entry<String, Integer>> sortedWordCount = sortByComparator(wordCount);
		
		StringBuilder sb = new StringBuilder();
		int numberOfKeys = 0;
		
		for(Entry<String, Integer> entry : sortedWordCount)
		{
			sb.append(entry.getKey().hashCode() + " : ");
			sb.append(entry.getValue());
			sb.append(" [ " + entry.getKey() + " ]");
			sb.append(System.lineSeparator());
			numberOfKeys++;
		}
		
		logger.trace(String.format(infoMessagesBundle.getString("avve.textpreprocess.wordFrequencyCounted"), numberOfKeys));

		statistics.appendStatistics(this.getClass(), sb.toString());
		return new String[] { inputText };
	}
	
    private static List<Entry<String, Integer>> sortByComparator(Map<String, Integer> unsortMap)
    {

        List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<String, Integer>>()
        {
            public int compare(Entry<String, Integer> o1,
                    Entry<String, Integer> o2)
            {
            	return o2.getValue().compareTo(o1.getValue());
            }
        });
        
        return list;
    }
}