package avve.textpreprocess;

import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;

public class ToLowerCasePreprocessor implements TextPreprocessor
{
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	private Logger logger;
	
	public ToLowerCasePreprocessor(Logger logger)
	{
		this.logger = logger;
	}
	
	@Override
	public String getName()
	{
		return "ToLowerCasePreprocessor";
	}
	
	@Override
	public void process(EbookContentData ebookContentData)
	{
		logger.info(infoMessagesBundle.getString("avve.textpreprocess.toLowerCaseStarted"));
		
		ebookContentData.setPlainText(ebookContentData.getPlainText().toLowerCase(new Locale("de", "DE")));
		
		logger.info(infoMessagesBundle.getString("avve.textpreprocess.toLowerCaseEnded"));
	}
}