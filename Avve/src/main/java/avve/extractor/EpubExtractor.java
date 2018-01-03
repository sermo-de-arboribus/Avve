package avve.extractor;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;
import avve.epubhandling.EpubFile;
import avve.services.*;
import avve.services.lucene.LuceneService;

/**
 * This main class provides an entry point for extracting text from EPUB files, applying a series of data transformations and writing the result 
 * to output files, which can be used for later usage by data mining and/or machine learning programs.
 * 
 * EpubExtractor expects to receive a folder path as an input argument. The folder is supposed to contain sub-folders, which are named after 
 * the classes that the EPUB documents belong to. The sub-folders then contain the actual EPUB files.
 * 
 * The EPUB files used as input should conform to the EPUB standard (see http://idpf.org/epub); all versions starting from EPUB 2.0 should work. 
 * EpubExtractor only parses a subset of the EPUB elements: container.xml, the opf file, and all (X)HTML files listed in the OPF. Furthermore
 * the document ID and the document language will be determined via metadata from the OPF file.
 * 
 * If the EPUB's document language, as specified in the OPF file's <dc:language> element, does not match the EpubExtractor's "language" constant,
 * the EPUB document will be skipped.
 * 
 * EpubExtractor uses log4j for producing more or less verbose output of what it does. Configuration of the log-level and output options can be
 * stored in the "resources" folder's "log4j2.xml" file.
 * 
 * @author "Kai Weber"
 *
 */
public class EpubExtractor
{
	private static final String language = "de";
	private static final Logger logger = LogManager.getLogger();
	private static final ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	private static FileService fileService = new FileServiceImpl();
	private static final int wordVectorSizeDefaultValue = 200;
	private static XmlService xmlService = new XmlService(fileService, logger);
	private static String statsDirectory = "output/stats";
	private static String textDirectory = "output/text";
	private static ControlledVocabularyService controlledVocabularyService = null;
	private static LuceneService luceneService = new LuceneService(logger, fileService);
	private static DataPreprocessorService textPreprocessor = null;
	
