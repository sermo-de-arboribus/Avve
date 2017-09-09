package avve.textpreprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.SortedMap;

import org.annolab.tt4j.TreeTaggerException;
import org.annolab.tt4j.TreeTaggerWrapper;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;
import avve.extractor.CommandLineArguments;

public class Lemmatizer implements TextPreprocessor
{
	private static final ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	private static int sentencePointer = 0;
	private static int tokenPointer = 0;
	
	private CommandLine cliArguments;
	HashMap<String, String> correctionMap;
	private Logger logger;
		
	public Lemmatizer(Logger logger, CommandLine cliArguments)
	{
		this.cliArguments = cliArguments;
		this.logger = logger;
		
		// if we have a command line argument set for POS-tag correction, we use a resource text file for correction values
		if(cliArguments.hasOption(CommandLineArguments.LEMMACORRECTION.toString()))
		{
			correctionMap = new HashMap<String, String>();
			
			try (InputStream modelIn = this.getClass().getClassLoader().getResourceAsStream("opennlp/lemmatizer-de-dict.txt"))
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
				this.logger.error(errorMessageBundle.getString("avve.textpreprocess.LemmatizerCorrectionFileInitError"), exc);
			}
		}
	}
	
	@Override
	public void process(EbookContentData ebookContentData)
	{
		boolean isCorrectionRequired = cliArguments.hasOption(CommandLineArguments.POSCORRECTION.toString()) && null != correctionMap;
		
		// TODO: remove later, this is just for testing
		String[][] posArray;
		
		// check if we already have a tokenized text as input for the lemmatizer
		if(ebookContentData.getTokens() == null || ebookContentData.getTokens().length == 0)
		{
			this.logger.error(errorMessageBundle.getString("avve.textpreprocess.noTokensAvailable"));
		}
		else
		{
			logger.info(String.format(infoMessagesBundle.getString("avve.textpreprocess.lemmatizerStarted"), ebookContentData.getDocumentId()));
		    // unfortunately this is a system-dependent external tool
			// we're currently using this TreeTagger lemmatizer, because it provides a German lemma model, whereas the OpenNLP lemmatizer has no such pre-built model
			// however, we're using a custom dictionary-based OpenNLP lemmatizer as a post-processor for improving data
		    if(System.getProperty("treetagger.home") == null || System.getProperty("treetagger.home").equals(""))
		    {
				System.setProperty("treetagger.home", "/home/kai/TreeTagger");	
		    }
			TreeTaggerWrapper<String> treeTagger = new TreeTaggerWrapper<String>();
			String[][] lemmaArray = new String[ebookContentData.getTokens().length][];
			posArray = new String[ebookContentData.getTokens().length][]; // test code
			
			try
			{
				treeTagger.setModel("german-utf8.par");
				treeTagger.setHandler((token, pos, lemma) -> 
				{
					lemmaArray[sentencePointer][tokenPointer] = lemma;
					posArray[sentencePointer][tokenPointer] = pos;
					tokenPointer++;
				});
				for(int i = 0; i < ebookContentData.getTokens().length; i++)
				{
					String[] tokenizedSentence = ebookContentData.getTokens()[i];
					
					lemmaArray[sentencePointer] = new String[tokenizedSentence.length];
					posArray[sentencePointer] = new String[tokenizedSentence.length];
					treeTagger.process(tokenizedSentence);
					
					// if we have a command line argument set for lemma correction, we process the tagging results through a replacement table
					if(isCorrectionRequired)
					{
						for(int j = 0; j < lemmaArray[sentencePointer].length; j++)
						{
							String lemmaAndPosTag = lemmaArray[sentencePointer][j].toLowerCase() + "_" + ebookContentData.getPartsOfSpeech()[sentencePointer][j];
							if(correctionMap.containsKey(lemmaAndPosTag))
							{
								lemmaArray[sentencePointer][j] = correctionMap.get(lemmaAndPosTag);
							}
						}
					}
					
					sentencePointer++;
					tokenPointer = 0;
				}
			}
			catch (IOException exc)
			{
				logger.error(errorMessageBundle.getString("avve.textpreprocess.lemmatizerIoException"), exc);
			}
			catch (TreeTaggerException exc)
			{
				logger.error(exc.getLocalizedMessage(), exc);
			}
			finally
			{
				treeTagger.destroy();
				sentencePointer = 0;
				tokenPointer = 0;
			}
			
			SortedMap<String, Integer> lemmaFrequencies = ebookContentData.getLemmaFrequencies();
			
			for(int i = 0; i < lemmaArray.length; i++)
			{
				for(int j = 0; j < lemmaArray[i].length; j++)
				{
					String key = lemmaArray[i][j];
					if(null != key)
					{
						if(lemmaFrequencies.containsKey(key))
						{
							lemmaFrequencies.put(key, lemmaFrequencies.get(key) + 1);
						}
						else
						{
							lemmaFrequencies.put(key, 1);
						}	
					}
				}
			}
			
			ebookContentData.setLemmas(lemmaArray);
			
			//TODO test output, remove later
			StringBuffer sb = new StringBuffer();
			
			for(int i = 0; i < posArray.length; i++)
			{
				for(int j = 0; j < posArray[i].length; j++)
				{
					sb.append(ebookContentData.getLemmas()[i][j]);
					sb.append("_");
					sb.append(posArray[i][j]);
					sb.append(" ");
				}
				sb.append(System.lineSeparator());
			}
			try
			{
				File outputDirectory = new File("output/debug/postag/" + ebookContentData.getWarengruppe() + "/");
				outputDirectory.mkdirs();
				FileWriter fw = new FileWriter(FilenameUtils.concat(outputDirectory.getAbsolutePath(), ebookContentData.getDocumentId().replaceAll(":", "_") + ".txt"));
				fw.write(sb.toString());
				fw.close();
			}
			catch (FileNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			logger.info(String.format(infoMessagesBundle.getString("avve.textpreprocess.numberOfLemmasDetected"), lemmaFrequencies.size()));
		}
	}
}
