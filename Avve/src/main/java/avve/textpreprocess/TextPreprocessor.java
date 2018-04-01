package avve.textpreprocess;

import avve.epubhandling.EbookContentData;

/**
 * A text preprocessor either modifies the text of an EBookContentData object or it populates certain data of it.
 * 
 * @author Kai Weber
 *
 */
public interface TextPreprocessor
{
	/**
	 * Returns the name indicating the type of the concrete TextPreprocessor
	 * @return
	 */
	String getName();
	
	/**
	 * The implementing classes are expected to do their main work in this method. If they require the EbookContentData to be in a 
	 * certain status, they should check this first, log an error and recover graciously 
	 * 
	 * @param contentData
	 */
	void process(EbookContentData contentData);
}