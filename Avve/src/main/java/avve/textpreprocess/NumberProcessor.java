package avve.textpreprocess;

import avve.epubhandling.EbookContentData;

/**
 * The NumberProcessor replaces all digits in an EbookContentData's plain text (via getPlainText()) and within the tokenized
 * text (via getTokens()) with a #-symbol (via setPlainText().
 * It should therefore run before other activities are executed on the plain text.
 * 
 * @author Kai Weber
 *
 */
public class NumberProcessor implements TextPreprocessor
{
	@Override
	public String getName()
	{
		return "NumberProcessor";
	}
	
	@Override
	public void process(EbookContentData contentData)
	{
		contentData.setPlainText(contentData.getPlainText().replaceAll("[0-9]", "#"));
		
		// check if we need to also process tokens
		if(null != contentData.getTokens() && contentData.getTokens().length > 0)
		{
			for(int i = 0; i < contentData.getTokens().length; i++)
			{
				for(int j = 0; j < contentData.getTokens()[i].length; j++)
				{
					contentData.getTokens()[i][j] = contentData.getTokens()[i][j].replaceAll("[0-9]", "#");
				}
			}
		}
	}
}