package avve.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;
import avve.textpreprocess.*;

public class DataPreprocessorService
{
	private Logger logger;
	private List<TextPreprocessor> preprocessors;
	
	public DataPreprocessorService(Logger logservice)
	{
		this.logger = logservice;
		this.preprocessors = new ArrayList<TextPreprocessor>();
		
		
		preprocessors.add(new SentenceDetectorPreprocessor(logger));
		preprocessors.add(new TextTokenizer(logger));
		preprocessors.add(new WordFrequencyPreprocessor(logger));
		preprocessors.add(new ToLowerCasePreprocessor(logger));
	}

	/**
	 * This method runs a series of operations on the input text and returns a String representation of the results. The operations are
	 * configured in the constructor of DataPreprocessorServices. Statistical information on the text is stored in a TextStatistics object
	 * @param plainText The text to be examined
	 * @param statistics The object to hold statistical information on the text being processed.
	 * @return The processed and usually transformed text representation
	 */
	public void preProcessText(EbookContentData ebookContentData)
	{
		// TODO: for the time being, just configure the services here; if useful, refactor later to an approach using external configuration
		
		new SentenceDetectorPreprocessor(logger).process(ebookContentData);
		new TextTokenizer(logger).process(ebookContentData);
		new WordFrequencyPreprocessor(logger).process(ebookContentData);
		new ToLowerCasePreprocessor(logger).process(ebookContentData);
		new PartOfSpeechTagger(logger).process(ebookContentData);
		new Lemmatizer(logger).process(ebookContentData);
	}
}