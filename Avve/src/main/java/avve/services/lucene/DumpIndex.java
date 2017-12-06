package avve.services.lucene;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;

import avve.services.FileService;
import avve.services.FileServiceImpl;

/**
 * Helper class to dump information from a Lucene index. Default is that all terms in an index are dumped to stdout.
 * 
 * If argument "docs" is passed in, then all documents with their respective fields are dumped instead.
 * 
 */
public class DumpIndex
{
	private static final Logger logger = LogManager.getLogger();
	private static FileService fileService = new FileServiceImpl();
	private static LuceneService luceneService = new LuceneService(logger, fileService);
	
    /**
     * Reads the index from Avve's standard index location
     */
    public static void main(String[] args) throws Exception
    {
    	new DumpIndex().dump(args);	
    }
    
    public DumpIndex()
    {
    }

    public void dump(String[] args) throws XMLStreamException, FactoryConfigurationError, CorruptIndexException, IOException
    {
        XMLStreamWriter out = XMLOutputFactory.newInstance().createXMLStreamWriter(System.out);

        IndexReader luceneIndexReader = DirectoryReader.open(luceneService.getLuceneIndexDirectory());

        out.writeStartDocument();
        out.writeStartElement("documents");
        
        if(args.length > 0 && null != args[0] && args[0].equals("docs"))
        {
            for (int i = 0; i < luceneIndexReader.numDocs(); i++)
                dumpDocument(luceneIndexReader.document(i), out, luceneIndexReader);	
        }
        else
        {
        	dumpTerms(out, luceneIndexReader);
        }
        
        out.writeEndElement();
        out.writeEndDocument();
        
        out.flush();
        out.close();
        
        luceneIndexReader.close();
    }

    private void dumpDocument(final Document document, final XMLStreamWriter out, final IndexReader luceneIndexReader)
            throws XMLStreamException
    {
        out.writeStartElement("document");
        for (IndexableField field : (List<IndexableField>) document.getFields()) {
            out.writeStartElement("field");
            out.writeAttribute("name", field.name());
            out.writeAttribute("value", field.stringValue());
            out.writeEndElement();
        }
        
        out.writeEndElement();
    }
    
    private void dumpTerms(final XMLStreamWriter out, IndexReader luceneIndexReader) throws XMLStreamException
    {
    	out.writeStartElement("terms");
    	
    	LuceneDictionary ld = new LuceneDictionary(luceneIndexReader, "plaintext");
    	try
    	{
        	BytesRefIterator iterator = ld.getEntryIterator();
        	BytesRef byteRef = null;
        	
			while ( ( byteRef = iterator.next() ) != null )
			{
				out.writeStartElement("term");
			    out.writeCharacters(byteRef.utf8ToString());
			    out.writeEndElement();
			}
		}
    	catch (IOException e)
    	{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	finally
    	{
    		out.writeEndElement();
    	}
    }
}