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
import org.apache.lucene.store.*;

import avve.epubhandling.EbookContentData;
import avve.services.FileService;

public class LuceneService
{
	private static final ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	private static String indexDirectory = "output/index";
	
	private Logger logger;
	private FileService fileService;
	
	public LuceneService(final Logger logger, final FileService fileService)
	{
		this.fileService = fileService;
		this.logger = logger;
	}
	
	public void addTextToLuceneIndex(final EbookContentData ebookContent, final String language, final boolean excludeForeignWords)
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
		    Field documentIdField = new Field("docId", ebookContent.getDocumentId(), documentIdFieldType);
		    luceneDocument.add(documentIdField);
		    
		    // arrange the text content field
		    FieldType luceneFieldType = new FieldType();
		    luceneFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
		    luceneFieldType.setStored(false);
		    luceneFieldType.setStoreTermVectors(true);
		    luceneFieldType.setTokenized(true);
		    Field fullTextField = null;
		    if(excludeForeignWords)
		    {
		    	fullTextField = new Field("fulltext", ebookContent.getLemmatizedTextWithoutForeignWords(), luceneFieldType);
		    }
		    else
		    {
		    	fullTextField = new Field("fulltext", ebookContent.getLemmatizedText(), luceneFieldType);
		    }
		    
		    luceneDocument.add(fullTextField);
		    
			if(null != iwriter)
			{
				iwriter.updateDocument(new Term(ebookContent.getDocumentId()), luceneDocument);
			    iwriter.close();
			}
			else
			{
				logger.error(String.format(errorMessageBundle.getString("avve.extractor.luceneIndexWritingError"), ebookContent.getDocumentId()));
			}
		}
		catch (final IOException exc)
		{
			logger.error(String.format(errorMessageBundle.getString("avve.extractor.luceneIndexWritingError"), ebookContent.getDocumentId()), exc);
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