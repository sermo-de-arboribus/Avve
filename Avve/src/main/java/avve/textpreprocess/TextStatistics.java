package avve.textpreprocess;

public interface TextStatistics
{
	void appendStatistics(Class<?> preprocessorClass, String statisticsString);
	String getStatistics(Class<?> preprocessorClass);
}