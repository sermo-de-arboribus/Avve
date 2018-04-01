package avve.textpreprocess;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.ResourceBundle;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;

/**
 * This is merely a wrapper class which delegates sentence detection to OpenNLP's maximum entropy sentence detector
 * It works on EbookContentData object's plain text and populates its sentences object (via setSentences())
 * 
 * @author Kai Weber
 *
 */
public class SentenceDetectorPreprocessor implements TextPreprocessor
{	
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	private Logger logger;
	private SentenceModel model;
	
	@Override
	public String getName()
	{
		return "SentenceDetectorPreprocessor";
	}
	
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
	public void process(EbookContentData ebookContentData)
	{
		logger.info(infoMessagesBundle.getString("avve.textpreprocess.sentenceDetectorStarted"));
		
		SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
		ebookContentData.setSentences(sentenceDetector.sentDetect(ebookContentData.getPlainText()));
		
		logger.info(String.format(infoMessagesBundle.getString("avve.textpreprocess.numberOfSentencesDetected"), ebookContentData.getSentences().length));
	}
}