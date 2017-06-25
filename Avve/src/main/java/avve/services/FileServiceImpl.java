package avve.services;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

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
	
	public void clearFolder(String dirpath) throws IOException
	{
		FileUtils.deleteDirectory(new File(dirpath));
	}
	
	public boolean createDirectory(String dirpath)
	{
		File directory = new File(dirpath);
		return directory.mkdirs();
	}
	
	public FileInputStream createFileInputStream(String filepath) throws FileNotFoundException
	{
		return new FileInputStream(filepath);
	}
	
	public FileOutputStream createFileOutputStream(String filepath) throws FileNotFoundException
	{
		return new FileOutputStream(filepath);
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