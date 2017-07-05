package avve.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import avve.textpreprocess.*;

public class DataPreprocessorService
{
	private Logger logger;
	private List<TextPreprocessor> preprocessors;
	
	public DataPreprocessorService(Logger logservice)
	{
		this.logger = logservice;
		this.preprocessors = new ArrayList<TextPreprocessor>();
		
		// TODO: for the time being, just configure the services here; if useful, refactor later to an approach using external configuration
		preprocessors.add(new SentenceDetectorPreprocessor(logger));
		preprocessors.add(new TextTokenizer(logger));
		preprocessors.add(new WordFrequencyPreprocessor(logger));
		preprocessors.add(new ToLowerCasePreprocessor());
	}

	/**
	 * This method runs a series of operations on the input text and returns a String representation of the results. The operations are
	 * configured in the constructor of DataPreprocessorServices. Statistical information on the text is stored in a TextStatistics object
	 * @param plainText The text to be examined
	 * @param statistics The object to hold statistical information on the text being processed.
	 * @return The processed and usually transformed text representation
	 */
	public String preProcessText(String plainText, TextStatistics statistics)
	{
		String intermediateText = plainText;
		
		for(TextPreprocessor tp : preprocessors)
		{
			String[] processorResult = tp.process(intermediateText, statistics);
			intermediateText = String.join(System.lineSeparator(), processorResult);
		}
		
		// handle POS tagging separately
		PartOfSpeechTagger posTagger = new PartOfSpeechTagger(logger);
		
		String[] pos = posTagger.process(intermediateText, statistics);
		intermediateText = intermediateText + System.lineSeparator() + String.join(" | ", pos);
		
		return intermediateText;
	}
}