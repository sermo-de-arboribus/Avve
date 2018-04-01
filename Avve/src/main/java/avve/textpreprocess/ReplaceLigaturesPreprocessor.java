package avve.textpreprocess;

import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;

/**
 * This preprocessor works on tokenized data from EbookContentData and replaces typographic ligatures (e. g. ﬁ) with corresponding
 * single characters (e. g. fi)
 * 
 * @author Kai Weber
 *
 */
public class ReplaceLigaturesPreprocessor  implements TextPreprocessor
{
	private static final ResourceBundle errorMessagesBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	public ReplaceLigaturesPreprocessor(Logger logger)
	{
		this.logger = logger;
	}
	
	@Override
	public String getName()
	{
		return "ReplaceLigaturesPreprocessor";
	}
	
	@Override
	public void process(EbookContentData contentData)
	{
		if(null == contentData.getTokens() || contentData.getTokens().length == 0)
		{
			logger.error(String.format(errorMessagesBundle.getString("avve.textpreprocess.noTokensAvailable"), "ReplaceLigaturesPreprocessor.process()"));
		}
		else
		{
			logger.info(infoMessagesBundle.getString("avve.textpreprocess.replacingLigatures"));
			
			// process all sentences
			for(int i = 0; i < contentData.getTokens().length; i++)
			{
				// process all words in the current sentence
				for(int j = 0; j < contentData.getTokens()[i].length; j++)
				{
					String token = contentData.getTokens()[i][j];
					token = token.replaceAll("ﬂ", "fl");
					token = token.replaceAll("ﬁ", "fi");
					token = token.replaceAll("ﬀ", "ff");
					token = token.replaceAll("ﬃ", "ffi");
					token = token.replaceAll("ﬄ", "ffl");
					token = token.replaceAll("ﬆ", "st");
					token = token.replaceAll("ﬅ", "st");
					contentData.getTokens()[i][j] = token;
				}
			}
		}
	}
	
	private Logger logger;
}