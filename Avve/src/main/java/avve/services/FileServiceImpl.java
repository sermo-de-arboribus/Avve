package avve.services;

import java.io.File;

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
}