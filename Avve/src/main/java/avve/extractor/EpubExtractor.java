package avve.extractor;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
		
		// read input file
		FileService fileService = new FileServiceImpl();
		try
		{
			EpubFile epubFile = new EpubFile(cliArguments.getOptionValue("i"), fileService, logger);
			String plainText = epubFile.extractPlainText();
			logger.trace(plainText);
		}
		catch (IOException exc)
		{
			logger.error(exc.getLocalizedMessage(), exc);
		}
		finally
		{
			logger.info(infoMessagesBundle.getString("programFinished"));
		}
	}
}