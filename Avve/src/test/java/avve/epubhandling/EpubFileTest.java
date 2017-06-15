package avve.epubhandling;

import static org.mockito.Mockito.*;

import java.io.*;
import org.junit.Test;

import avve.services.FileService;
import avve.services.FileServiceImpl;

public class EpubFileTest
{
	@Test(expected=FileNotFoundException.class)
	public void constructor_throws_error_if_file_doesnt_exist() throws IOException
	{
		// Arrange
		FileService mockedFileService = mock(FileServiceImpl.class);
		
		String filepath = "my filepath";
		when(mockedFileService.fileExists(filepath)).thenReturn(false);
		when(mockedFileService.isDirectory(filepath)).thenReturn(false);
		
		// Act
		EpubFile epubFile = new EpubFile(filepath, mockedFileService);
	}
}
