package avve.textpreprocess;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;

/**
 * This class replaces all URLs which can be found with the following regular expression:
 * 
 * (http|ftp|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?
 * 
 * It works on EbookContentData's plain text only. It leaves the protocol name http / ftp / https in the text and removes all following characters
 * 
 * @author Kai Weber
 *
 */
public class UrlNormalizer implements TextPreprocessor
{
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	private Logger logService;
	
	public UrlNormalizer(final Logger logger)
	{
		logService = logger;
		this.logService.debug(infoMessagesBundle.getString("avve.textpreprocess.urlNormalizerCreated"));	
	}
	
	@Override
	public String getName()
	{
		return "UrlNormalizer";
	}

	@Override
	public void process(EbookContentData contentData)
	{
		// using a regex suggested by CodeWrite on Stack Overflow: https://stackoverflow.com/questions/6038061/regular-expression-to-find-urls-within-a-string
		Pattern pattern = Pattern.compile("(http|ftp|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		Matcher matcher = pattern.matcher(contentData.getPlainText());
		    
		int numberOfReplacements = 0;
		StringBuffer sb = new StringBuffer();
		
		while(matcher.find())
		{
			numberOfReplacements++;
			matcher.appendReplacement(sb, "$1:// ");
		}
		matcher.appendTail(sb);
		
		contentData.setPlainText(sb.toString());
		this.logService.info(String.format(infoMessagesBundle.getString("avve.textpreprocess.urlsNormalized"), numberOfReplacements));	
	}
}