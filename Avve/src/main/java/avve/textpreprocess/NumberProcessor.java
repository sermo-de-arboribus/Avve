package avve.textpreprocess;

import avve.epubhandling.EbookContentData;

public class NumberProcessor implements TextPreprocessor
{
	@Override
	public void process(EbookContentData contentData)
	{
		contentData.setPlainText(contentData.getPlainText().replaceAll("[0-9]", "#"));
	}
}