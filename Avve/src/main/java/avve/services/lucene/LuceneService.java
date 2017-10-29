package avve.services.lucene;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.LengthFilterFactory;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

import avve.epubhandling.EpubFile;
import avve.services.FileService;

public class LuceneService
{
	private static final ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	private static String indexDirectory = "output/index";
	
	private Logger logger;
	private FileService fileService;
	
	public LuceneService(Logger logger, FileService fileService)
	{
		this.fileService = fileService;
		this.logger = logger;
	}
	
	public void addTextToLuceneIndex(String plainText, String documentId, EpubFile epubFile, String language)
	{		
		IndexWriter iwriter = null;
		
		try(Analyzer analyzer = getLuceneAnalyzer(language))
		{
		    Directory directory = getLuceneIndexDirectory();
		    IndexWriterConfig config = new IndexWriterConfig(analyzer);
		    
		    int retryCount = 0;
		    while(null == iwriter && retryCount < 5)
		    {
			    try
			    {
				    iwriter = new IndexWriter(directory, config);	
			    }
			    catch(LockObtainFailedException exc)
			    {
			    	retryCount++;
			    	try
			    	{
						Thread.sleep((int)Math.pow(10, retryCount));
					}
			    	catch (InterruptedException iexc)
			    	{
			    		// ignore
					}
			    }
		    }

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
		    
			if(null != iwriter)
			{
				iwriter.updateDocument(new Term(documentId), luceneDocument);
			    iwriter.close();
			}
			else
			{
				logger.error(String.format(errorMessageBundle.getString("avve.extractor.luceneIndexWritingError"), epubFile.getDocumentId()));
			}
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
	
	public Analyzer getLuceneAnalyzer(String language)
	{
		Analyzer analyzer = null;
		switch(language)
		{
			case "de":
				try
				{
					analyzer = CustomAnalyzer.builder()
							.withTokenizer(StandardTokenizerFactory.class)
							.addTokenFilter(StandardFilterFactory.class)
							.addTokenFilter(LowerCaseFilterFactory.class)
							.addTokenFilter(LengthFilterFactory.class, "min", "3", "max", "80")
							.addTokenFilter(GermanStopFilterFactory.class, "ignoreCase", "false")
							.build();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				logger.trace(infoMessagesBundle.getString("avve.services.lucene.customGermanAnalyzerBuild"));
				
				break;
			default:
				analyzer = new StandardAnalyzer();
		}
		
		return analyzer;
	}
	
	public Directory getLuceneIndexDirectory()
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
}