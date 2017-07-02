package avve.textpreprocess;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.apache.logging.log4j.Logger;

public class TextStatisticsImpl implements TextStatistics
{
	private static final ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	
	private Logger logger;
	private Map<Class<?>, String> textStatisticsStrings;
	
	public TextStatisticsImpl(Logger logger)
	{
		this.logger = logger;
		textStatisticsStrings = new HashMap<Class<?>, String>();
	}
	
	@Override
	public void appendStatistics(Class<?> preprocessorClass, String statisticsString)
	{
		if(textStatisticsStrings.containsKey(preprocessorClass))
		{
			logger.warn(String.format(errorMessageBundle.getString("avve.textpreprocess.textStatisticsReplaced"), preprocessorClass.getName()));
		}
		textStatisticsStrings.put(preprocessorClass, statisticsString);
	}
	
	@Override
	public String getStatistics(Class<?> preprocessorClass)
	{
		return textStatisticsStrings.get(preprocessorClass);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		for(Entry<Class<?>, String> entry : textStatisticsStrings.entrySet())
		{
			sb.append(entry.getKey().getCanonicalName() + ":" + System.lineSeparator());
			sb.append(entry.getValue() + System.lineSeparator());
		}
		
		return sb.toString();
	}
}