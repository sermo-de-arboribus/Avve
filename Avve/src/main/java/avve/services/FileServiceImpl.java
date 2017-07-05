package avve.services;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class FileServiceImpl implements FileService
{
	@Override
	public boolean fileExists(final String filepath)
	{
		return new File(filepath).exists();
	}

	@Override
	public boolean isDirectory(final String filepath)
	{
		return new File(filepath).isDirectory();
	}
	
	@Override
	public void clearFolder(final String dirpath) throws IOException
	{
		FileUtils.deleteDirectory(new File(dirpath));
	}
	
	@Override
	public boolean createDirectory(final String dirpath)
	{
		File directory = new File(dirpath);
		return directory.mkdirs();
	}
	
	@Override
	public FileInputStream createFileInputStream(final String filepath) throws FileNotFoundException
	{
		return new FileInputStream(filepath);
	}
	
	@Override
	public FileOutputStream createFileOutputStream(final String filepath) throws FileNotFoundException
	{
		return new FileOutputStream(filepath);
	}
	
	@Override
	public Collection<File> listFilesInDirectory(final String directoryPath)
	{
		Collection<File> result = FileUtils.listFiles(new File(directoryPath), null, false);
		
		return result;
	}
	
	@Override
	public void safeClose(final Closeable closeable)
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