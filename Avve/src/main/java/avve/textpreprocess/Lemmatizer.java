package avve.textpreprocess;

import java.io.BufferedReader;
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
import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;
import avve.extractor.CommandLineArguments;

/**
 * This class lemmatizes a tokenized String[][] of an EbookContentData object. It uses TreeTagger (see http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/),
 * so TreeTagger must be installed in your system. Furthermore it assumes that when starting Avve, a Java system property is set, pointing to
 * the directory where TreeTagger is installed. The Java system property key is "treeTagger.home". This can be achieved at startup of Avve
 * by adding the following to the start parameters:
 * 
 * -Dtreetagger.home="C:\TreeTagger"
 * 
 * (Adjust the path to where your TreeTagger has been installed)
 * 
 * The output of the TreeTagger can be corrected with the help of a triplet replacement list, which is stored in the resources folder under
 * .../src/main/resources/opennlp/lemmatizer-de-dict.txt. If you want this correction file to be used, pass the command line parameter flag -lc
 * on startup of Avve.
 * 
 * @author Kai Weber
 *
 */
public class Lemmatizer implements TextPreprocessor
{
	private static final ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	private static int sentencePointer = 0;
	private static int tokenPointer = 0;
	
	private CommandLine cliArguments;
	HashMap<String, String> correctionMap;
	private Logger logger;
	private TreeTaggerWrapper<String> treeTagger;
		
	public Lemmatizer(Logger logger, CommandLine cliArguments)
	{
		this.cliArguments = cliArguments;
		this.logger = logger;
		this.treeTagger = new TreeTaggerWrapper<String>();
		try
		{
			treeTagger.setModel("german-utf8.par");
		} 
		catch (IOException exc)
		{
			logger.error(errorMessageBundle.getString("avve.textpreprocess.lemmatizerIoException"), exc);
		}
				
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
	public String getName()
	{
		return "Lemmatizer";
	}
	
	/**
	 * Takes the EbookContentData object's tokenized text (via getTokens()) and stores the lemmatized version (via setLemmas())
	 */
	@Override
	public void process(EbookContentData ebookContentData)
	{
		boolean isCorrectionRequired = cliArguments.hasOption(CommandLineArguments.POSCORRECTION.toString()) && null != correctionMap;
		
		// check if we already have a tokenized text as input for the lemmatizer
		if(ebookContentData.getTokens() == null || ebookContentData.getTokens().length == 0)
		{
			ebookContentData.setLemmas(new String[0][0]);
			this.logger.error(String.format(errorMessageBundle.getString("avve.textpreprocess.noTokensAvailable"), "Lemmatizer.process()"));
		}
		else
		{
			logger.info(String.format(infoMessagesBundle.getString("avve.textpreprocess.lemmatizerStarted"), ebookContentData.getDocumentId()));
		    // unfortunately this is a system-dependent external tool
			// we're currently using this TreeTagger lemmatizer, because it provides a German lemma model, whereas the OpenNLP lemmatizer has no such pre-built model
			// however, we're using a custom dictionary-based OpenNLP lemmatizer as a post-processor for improving data
			String[][] lemmaArray = new String[ebookContentData.getTokens().length][];
			
			try
			{
				treeTagger.setHandler((token, pos, lemma) -> 
				{
					lemmaArray[sentencePointer][tokenPointer] = lemma;
					tokenPointer++;
				});
				for(int i = 0; i < ebookContentData.getTokens().length; i++)
				{
					String[] tokenizedSentence = ebookContentData.getTokens()[i];
					
					lemmaArray[sentencePointer] = new String[tokenizedSentence.length];
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
				sentencePointer = 0;
				tokenPointer = 0;
			}
			
			// lemmaFrequencies only calculated for logging purposes
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
			
			logger.info(String.format(infoMessagesBundle.getString("avve.textpreprocess.numberOfLemmasDetected"), lemmaFrequencies.size()));
		}
	}
	
	@Override
	protected void finalize()
	{
		treeTagger.destroy();
	}
}