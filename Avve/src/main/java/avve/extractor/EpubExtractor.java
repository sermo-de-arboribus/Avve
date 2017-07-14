package avve.extractor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;
import avve.epubhandling.EpubFile;
import avve.services.*;

public class EpubExtractor
{
	private static final String language = "de";
	private static final Logger logger = LogManager.getLogger();
	private static final ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	public static void main(String[] args)
	{
		FileService fileService = new FileServiceImpl();
		
		// parse and validate command line options
		CommandLine cliArguments = parseCommandLineArguments(args);
		
		// build a list of files that need to be processed
		ArrayList<File> inputFiles = getCollectionOfInputFiles(fileService, cliArguments);
		
		// process all input files
		for(File inputFile : inputFiles)
		{
			logger.info(infoMessagesBundle.getString("startEpubExtraction") + ": " + inputFile);
			
			// read input files
			String plainText = "";
			String languageCode = null;
			try
			{
				EpubFile epubFile = new EpubFile(inputFile.getAbsolutePath(), fileService, logger);
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
						
				EbookContentData ebookContentData = new EbookContentData(plainText);
				
				textPreprocessor.preProcessText(ebookContentData);
				
				// save the processing result
				fileService.createDirectory("output");
				String warengruppe;
				if(cliArguments.hasOption(CommandLineArguments.WARENGRUPPE.toString()))
				{
					warengruppe = cliArguments.getOptionValue("wg") + "/";
				}
				else if(cliArguments.hasOption(CommandLineArguments.FOLDER.toString()))
				{
					warengruppe = inputFile.getParentFile().getName() + "/";
				}
				else
				{
					warengruppe = "";
				}
				
				String outputDirForFiles = "output/text/" + warengruppe;
				String outputDirForStatistics = "output/stats/" + warengruppe;
				
				String outputFile = outputDirForFiles + FilenameUtils.getBaseName(inputFile.getAbsolutePath()) + ".txt";
				String outputStatistics = outputDirForStatistics + FilenameUtils.getBaseName(inputFile.getAbsolutePath()) + ".txt";
				try
				{
					fileService.createDirectory(outputDirForFiles);
					fileService.createDirectory(outputDirForStatistics);
					
					OutputStream out1 = fileService.createFileOutputStream(outputFile);
					PrintStream printStream = new PrintStream(out1);
					printStream.print(ebookContentData.getPlainText());
					printStream.close();
					
					OutputStream out2 = fileService.createFileOutputStream(outputStatistics);
					printStream = new PrintStream(out2);
					if(cliArguments.hasOption("wg"))
					{
						printStream.print("warengruppe: " + cliArguments.getOptionValue("wg"));
						printStream.print(System.lineSeparator());
					}
					
					StringBuilder sb = new StringBuilder();
					sb.append(ebookContentData.getLemmaFrequencies() + System.lineSeparator());
					sb.append(ebookContentData.getPartsOfSpeechFrequencies() + System.lineSeparator());
					sb.append(ebookContentData.getWordFrequencies() + System.lineSeparator());
					sb.append(ebookContentData.getSentences().length + System.lineSeparator());
					sb.append(ebookContentData.getTokens().length + System.lineSeparator());
					
					printStream.print(sb.toString());
					printStream.close();
				}
				catch (FileNotFoundException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				finally
				{
					;
				}
			}
			else
			{
				logger.error(String.format(errorMessageBundle.getString("InvalidLanguage"), languageCode));
			}
			logger.info(infoMessagesBundle.getString("programFinished"));
		}

	}

	private static ArrayList<File> getCollectionOfInputFiles(FileService fileService, CommandLine cliArguments)
	{
		ArrayList<File> inputFiles = new ArrayList<File>();
		if(cliArguments.hasOption(CommandLineArguments.INPUT.toString()))
		{
			String inputFile = cliArguments.getOptionValue(CommandLineArguments.INPUT.toString());
			inputFiles.add(new File(inputFile));
		}
		else if(cliArguments.hasOption(CommandLineArguments.FOLDER.toString()));
		{
			String inputFolder = cliArguments.getOptionValue(CommandLineArguments.FOLDER.toString());
			inputFiles.addAll(fileService.getFilesFromAllSubdirectories(inputFolder));
		}
		return inputFiles;
	}

	private static Options getCommandLineOptions()
	{
		Options options = new Options();
		options.addOption(CommandLineArguments.INPUT.toString(), "input", true, infoMessagesBundle.getString("explainInputOption"));
		options.addOption(CommandLineArguments.FOLDER.toString(), "inputfolder", true, infoMessagesBundle.getString("explainInputFolderOption"));
		options.addOption(CommandLineArguments.WARENGRUPPE.toString(), "warengruppe", true, infoMessagesBundle.getString("explainWarengruppeOption"));
		return options;
	}
	
	private static CommandLine parseCommandLineArguments(String[] args)
	{
		CommandLineParser cliParser = new DefaultParser();
		Options options = getCommandLineOptions();
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
		return cliArguments;
	}
}