package avve.extractor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import avve.epubhandling.EpubFile;
import avve.services.*;

public class EpubExtractor
{
	private static final Logger logger = LogManager.getLogger();
	private static final ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	public static void main(String[] args)
	{
		// parse and validate command line options
		CommandLineParser cliParser = new DefaultParser();
		Options options = new Options();
		options.addOption("i", "input", true, infoMessagesBundle.getString("explainInputOption"));
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
		try
		{
			EpubFile epubFile = new EpubFile(inputFile, fileService, logger);
			plainText = epubFile.extractPlainText();
		}
		catch (IOException exc)
		{
			logger.error(exc.getLocalizedMessage(), exc);
		}
		
		// Pre-process the text data
		plainText = plainText.toLowerCase(new Locale("de", "DE"));
		
		DataPreprocessorService textPreprocessor = new DataPreprocessorService(logger);
		String preprocessingResult = textPreprocessor.preProcessText(plainText);
		
		// save the processing result
		fileService.createDirectory("output");
		String outputFile = "output/" + FilenameUtils.getBaseName(inputFile) + ".txt";
		try
		{
			OutputStream out = fileService.createFileOutputStream(outputFile);
			PrintStream printStream = new PrintStream(out);
			printStream.print(preprocessingResult);
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
}