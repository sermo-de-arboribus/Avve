package avve.services;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

/**
 * This interface abstracts file access to allow for unit testing with dependency injection.
 * 
 * @author "Kai Weber"
 */
public interface FileService
{
	boolean createDirectory(String dirpath);
	
	/**
	 * Tests whether the file or directory denoted by this abstract pathname exists.
	 * 
	 * @param file 
	 * @return true if and only if the file or directory denoted by this abstract pathname exists; false otherwise
	 * @throws SecurityException - If a security manager exists and its java.lang.SecurityManager.checkRead(java.lang.String) method denies read access to the file or directory 
	 */
	boolean fileExists(String filepath);
	
	void clearFolder(String dirpath) throws IOException;
	
	FileInputStream createFileInputStream(String filepath) throws FileNotFoundException;
	
	FileOutputStream createFileOutputStream(String filepath) throws FileNotFoundException;
	
	/**
	 * Tests whether the file denoted by this abstract pathname is a directory.
	 * Where it is required to distinguish an I/O exception from the case that the file is not a directory, or where several attributes of the same file are required at the same time, then the Files.readAttributes method may be used.
	 * 
	 * @param file
	 * @return true if and only if the file denoted by this abstract pathname exists and is a directory; false otherwise
	 * @throws SecurityException - If a security manager exists and its java.lang.SecurityManager.checkRead(java.lang.String) method denies read access to the file
	 */
	boolean isDirectory(String filepath);
	
	/**
	 * Return the names of all directories within a given parent directory
	 * @param directoryPath The current parent directory
	 * @return A collection with all child directory names (not recursive)
	 */
	Collection<String> getAllFolders(String directoryPath);
	
	Collection<File> listFilesInDirectory(String directoryPath);
	
	/**
	 * Instantiates a new java.io.File object 
	 * @param filepath the filepath associated with the File object
	 * @return the newly instantiated File object
	 */
	File newFileObject(String filepath);
	
	void safeClose(Closeable closeable);

	Collection<File> getFilesFromAllSubdirectories(String basePath);
}