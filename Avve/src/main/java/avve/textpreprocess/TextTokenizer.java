package avve.textpreprocess;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;
import avve.textpreprocess.TextPreprocessor;
import opennlp.tools.tokenize.*;

public class TextTokenizer implements TextPreprocessor
{
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	private Logger logger;
	private Tokenizer tokenizer;
	
	public TextTokenizer(Logger logger)
	{
		this.logger = logger;
		
		try (InputStream modelIn = this.getClass().getClassLoader().getResourceAsStream("opennlp/de-token.bin"))
		{
			  TokenizerModel model = new TokenizerModel(modelIn);
			  tokenizer = new TokenizerME(model);
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
		logger.info(infoMessagesBundle.getString("avve.textpreprocess.tokenizingText"));
		
		String[][] tokenizedSentences = new String[ebookContentData.getSentences().length][];
		int counter = 0;
		
		for(int i = 0; i < tokenizedSentences.length; i++)
		{
			tokenizedSentences[i] = tokenizer.tokenize(ebookContentData.getSentences()[i]);
			counter += tokenizedSentences[i].length;
		}
		
		ebookContentData.setTokens(tokenizedSentences);
		ebookContentData.setNumberOfTokens(counter);
		
		logger.info(String.format(infoMessagesBundle.getString("avve.textpreprocess.numberOfTokensDetected"), counter));
	}
}