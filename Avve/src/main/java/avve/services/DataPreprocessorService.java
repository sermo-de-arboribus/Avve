package avve.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.cli.CommandLine;
import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;
import avve.extractor.CommandLineArguments;
import avve.textpreprocess.*;

public class DataPreprocessorService
{	
	public DataPreprocessorService(final Logger logservice, final CommandLine cliArguments)
	{
		this.logger = logservice;
		
		logger.info(infoMessagesBundle.getString("avve.services.configuringDataPrepreprocessorService"));
		
		preprocessorQueue = new ArrayList<TextPreprocessor>(12);
		
		// TODO: for the time being, just configure the services here; if useful, refactor later to an approach using external configuration
		preprocessorQueue.add(new SentenceDetectorPreprocessor(logger));
		preprocessorQueue.add(new TextTokenizer(logger));
		preprocessorQueue.add(new RemovePunctuationPreprocessor(logger));
		preprocessorQueue.add(new WordFrequencyPreprocessor(logger));
		preprocessorQueue.add(new PartOfSpeechTagger(logger, cliArguments));
		preprocessorQueue.add(new NumberProcessor());
		preprocessorQueue.add(new Lemmatizer(logger, cliArguments));
		if(cliArguments.hasOption(CommandLineArguments.USETHESAURUS.toString()))
		{
			preprocessorQueue.add(new HyperonymPreprocessor(logger));
		}
		// preprocessorQueue.add(new ToLowerCasePreprocessor(logger));
		
		for(TextPreprocessor preprocessor : preprocessorQueue)
		{
			logger.info(String.format(infoMessagesBundle.getString("avve.services.textpreProcessorAdded"), preprocessor.getName()));
		}
	}

	/**
	 * This method runs a series of operations on the input text and returns a String representation of the results. The operations are
	 * configured in the constructor of DataPreprocessorServices. Statistical information on the text is stored in a TextStatistics object
	 * @param plainText The text to be examined
	 * @param statistics The object to hold statistical information on the text being processed.
	 * @return The processed and usually transformed text representation
	 */
	public void preProcessText(final EbookContentData ebookContentData)
	{
		for(TextPreprocessor preprocessor : preprocessorQueue)
		{
			preprocessor.process(ebookContentData);
		}
	}
	
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	private Logger logger;
	private List<TextPreprocessor> preprocessorQueue;
}