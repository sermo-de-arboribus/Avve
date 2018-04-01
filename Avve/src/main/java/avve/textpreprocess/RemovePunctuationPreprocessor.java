package avve.textpreprocess;

import java.util.*;

import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;

/**
 * This class cleans up punctuation that is left over after the tokenization process. It works on the EbookContentData's tokens (via 
 * getTokens()) and modifies them (via setTokens())
 * 
 * @author Kai Weber
 *
 */
public class RemovePunctuationPreprocessor implements TextPreprocessor
{
	private static final ResourceBundle errorMessagesBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	public RemovePunctuationPreprocessor(Logger logger)
	{
		this.logger = logger;
	}

	@Override
	public String getName()
	{
		return "RemovePunctuationPreprocessor";
	}
	
	@Override
	public void process(EbookContentData contentData)
	{
		if(null == contentData.getTokens() || contentData.getTokens().length == 0)
		{
			logger.error(String.format(errorMessagesBundle.getString("avve.textpreprocess.noTokensAvailable"), "RemovePunctuationPreprocessor.process()"));
		}
		else
		{
			logger.info(infoMessagesBundle.getString("avve.textpreprocess.removingPunctuation"));
			
			for(int i = 0; i < contentData.getTokens().length; i++)
			{
				// temporary storage for the current sentence
				ArrayList<String> tempTokenStore = new ArrayList<String>();
				
				// process all words in the current sentence
				for(int j = 0; j < contentData.getTokens()[i].length; j++)
				{
					String orig = contentData.getTokens()[i][j];
					String replaced = orig.replaceAll("[!\"$%&'()*+,./:;<=>?@\\[\\]^_`{|}~‘’‚‛„”“‟†‡•‣․%…‧’′″‴‵‶‷‸‹›⸮¿¡»«©·→←＿]", " ");
					String[] newTokens = replaced.split(" ");
					for(String token : newTokens)
					{
						if(!token.matches("^\\s*$"))
						{
							tempTokenStore.add(token);
						}	
					}
				}
				contentData.getTokens()[i] = tempTokenStore.toArray(new String[0]);
			}
		}
	}

	private Logger logger;
}