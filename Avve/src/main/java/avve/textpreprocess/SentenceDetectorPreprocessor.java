package avve.textpreprocess;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.logging.log4j.Logger;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

public class SentenceDetectorPreprocessor implements TextPreprocessor
{
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
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
			// TODO Auto-generated catch block
			exc.printStackTrace();
		}
	}

	@Override
	public String process(String inputText)
	{
		SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
		String[] sentences = sentenceDetector.sentDetect(inputText);
		StringBuilder sb = new StringBuilder();
		
		for(String sentence : sentences)
		{
			sb.append(sentence);
			sb.append(System.lineSeparator());
		}
		
		return sb.toString();
	}
}