package avve.textpreprocess;

public interface TextPreprocessor
{
	String[] process(String inputText, TextStatistics statistics);
}