	/**
	 * The main method parses all EPUB files in the input folder (INPUT command line argument) and writes the respective output after
	 * transformations and statistics have been built.
	 * 
	 * 	For complete information on command line arguments see getCommandLineOptions() in the CommandLineArguments class
	 * 
	 * @param args Command line arguments: "i" a single input file, "wg" a Warengruppe class label for the input file, "folder" an input folder, with subfolders named after the class labels for the input files contained within each subfolder
	 */
	public static void main(final String[] args)
	{
		// prepare some execution time statistics
		LocalDateTime startTime = LocalDateTime.now();
		long startTimestamp = System.currentTimeMillis();
		logger.info(String.format(infoMessagesBundle.getString("avve.extractor.started"), startTime));
		
		// parse and validate command line options
		CommandLine cliArguments = parseCommandLineArguments(args);
		
		// build a list of files that need to be processed
		ArrayList<File> inputFiles = getCollectionOfInputFiles(fileService, cliArguments);

		logger.info(String.format(infoMessagesBundle.getString("avve.extractor.numberOfFilesToProcess"), inputFiles.size()));
		
		if(cliArguments.hasOption(CommandLineArguments.CONTROLLEDVOCABULARY.toString()))
		{
			try
			{
				controlledVocabularyService = new ControlledVocabularyService(cliArguments.getOptionValue(CommandLineArguments.CONTROLLEDVOCABULARY.toString()), fileService, logger);
			}
			catch (IOException exc)
			{
				logger.error(exc.getLocalizedMessage(), exc);
				System.exit(1);
			}
		}
		
		// process all input files, first run: preprocess input files, push text to Lucene index, write serialized temp files
		for(File inputFile : inputFiles)
		{
			logger.info(infoMessagesBundle.getString("avve.extractor.startEpubExtraction") + ": " + inputFile);

			// parse input files
			String plainText = "";
			String languageCode = null;
			EpubFile epubFile = null;
			try
			{
				epubFile = new EpubFile(inputFile.getAbsolutePath(), fileService, logger);
				plainText = epubFile.extractPlainText();
				languageCode = epubFile.getLanguageCode();
			}
			catch (IOException exc)
			{
				logger.error(exc.getLocalizedMessage(), exc);
			}
			
			// determine "warengruppe" class code, either from command line parameter or from folder name
			String warengruppe = determineClassName(cliArguments, inputFile);
			
			// Pre-process the text data (e.g. tokenization, sentence detection, part-of-speech tagging
			EbookContentData ebookContentData = preprocessText(plainText, epubFile, warengruppe, cliArguments);
			
			if(null != epubFile && languageCode.equals(language))
			{
				String lemmatizedText = ebookContentData.getLemmatizedText();
				
				// add the text to a Lucene index (for TF/IDF retrieval)
				luceneService.addTextToLuceneIndex(ebookContentData, language, cliArguments.hasOption(CommandLineArguments.DONOTINDEXFOREIGNWORDS.toString()));
				
				if(lemmatizedText.length() > 0)
				{
					// serialize temporary file to disk
					serializeTempEbookContentFileToDisk(inputFile, warengruppe, ebookContentData);	
				}
				else
				{
					logger.error(String.format(errorMessageBundle.getString("avve.textpreprocess.noTokensAvailable"), "EpubExtractor.serializeTempEbookContentFileToDisk()"));
				}
			}
			else
			{
				logger.error(String.format(errorMessageBundle.getString("InvalidLanguage"), languageCode));
			}
		}
		
		ArrayList<File> preprocessedFiles = getCollectionOfSerializedTempFiles(fileService, "output/temp/");
		// second iteration: build statistics and write xrff files for Weka or Meka data mining
		for(File preprocessedFile : preprocessedFiles)
		{
			logger.info(infoMessagesBundle.getString("avve.extractor.startWorkingOnSerializedTempFiles") + ": " + preprocessedFile);
			
			InputStream fileInputStream = null;
			EbookContentData ebookContentData = null;
			
			try
			{
				fileInputStream = fileService.createFileInputStream(preprocessedFile.getAbsolutePath());

				ObjectInputStream objectInputStream = new ObjectInputStream( fileInputStream );
				ebookContentData = (EbookContentData) objectInputStream.readObject();
			}
			catch (IOException exc)
			{
				logger.error(exc.getLocalizedMessage());
			}
			catch (ClassNotFoundException exc)
			{
				logger.error(exc.getLocalizedMessage());
			}
			finally
			{
				fileService.safeClose(fileInputStream);
			}
			
			if(null != ebookContentData && ebookContentData.getLanguage().equals(language))
			{
				// determine "warengruppe" class code, either from command line parameter or from folder name
				String warengruppe = ebookContentData.getWarengruppe();
				
				// save the processing result to the file system, one file with plain text, one file with statistical attributes
				writePreprocessingResultsToFileSystem(warengruppe, ebookContentData, preprocessedFile, cliArguments);
			}
			else
			{
				logger.error(String.format(errorMessageBundle.getString("InvalidLanguage"), ebookContentData.getLanguage()));
			}
		}
		
		if(cliArguments.hasOption(CommandLineArguments.MULTILABEL.toString()))
		{
			// combine all xrff files written in the previous step and save them as a single multilabel file Meka in ARFF format
			xmlService.createCombinedMultiClassFile(getCollectionOfClassNames(fileService, cliArguments));
		}
		else
		{
			// combine all xrff files written in the previous step and save them as a single file for Weka in XRFF format
			xmlService.combineXrffFiles(getCollectionOfClassNames(fileService, cliArguments));	
		}

		LocalDateTime endTime = LocalDateTime.now();
		long endTimestamp = System.currentTimeMillis();
		
		logger.info(String.format(infoMessagesBundle.getString("avve.extractor.executionTime"), (endTimestamp - startTimestamp) / 1000));
		logger.info(String.format(infoMessagesBundle.getString("avve.extractor.programFinished"), endTime));
	}

	private static String determineClassName(CommandLine cliArguments, File inputFile)
	{
		String warengruppe;
		if(cliArguments.hasOption(CommandLineArguments.WARENGRUPPE.toString()))
		{
			warengruppe = cliArguments.getOptionValue("wg") + "/";
		}
		else if(cliArguments.hasOption(CommandLineArguments.FOLDER.toString()))
		{
			warengruppe = inputFile.getParentFile().getName();
		}
		else
		{
			warengruppe = "";
		}
		return warengruppe;
	}

