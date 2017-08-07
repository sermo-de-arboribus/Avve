package avve.extractor;

import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import avve.epubhandling.EbookContentData;
import avve.epubhandling.EpubFile;
import avve.services.*;

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
	private static String indexDirectory = "output/index";
	private static String statsDirectory = "output/stats";
	private static String textDirectory = "output/text";
	
	/**
	 * The main method parses all EPUB files in the input folder (INPUT command line argument) and writes the respective output after
	 * transformations and statistics have been built.
	 * 
	 * @param args Command line arguments
	 */
	// TODO: documentation for command line arguments
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
		
		// process all input files
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
			
			if(null != epubFile && languageCode.equals(language))
			{
				// determine "warengruppe" class code, either from command line parameter or from folder name
				String warengruppe = determineClassName(cliArguments, inputFile);
				
				// Pre-process the text data (e.g. tokenization, sentence detection, part-of-speech tagging
				EbookContentData ebookContentData = preprocessText(plainText, epubFile, warengruppe);
				
				// add the text to a Lucene index (for TF/IDF retrieval)
				addTextToLuceneIndex(ebookContentData.getLemmatizedText(), epubFile.getDocumentId(), epubFile);
				
				// save the processing result to the file system, one file with plain text, one file with statistical attributes
				writePreprocessingResultsToFileSystem(warengruppe, ebookContentData, inputFile);
			}
			else
			{
				logger.error(String.format(errorMessageBundle.getString("InvalidLanguage"), languageCode));
			}
		}
		
		// Go through all statistics files from previous step once again, this time adding TF/IDF score data (which only makes sense after the index for all documents has been built)
		Collection<File> xrffFiles = fileService.getFilesFromAllSubdirectories(statsDirectory);
		
		Directory directory = getLuceneIndexDirectory();
		
		for(File xrffFile : xrffFiles)
		{
			XrffFileModifier xrffModifier = new XrffFileModifier(xrffFile, directory, fileService, logger);
			xrffModifier.addTfIdfStatistics();
			xrffModifier.saveChanges();
		}
		
		
		LocalDateTime endTime = LocalDateTime.now();
		long endTimestamp = System.currentTimeMillis();
		
		logger.info(String.format(infoMessagesBundle.getString("avve.extractor.executionTime"), (endTimestamp - startTimestamp) / 1000));
		logger.info(String.format(infoMessagesBundle.getString("avve.extractor.programFinished"), endTime));
	}

	
	public static Analyzer getLuceneAnalyzer()
	{
		Analyzer analyzer = null;
		switch(language)
		{
			case "de":
				analyzer = new GermanAnalyzer();
				break;
			default:
				analyzer = new StandardAnalyzer();
		}
		
		return analyzer;
	}
	
	private static void addTextToLuceneIndex(String plainText, String documentId, EpubFile epubFile)
	{
		Analyzer analyzer = getLuceneAnalyzer();

		IndexWriter iwriter = null;
		try
		{
		    Directory directory = getLuceneIndexDirectory();
		    IndexWriterConfig config = new IndexWriterConfig(analyzer);
		    iwriter = new IndexWriter(directory, config);
		    Document luceneDocument = new Document();
		    
		    // arrange the document id field
		    FieldType documentIdFieldType = new FieldType();
		    documentIdFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
		    documentIdFieldType.setStored(true);
		    documentIdFieldType.setStoreTermVectors(false);
		    documentIdFieldType.setTokenized(false);
		    Field documentIdField = new Field("docId", documentId, documentIdFieldType);
		    luceneDocument.add(documentIdField);
		    
		    // arrange the text content field
		    FieldType luceneFieldType = new FieldType();
		    luceneFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
		    luceneFieldType.setStored(false);
		    luceneFieldType.setStoreTermVectors(true);
		    luceneFieldType.setTokenized(true);
		    Field luceneField = new Field("plaintext", plainText, luceneFieldType);
		    luceneDocument.add(luceneField);
		    
			iwriter.addDocument(luceneDocument);
		    iwriter.close();
		}
		catch (final IOException exc)
		{
			logger.error(String.format(errorMessageBundle.getString("avve.extractor.luceneIndexWritingError"), epubFile.getDocumentId()), exc);
		}
		finally
		{
			fileService.safeClose(iwriter);
		}
	}

	private static Directory getLuceneIndexDirectory()
	{
		Directory directory = null;
		
		try
		{
			directory = FSDirectory.open(Paths.get(indexDirectory));
		}
		catch (final IOException exc)
		{
			logger.error(String.format(errorMessageBundle.getString("avve.extractor.luceneIndexWritingError"), Paths.get(indexDirectory)), exc);
		}
		
		return directory;
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
	
	private static EbookContentData preprocessText(String plainText, EpubFile epubFile, String warengruppe)
	{
		DataPreprocessorService textPreprocessor = new DataPreprocessorService(logger);
		// The pre-processing results will be stored in the EbookContentData object
		EbookContentData ebookContentData = new EbookContentData(epubFile, plainText, warengruppe, logger);
		
		textPreprocessor.preProcessText(ebookContentData);
		return ebookContentData;
	}
	
	private static void writePreprocessingResultsToFileSystem(String warengruppe, EbookContentData ebookContentData, File inputFile)
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
			
			XrffFileWriter xrffFile = new XrffFileWriter(outputAttributes, fileService);
			xrffFile.saveEbookContentData(ebookContentData);
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