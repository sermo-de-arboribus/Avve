package avve.services.lucene;

import java.util.Collection;
import java.util.Map;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.de.GermanAnalyzer;

/**
 * A wrapper class for Lucene's StopFilterFactory, which allows to use an in-memory stop words objects, rather than reading stopwords from disc
 */
public class GermanStopFilterFactory extends org.apache.lucene.analysis.core.StopFilterFactory
{
	private CharArraySet stopWords;

	public GermanStopFilterFactory(Map<String, String> args)
	{
		super(args);
	}
	
	@Override
	public CharArraySet getStopWords()
	{
		if(null == stopWords)
		{
			Collection<Object> stopWordsCollection = GermanAnalyzer.getDefaultStopSet();
			
			stopWordsCollection.add("dass");
			stopWordsCollection.add("schon");
			stopWordsCollection.add("mehr");
			stopWordsCollection.add("Cover");
			
			stopWords = new CharArraySet(stopWordsCollection, false);
		}
		
		return stopWords;
	}
}