package avve.textpreprocess;

import java.util.Locale;

public class ToLowerCasePreprocessor implements TextPreprocessor
{
	@Override
	public String[] process(String inputText, TextStatistics statistics)
	{
		return new String[] { inputText.toLowerCase(new Locale("de", "DE")) };
	}
}