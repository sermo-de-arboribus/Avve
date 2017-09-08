package avve.services;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

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
		String directory = FilenameUtils.getFullPath(filepath);
		if(!fileExists(directory))
		{
			createDirectory(directory);
		}
		return new FileOutputStream(filepath);
	}
	
	@Override
	public Collection<String> getAllFolders(String directoryPath)
	{
		ArrayList<String> result = new ArrayList<String>();
		
		File parentDirectory = new File(directoryPath);
		File[] directories = parentDirectory.listFiles(new DirectoriesFilter());
		
		for(File dir : directories)
		{
			result.add(dir.getName());
		}
		
		return result;
	}
	
	@Override
	public Collection<File> listFilesInDirectory(final String directoryPath)
	{
		Collection<File> result = FileUtils.listFiles(new File(directoryPath), null, false);
		
		return result;
	}
	
	@Override
	public File newFileObject(String filepath)
	{
		return new File(filepath);
	}
	
	@Override
	public void safeClose(final Closeable closeable)
	{
		if(closeable != null)
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

	@Override
	public Collection<File> getFilesFromAllSubdirectories(String basePath)
	{
		Collection<File> result = FileUtils.listFiles(new File(basePath), null, true);
		return result;
	}
	
	private static class DirectoriesFilter implements FilenameFilter
	{
	    @Override
	    public boolean accept(File entry, String name)
	    {
	        return new File(entry, name).isDirectory();
	    }
	}
}