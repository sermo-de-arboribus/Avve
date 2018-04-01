package avve.textpreprocess.hyperonym;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.logging.log4j.Logger;

/**
 * Helper class for handling access properties for the OpenThesaurus MySQL database. Used by the HyperonymPreprocessor class.
 * 
 * @author Kai Weber
 *
 */
public class HyperonymProperties
{
	private static final ResourceBundle errorMessagesBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final String PROPERTIES_FILE_PATH = "hyperonym/hyperonym.properties";
	private static HyperonymProperties singletonInstance;
	
	private final Properties hyperonymProperties = new Properties(); 
	private Logger logservice;
	
	public static synchronized HyperonymProperties getInstance(final Logger logservice)
	{
		if(null == singletonInstance)
		{
			singletonInstance = new HyperonymProperties(logservice);
		}
		return singletonInstance;
	}
	
	public synchronized String getProperty(final HyperonymPropertyName key)
	{
		return hyperonymProperties.getProperty(key.propertyKey);
	}
	
	private HyperonymProperties(final Logger logservice)
	{
		this.logservice = logservice;
		readHyperonymProperties();
	}
	
	private void readHyperonymProperties()
	{
		InputStream in = null;
		try
		{
			in = this.getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE_PATH);
			hyperonymProperties.load(in);
		}
		catch (IOException exc)
		{
			logservice.error(String.format(errorMessagesBundle.getString("avve.textpreprocess.hyperonym.couldNotLoadProperties"), PROPERTIES_FILE_PATH));
			logservice.error(exc);
		}
	}
}