package avve.services;

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.Logger;

import avve.textpreprocess.TextPreprocessor;
import avve.textpreprocess.TextStatistics;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class TextTokenizer implements TextPreprocessor
{
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
	public String[] process(String inputText, TextStatistics statistics)
	{
		String[] tokens = tokenizer.tokenize(inputText);
		
		statistics.appendStatistics(this.getClass(), "number of tokens: " + tokens.length);
		
		return tokens;
	}
}