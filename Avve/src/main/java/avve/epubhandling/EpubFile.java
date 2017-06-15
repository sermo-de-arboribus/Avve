package avve.epubhandling;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import avve.services.FileService;

public class EpubFile
{
	private static ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	
	public EpubFile(String filePath, FileService fileService) throws IOException
	{
		// ensure that file exists
		if(!fileService.fileExists(filePath) || !fileService.isDirectory(filePath))
		{
		    throw new FileNotFoundException(errorMessageBundle.getString("EpubFileNotFound"));
		}
	}
}