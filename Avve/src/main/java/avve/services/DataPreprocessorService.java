package avve.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import avve.textpreprocess.SentenceDetectorPreprocessor;
import avve.textpreprocess.TextPreprocessor;
import avve.textpreprocess.WordFrequencyPreprocessor;

public class DataPreprocessorService
{
	private Logger logger;
	private List<TextPreprocessor> preprocessors;
	
	public DataPreprocessorService(Logger logservice)
	{
		this.logger = logservice;
		this.preprocessors = new ArrayList<TextPreprocessor>();
		
		// TODO: for the time being, just configure the services here; if useful, refactor later to an approach using external configuration
		preprocessors.add(new WordFrequencyPreprocessor(logger));
		preprocessors.add(new SentenceDetectorPreprocessor(logger));
	}

	public String preProcessText(String plainText)
	{
		StringBuilder sb = new StringBuilder();
		
		for(TextPreprocessor tp : preprocessors)
		{
			sb.append(tp.process(plainText));
			sb.append(System.lineSeparator());
		}
		
		return sb.toString();
	}
}
