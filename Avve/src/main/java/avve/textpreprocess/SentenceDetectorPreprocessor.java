package avve.textpreprocess;

import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

import org.apache.logging.log4j.Logger;

public class SentenceDetectorPreprocessor implements TextPreprocessor
{	
	private Logger logger;
	private SentenceModel model;
	
	public SentenceDetectorPreprocessor(Logger logger)
	{
		this.logger = logger;
		try (InputStream modelIn = this.getClass().getClassLoader().getResourceAsStream("opennlp/de-sent.bin"))
		{
			  model = new SentenceModel(modelIn);
		}
		catch (IOException exc)
		{
			this.logger.error(exc.getLocalizedMessage());
			exc.printStackTrace();
		}
	}

	@Override
	public String[] process(String inputText, TextStatistics statistics)
	{
		SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
		String[] sentences = sentenceDetector.sentDetect(inputText);
		
		String numberOfSentences = "number of sentences: " + sentences.length;
		statistics.appendStatistics(this.getClass(), numberOfSentences);
		
		return sentences;
	}
}