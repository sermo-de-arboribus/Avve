package avve.textpreprocess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.SortedMap;

import org.apache.commons.cli.CommandLine;
import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;
import avve.extractor.CommandLineArguments;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

public class PartOfSpeechTagger implements TextPreprocessor
{
	private static final ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	CommandLine cliArguments;
	HashMap<String, String> correctionMap;
	private Logger logger;
	private POSTaggerME tagger;
	
	public PartOfSpeechTagger(Logger logger, CommandLine cliArguments)
	{
		this.cliArguments = cliArguments;
		this.logger = logger;
		
		try (InputStream modelIn = this.getClass().getClassLoader().getResourceAsStream("opennlp/de-pos-maxent.bin"))
		{
			POSModel model = new POSModel(modelIn);
			tagger = new POSTaggerME(model);
		}
		catch (IOException exc)
		{
			this.logger.error(errorMessageBundle.getString("avve.textpreprocess.partOfSpeechTaggerModelInitError"), exc);
		}
		
		// if we have a command line argument set for POS-tag correction, we use a resource text file for correction values
		if(cliArguments.hasOption(CommandLineArguments.POSCORRECTION.toString()))
		{
			correctionMap = new HashMap<String, String>();
			
			try (InputStream modelIn = this.getClass().getClassLoader().getResourceAsStream("opennlp/postag-de-dict.txt"))
			{
				BufferedReader reader = new BufferedReader(new InputStreamReader(modelIn, "UTF-8"));
				String line;
				while((line = reader.readLine()) != null)
				{
					if(!line.startsWith("//"))
					{
						String[] tokens = line.split("\\s+");
						correctionMap.put(tokens[0] + "_" + tokens[1], tokens[2]);
					}
				}
			}
			catch (IOException exc)
			{
				this.logger.error(errorMessageBundle.getString("avve.textpreprocess.PartOfSpeechTaggerCorrectionFileInitError"), exc);
			}
		}
	}
	
	@Override
	public void process(EbookContentData ebookContentData)
	{
		if(ebookContentData.getTokens() == null || ebookContentData.getTokens().length == 0)
		{
			this.logger.error(String.format(errorMessageBundle.getString("avve.textpreprocess.noTokensAvailable"), "PartOfSpeechTagger.process()"));
		}
		else
		{
			boolean isCorrectionRequired = cliArguments.hasOption(CommandLineArguments.POSCORRECTION.toString()) && null != correctionMap;
			
			String[][] partsOfSpeech = new String[ebookContentData.getTokens().length][];
			
			for(int i = 0; i < partsOfSpeech.length; i++)
			{
				String[] currentSentence = ebookContentData.getTokens()[i];
				partsOfSpeech[i] = tagger.tag(currentSentence);
				
				// if we have a command line argument set for POS-tag correction, we process the tagging results through a replacement table
				if(isCorrectionRequired)
				{
					for(int j = 0; j < partsOfSpeech[i].length; j++)
					{
						String tokenAndPosTag = ebookContentData.getTokens()[i][j].toLowerCase() + "_" + partsOfSpeech[i][j];
						if(correctionMap.containsKey(tokenAndPosTag))
						{
							partsOfSpeech[i][j] = correctionMap.get(tokenAndPosTag);
						}
					}
				}
			}
			ebookContentData.setPartsOfSpeech(partsOfSpeech);
			
			SortedMap<String, Integer> posMap = ebookContentData.getPartsOfSpeechFrequencies();
			
			for(String[] sentence : ebookContentData.getPartsOfSpeech())
			{
				for(String tag : sentence)
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
			}
			
			logger.trace(String.format(infoMessagesBundle.getString("avve.textpreprocess.partOfSpeechTaggerDifferentPos"), posMap.keySet().size()));
		}
	}
}