package avve.extractor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import avve.epubhandling.EpubFile;
import avve.services.*;
import avve.textpreprocess.TextStatistics;
import avve.textpreprocess.TextStatisticsImpl;

public class EpubExtractor
{
	private static final String language = "de";
	private static final Logger logger = LogManager.getLogger();
	private static final ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	public static void main(String[] args)
	{
		// parse and validate command line options
		CommandLineParser cliParser = new DefaultParser();
		Options options = new Options();
		options.addOption("i", "input", true, infoMessagesBundle.getString("explainInputOption"));
		options.addOption("wg", "warengruppe", false, infoMessagesBundle.getString("explainWarengruppeOption"));
		CommandLine cliArguments = null;
		try
		{
			cliArguments = cliParser.parse(options, args);
		}
		catch (ParseException exc)
		{
			String header = errorMessageBundle.getString("HelpMessageHeader");
			String footer = errorMessageBundle.getString("HelpMessageFooter");
			 
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Avve", header, options, footer, true);
			logger.error(exc.getLocalizedMessage(), exc);
		}
		String inputFile = cliArguments.getOptionValue("i");
		
		// read input file
		FileService fileService = new FileServiceImpl();
		String plainText = "";
		String languageCode = null;
		try
		{
			EpubFile epubFile = new EpubFile(inputFile, fileService, logger);
			plainText = epubFile.extractPlainText();
			languageCode = epubFile.getLanguageCode();
		}
		catch (IOException exc)
		{
			logger.error(exc.getLocalizedMessage(), exc);
		}
		
		if(languageCode.equals(language))
		{
			// Pre-process the text data
			DataPreprocessorService textPreprocessor = new DataPreprocessorService(logger);
			TextStatistics statistics = new TextStatisticsImpl(logger);
					
			String preprocessingResult = textPreprocessor.preProcessText(plainText, statistics);
			
			// save the processing result
			fileService.createDirectory("output");
			String outputFile = "output/text/" + FilenameUtils.getBaseName(inputFile) + ".txt";
			String outputStatistics = "output/stats/" + FilenameUtils.getBaseName(inputFile) + "_statistics.txt";
			try
			{
				fileService.createDirectory("output/text/");
				fileService.createDirectory("output/stats/");
				
				OutputStream out1 = fileService.createFileOutputStream(outputFile);
				PrintStream printStream = new PrintStream(out1);
				printStream.print(preprocessingResult);
				printStream.close();
				
				OutputStream out2 = fileService.createFileOutputStream(outputStatistics);
				printStream = new PrintStream(out2);
				if(cliArguments.hasOption("wg"))
				{
					printStream.print("warengruppe: " + cliArguments.getOptionValue("wg"));
				}
				printStream.print(statistics.toString());
				printStream.close();
			}
			catch (FileNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally
			{
				logger.info(infoMessagesBundle.getString("programFinished"));
			}
		}
		else
		{
			logger.error(String.format(errorMessageBundle.getString("InvalidLanguage"), languageCode));
		}
	}
}