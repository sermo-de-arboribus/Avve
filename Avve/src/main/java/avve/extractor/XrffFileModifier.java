package avve.extractor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.Map.Entry;

import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.QueryBuilder;

import avve.services.FileService;

import org.apache.lucene.util.BytesRef;

import nu.xom.*;

public class XrffFileModifier
{
	private static final ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());

	
	public XrffFileModifier(File inputfile, Directory luceneIndexDirectory, FileService fileService, Logger logger)
	{
		this.fileService = fileService;
		this.inputfile = inputfile;
		this.logger = logger;
		this.luceneIndexDirectory = luceneIndexDirectory;
		
		try
		{
			Builder parser = new Builder();
			xrffDocument = parser.build(inputfile);
		}
		catch (ParsingException ex) 
		{
			this.logger.error(String.format(errorMessageBundle.getString("avve.extractor.xrffFileMalformedError"), inputfile.getAbsolutePath()));
		}
		catch (IOException ex)
		{
			this.logger.error(String.format(errorMessageBundle.getString("avve.extractor.xrffFileIOError"), inputfile.getAbsolutePath()));
		}
	}
	
	public void addTfIdfStatistics()
	{
		try
		{
			IndexReader luceneIndexReader = DirectoryReader.open(luceneIndexDirectory);
			String documentId = getDocumentIdFromXrffFile();
			
			logger.info(String.format(infoMessagesBundle.getString("avve.extractor.retrievingTfIdfForDocument"), documentId) + " [" + inputfile + "]");
			
		    IndexSearcher isearcher = new IndexSearcher(luceneIndexReader);
		    // Parse a simple query that searches for "text":
		    QueryBuilder queryBuilder = new QueryBuilder(new KeywordAnalyzer());
		    Query query = queryBuilder.createPhraseQuery("docId", documentId);
		    
		    ScoreDoc[] hits = isearcher.search(query, 1).scoreDocs;
		    if(hits.length > 0)
		    {
			    Terms terms = luceneIndexReader.getTermVector(hits[0].doc, "plaintext");
			    
			    long numberOfDocuments = luceneIndexReader.getDocCount("plaintext");
			    TermsEnum termsEnum = terms.iterator();
			    BytesRef bytesRefToTerm = null;
			    SortedMap<String, TfIdfTuple> tdIdfTuples = new TreeMap<String, TfIdfTuple>();
			    
			    // iterate through all terms in the current document's "plaintext" field
			    while ((bytesRefToTerm = termsEnum.next()) != null)
			    {
			    	String term = bytesRefToTerm.utf8ToString();
			    	
			    	// calculate term frequency
			    	if(tdIdfTuples.containsKey(term))
			    	{
			    		// increment term frequency of existing entry
			    		tdIdfTuples.get(term).incrementTermFrequencyByOne();
			    	}
			    	else
			    	{
			    		// initialize term frequency and calculate inverse document frequency (only need to do that once per term)
			    		double idf = 1 + Math.log(numberOfDocuments / luceneIndexReader.docFreq(new Term("plaintext", bytesRefToTerm)) + 1.0);
			    		tdIdfTuples.put(term, new TfIdfTuple(1, idf /*, 1.0 / Math.sqrt(termsEnum.docFreq())*/)); // TODO: add normalization factor	
			    		// TODO: See https://lucene.apache.org/core/6_4_0/core/org/apache/lucene/search/package-summary.html, header "Changing Scoring - Similarity"
			    	}
			    }
			    
			    List<Entry<String, TfIdfTuple>> sortedByIdfDescending = new ArrayList<Entry<String, TfIdfTuple>>(tdIdfTuples.entrySet());
			    
			    Collections.sort(sortedByIdfDescending, new Comparator<Entry<String, TfIdfTuple>>()
			    {
			    	@Override
			    	public int compare(Entry<String, TfIdfTuple> e1, Entry<String, TfIdfTuple> e2)
			    	{
			    		return e2.getValue().compareTo(e1.getValue());
			    	}
			    });
			    
			    Nodes attributesNode = xrffDocument.query("/dataset/header/attributes");
			    Element attributesElement = (Element)attributesNode.get(0);
			    Element attributeElement = new Element("attribute");
			    attributeElement.addAttribute(new Attribute("name", "top-idf"));
			    attributeElement.addAttribute(new Attribute("type", "string"));
			    attributesElement.insertChild(attributeElement, attributesElement.getChildCount() - 2);
			    
			    Nodes instanceNode = xrffDocument.query("/dataset/body/instances/instance");
			    Element instanceElement = (Element)instanceNode.get(0);
			    StringBuilder stringBuilder = new StringBuilder();
			    int numberOfIdfValuesToInclude = sortedByIdfDescending.size() > 50 ? 50 : sortedByIdfDescending.size();
			    for(int i = 0; i < numberOfIdfValuesToInclude; i++ )
			    {
			    	String term = sortedByIdfDescending.get(i).getKey();
			    	stringBuilder.append("[" + i + "] ");
			    	stringBuilder.append(term);
			    	stringBuilder.append(" - ");
			    	stringBuilder.append(sortedByIdfDescending.get(i).getValue().getInverseDocumentFrequency()); // the idf
			    	stringBuilder.append(" - ");
			    	stringBuilder.append(sortedByIdfDescending.get(i).getValue().getTermFrequency());
			    	stringBuilder.append(System.lineSeparator());
			    }
			    Element newValueElement = new Element("value");
			    newValueElement.appendChild(stringBuilder.toString());
			    instanceElement.insertChild(newValueElement, instanceElement.getChildCount() - 2);
		    }
	    }
		catch (IOException exc)
		{
			// TODO: log error
	        exc.printStackTrace();
	    }
	}

	public void saveChanges()
	{
		OutputStream outputStream = null;
	 	try
		{
			outputStream = fileService.createFileOutputStream(inputfile.getAbsolutePath());

			Serializer serializer = new Serializer(outputStream, "UTF-8");
			serializer.setIndent(4);
		 	serializer.setMaxLength(256);
			serializer.write(xrffDocument);
		}
	 	catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 	finally
	 	{
	 		fileService.safeClose(outputStream);
	 	}
	}

	private String getDocumentIdFromXrffFile()
	{
		String documentId = "";
		
		Nodes queryResult = xrffDocument.query("/dataset/body/instances/instance/@documentId");
		if(queryResult.size() > 0)
		{
			documentId = queryResult.get(0).getValue();
		}
		
		return documentId;
	}

	private FileService fileService;
	private File inputfile;
	private Logger logger;
	private Directory luceneIndexDirectory;
	private Document xrffDocument;
}