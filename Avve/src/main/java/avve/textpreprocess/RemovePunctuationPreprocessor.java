package avve.textpreprocess;

import java.util.*;

import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;

public class RemovePunctuationPreprocessor implements TextPreprocessor
{
	private static final ResourceBundle errorMessagesBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	public RemovePunctuationPreprocessor(Logger logger)
	{
		this.logger = logger;
	}

	@Override
	public void process(EbookContentData contentData)
	{
		if(null == contentData.getTokens() || contentData.getTokens().length == 0)
		{
			logger.error(errorMessagesBundle.getString("avve.textpreprocess.noTokensAvailable"));
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
					String cleanedToken = contentData.getTokens()[i][j].replaceAll("[!\"$%&'()*+,./:;<=>?@\\[\\]^_`{|}~‘’‚‛„”“‟†‡•‣․%…‧’′″‴‵‶‷‸‹›⸮¿¡]", "");
					if(cleanedToken != null && cleanedToken.length() > 0)
					{
						tempTokenStore.add(cleanedToken);
					}
				}
				contentData.getTokens()[i] = tempTokenStore.toArray(new String[0]);
			}
		}
	}

	private Logger logger;
}