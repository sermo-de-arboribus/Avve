package avve.textpreprocess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.SortedMap;

import org.annolab.tt4j.TreeTaggerException;
import org.annolab.tt4j.TreeTaggerWrapper;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;

public class Lemmatizer implements TextPreprocessor
{
	private Logger logger;
	
	private static final ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());

	private static int sentencePointer = 0;
	private static int tokenPointer = 0;
	
	public Lemmatizer(Logger logger)
	{
		this.logger = logger;
	}
	
	@Override
	public void process(EbookContentData ebookContentData)
	{
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
				for(String[] tokenizedSentence : ebookContentData.getTokens())
				{
					lemmaArray[sentencePointer] = new String[tokenizedSentence.length];
					posArray[sentencePointer] = new String[tokenizedSentence.length];
					treeTagger.process(tokenizedSentence);
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
				File outputDirectory = new File("debug/postag/" + ebookContentData.getWarengruppe() + "/");
				outputDirectory.mkdirs();
				FileWriter fw = new FileWriter(FilenameUtils.concat(outputDirectory.getAbsolutePath(), ebookContentData.getDocumentId()));
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
