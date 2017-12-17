package avve.textpreprocess;

import avve.epubhandling.EbookContentData;

public interface TextPreprocessor
{
	String getName();
	void process(EbookContentData contentData);
}