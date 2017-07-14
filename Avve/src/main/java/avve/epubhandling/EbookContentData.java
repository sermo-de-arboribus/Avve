package avve.epubhandling;

import java.util.SortedMap;
import java.util.TreeMap;

public class EbookContentData
{
	private String plainText;
	private String[] sentences;
	private String[] tokens;
	private String[] lemmas;
	private String[] partsOfSpeech;
	private SortedMap<String, Integer> lemmaFrequencies;
	private SortedMap<String, Integer> wordFrequencies;
	private SortedMap<String, Integer> partsOfSpeechFrequencies;
	
	public EbookContentData(String plainText)
	{
		this. plainText = plainText;
		lemmaFrequencies = new TreeMap<String, Integer>();
		wordFrequencies = new TreeMap<String, Integer>();
		partsOfSpeechFrequencies = new TreeMap<String, Integer>();
	}
	
	public String getPlainText()
	{
		return plainText;
	}
	
	public String[] getSentences()
	{
		return sentences;
	}
	
	public String[] getTokens()
	{
		return tokens;
	}
	

	public SortedMap<String, Integer> getLemmaFrequencies()
	{
		return lemmaFrequencies;
	}
	
	public String[] getLemmas()
	{
		return lemmas;
	}
	
	public String[] getPartsOfSpeech()
	{
		return partsOfSpeech;
	}
	
	public SortedMap<String, Integer> getWordFrequencies()
	{
		return wordFrequencies;
	}
	
	public SortedMap<String, Integer> getPartsOfSpeechFrequencies()
	{
		return partsOfSpeechFrequencies;
	}
	
	public void setSentences(String[] sentences)
	{
		this.sentences = sentences;
	}

	public void setTokens(String[] tokens)
	{
		this.tokens = tokens;
	}

	public void setLemmas(String[] lemmas)
	{
		this.lemmas = lemmas;
	}

	public void setPartsOfSpeech(String[] partsOfSpeech)
	{
		this.partsOfSpeech = partsOfSpeech;
	}

	public void setPlainText(String plainText)
	{
		this.plainText = plainText;
	}
}