package avve.services;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class FileServiceImpl implements FileService
{
	public boolean fileExists(String filepath)
	{
		return new File(filepath).exists();
	}

	public boolean isDirectory(String filepath)
	{
		return new File(filepath).isDirectory();
	}
	
	public void safeClose(Closeable closeable)
	{
		try
		{
			closeable.close();
		}
		catch(IOException exc)
		{
			;
		}
	}
}