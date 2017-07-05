package avve.textpreprocess;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.logging.log4j.Logger;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

public class PartOfSpeechTagger implements TextPreprocessor
{
	private static final ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	POSTaggerME tagger;
	Logger logger;
	
	public PartOfSpeechTagger(Logger logger)
	{
		this.logger = logger;
		
		try (InputStream modelIn = this.getClass().getClassLoader().getResourceAsStream("opennlp/de-pos-maxent.bin"))
		{
			POSModel model = new POSModel(modelIn);
			tagger = new POSTaggerME(model);
		}
		catch (IOException exc)
		{
			this.logger.error(errorMessageBundle.getString("PartOfSpeechTaggerModelInitError"));
			exc.printStackTrace();
		}
	}
	
	@Override
	public String[] process(String inputText, TextStatistics statistics)
	{
		TextTokenizer tokenizer = new TextTokenizer(this.logger);
		String[] tokens = tokenizer.process(inputText, statistics);
		
		return process(tokens, statistics);
	}

	public String[] process(String[] inputTokens, TextStatistics statistics)
	{
		String partOfSpeechTags[] = tagger.tag(inputTokens);
		
		HashMap<String, Integer> posMap = new HashMap<>();
		
		for(String tag : partOfSpeechTags)
		{
			if(posMap.containsKey(tag))
			{
				posMap.put(tag, posMap.get(tag) + 1);
			}
			else
			{
				posMap.put(tag, 1);
			}
		}
		
		StringBuilder sb = new StringBuilder();
		int numberOfKeys = 0;
		
		for(String key : posMap.keySet())
		{
			sb.append(key + " : ");
			sb.append(posMap.get(key));
			sb.append(System.lineSeparator());
			numberOfKeys++;
		}
		
		logger.trace(String.format(infoMessagesBundle.getString("avve.textpreprocess.partOfSpeechTaggerDifferentPos"), numberOfKeys));

		statistics.appendStatistics(this.getClass(), sb.toString());
		
		return partOfSpeechTags;
	}
}