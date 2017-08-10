package avve.extractor;

import org.apache.commons.lang3.builder.*;

public class TfIdfTuple implements Comparable<TfIdfTuple>
{
	public TfIdfTuple(int termFrequency, double inverseDocumentFrequency)
	{
		this(termFrequency, inverseDocumentFrequency, 1.0);
	}
	
	public TfIdfTuple(int termFrequency, double inverseDocumentFrequency, double normalizingFactor)
	{
		norm = normalizingFactor;
		tf = termFrequency;
		idf = inverseDocumentFrequency;
	}

	@Override
	public int compareTo(TfIdfTuple other)
	{
		return new CompareToBuilder().append(this.getNormalizedTfIdfValue(), other.getNormalizedTfIdfValue()).toComparison();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if(!(other instanceof TfIdfTuple))
		{
			return false;
		}
		if(other == this)
		{
			return true;
		}

		TfIdfTuple otherTuple = (TfIdfTuple)other;
		return new EqualsBuilder().append(this.getNormalizedTfIdfValue(), otherTuple.getNormalizedTfIdfValue()).isEquals();
	}
	
	public double getInverseDocumentFrequency()
	{
		return idf;
	}
	
	public int getTermFrequency()
	{
		return tf;
	}
	
	public double getNormalizedTfIdfValue()
	{
		// formula adapted from the default TF-IDF calculation in Solr, which is: sqrt(tf) * ( 1 + log(numDocs / (docFreq + 1)) * boostFactor * (1 / sqrt(numTerms))
		// but for our purpose we are boosting tf
		return Math.sqrt(tf) * idf * norm;
	}
	
	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(131, 181).append(getNormalizedTfIdfValue()).toHashCode();
	}
	
	public void incrementTermFrequencyByOne()
	{
		tf++;
	}
	
	public void incrementTermFrequency(int increment)
	{
		if(increment < 0)
		{
			// this is not an increment
			// TODO: ignore or throw an error?
		}
		else
		{
			tf += increment;
		}
	}
	
	private int tf;
	private double idf;
	private double norm;
}