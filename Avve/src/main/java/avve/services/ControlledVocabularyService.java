package avve.services;

import java.io.*;
import java.util.*;

import org.apache.logging.log4j.Logger;

/**
 * This service handles controlled vocabulary files. It provides an iterator through a list of controlled vocabulary terms
 * 
 * @author Kai Weber
 *
 */
public class ControlledVocabularyService
{
	private static final ResourceBundle errorMessagesBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	
	private Set<String> controlledVocabularyTerms;
	
	public ControlledVocabularyService(String pathToControlledVocabularyFile, FileService fileService, Logger logger) throws IOException
	{
		// ensure that file exists
		if(!fileService.fileExists(pathToControlledVocabularyFile) || fileService.isDirectory(pathToControlledVocabularyFile))
		{
		    throw new FileNotFoundException(String.format(errorMessagesBundle.getString("avve.services.ControlledVocabularyFileNotFound"), pathToControlledVocabularyFile));
		}
		
		File file = fileService.newFileObject(pathToControlledVocabularyFile);
		FileReader fileReader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		controlledVocabularyTerms = new HashSet<String>();
        String line = null;
        while ((line = bufferedReader.readLine()) != null)
        {
        	controlledVocabularyTerms.add(line.toLowerCase());
        }
        bufferedReader.close();
	}
	
	public Iterator<String> getControlledVocabularyIterator()
	{
		return controlledVocabularyTerms.iterator();
	}
}