	private static Collection<String> getCollectionOfClassNames(FileService fileService, CommandLine cliArguments)
	{
		ArrayList<String> classNames = new ArrayList<String>();
		if(cliArguments.hasOption(CommandLineArguments.INPUT.toString()))
		{
			classNames.add(cliArguments.getOptionValue(CommandLineArguments.INPUT.toString()));
		}
		else if(cliArguments.hasOption(CommandLineArguments.FOLDER.toString()));
		{
			classNames.addAll(fileService.getAllFolders(statsDirectory));
		}
		return classNames;
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
	
	private static ArrayList<File> getCollectionOfSerializedTempFiles(FileService fileService, String baseDirectory)
	{
		ArrayList<File> inputFiles = new ArrayList<File>();
		inputFiles.addAll(fileService.getFilesFromAllSubdirectories(baseDirectory));
		return inputFiles;
	}
	
	private static CommandLine parseCommandLineArguments(String[] args)
	{
		CommandLineParser cliParser = new DefaultParser();
		Options options = CommandLineArguments.getCommandLineOptions();
		CommandLine cliArguments = null;
		try
		{
			cliArguments = cliParser.parse(options, args);
		}
		catch (ParseException exc)
		{
			String header = errorMessageBundle.getString("avve.extractor.helpMessageHeader");
			String footer = errorMessageBundle.getString("avve.extractor.helpMessageFooter");
			 
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Avve", header, options, footer, true);
			logger.error(exc.getLocalizedMessage(), exc);
		}
		return cliArguments;
	}
	
	private static EbookContentData preprocessText(String plainText, EpubFile epubFile, String warengruppe, CommandLine cliArguments)
	{
		// only instantiate preprocessor service once to save on resources
		if(null == textPreprocessor)
		{
			textPreprocessor = new DataPreprocessorService(logger, cliArguments);
		}
		// The pre-processing results will be stored in the EbookContentData object
		EbookContentData ebookContentData = new EbookContentData(epubFile, plainText, warengruppe, logger);
		
		textPreprocessor.preProcessText(ebookContentData);
		return ebookContentData;
	}
	
	private static void serializeTempEbookContentFileToDisk(File inputFile, String warengruppe, EbookContentData ebookContentData)
	{
		OutputStream fileOutputStream = null;
		ObjectOutputStream objectOutputStream = null;
		
		fileService.createDirectory("output/temp/" + warengruppe);
		try
		{
			fileOutputStream = new FileOutputStream(FilenameUtils.concat("output/temp/" + warengruppe + "/", inputFile.getName() + ".ser"));
			objectOutputStream = new ObjectOutputStream( fileOutputStream );
			objectOutputStream.writeObject( ebookContentData );
		}
		catch ( IOException exc )
		{
			logger.error(exc.getLocalizedMessage());
		}
		finally
		{
			fileService.safeClose(objectOutputStream);
			fileService.safeClose(fileOutputStream);
		}
	}
	
	private static void writePreprocessingResultsToFileSystem(String warengruppe, EbookContentData ebookContentData, File inputFile, CommandLine cliArguments)
	{
		fileService.createDirectory("output");
		
		String outputDirForFiles = textDirectory + "/" + warengruppe + "/";
		String outputDirForAttributes = statsDirectory + "/" + warengruppe + "/";
		
		String outputFile = outputDirForFiles + FilenameUtils.getBaseName(inputFile.getAbsolutePath()) + ".txt";
		String outputAttributes = outputDirForAttributes + FilenameUtils.getBaseName(inputFile.getAbsolutePath()) + ".xml";
		
		PrintStream printStream = null;
		
		try
		{
			fileService.createDirectory(outputDirForFiles);
			fileService.createDirectory(outputDirForAttributes);
			
			OutputStream out1 = fileService.createFileOutputStream(outputFile);
			printStream = new PrintStream(out1);
			printStream.print(ebookContentData.getPlainText());
			printStream.close();
			
			int wordVectorSize;
			try
			{
				wordVectorSize = Integer.parseInt(cliArguments.getOptionValue(CommandLineArguments.WORDVECTORSIZE.toString()));
			}
			catch (NumberFormatException exc)
			{
				logger.error(String.format(errorMessageBundle.getString("avve.extractor.wordVectorNumberFormatError"), wordVectorSizeDefaultValue));
				wordVectorSize = wordVectorSizeDefaultValue;
			}
			catch (NullPointerException exc)
			{
				wordVectorSize = wordVectorSizeDefaultValue;
			}
			
			XrffFileWriter xrffFile = new XrffFileWriter(outputAttributes, fileService, luceneService.getLuceneIndexDirectory(), logger, controlledVocabularyService);
			xrffFile.saveEbookContentData(ebookContentData, wordVectorSize);
		}
		catch (FileNotFoundException exc)
		{
			logger.error(String.format(errorMessageBundle.getString("avve.extractor.fileOutputError"), outputFile + " / " + outputAttributes));
			exc.printStackTrace();
		}
		finally
		{
			fileService.safeClose(printStream);
		}
	}
}