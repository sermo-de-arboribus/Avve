package avve.services.lucene;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.util.ResourceLoader;

/**
 * A wrapper class for Lucene's StopFilterFactory, which allows to use an in-memory stop words objects, rather than reading stopwords from disc
 */
public class GermanStopFilterFactory extends org.apache.lucene.analysis.core.StopFilterFactory
{
	private CharArraySet germanStopWords;

	public GermanStopFilterFactory(Map<String, String> args)
	{
		super(args);
	}
	
	@Override
	public TokenStream create(TokenStream input)
	{
	    StopFilter stopFilter = new StopFilter(input, germanStopWords);
	    return stopFilter;
	}
	  
	@Override
	public CharArraySet getStopWords()
	{	
		return germanStopWords;
	}
	
	@Override
	public void inform(ResourceLoader loader) throws IOException
	{
		Collection<Object> stopWordsCollection = GermanAnalyzer.getDefaultStopSet();
			
		stopWordsCollection.add("dass");
		stopWordsCollection.add("schon");
		stopWordsCollection.add("mehr");
		stopWordsCollection.add("cover");
			
		germanStopWords = new CharArraySet(stopWordsCollection, false);
	}
}