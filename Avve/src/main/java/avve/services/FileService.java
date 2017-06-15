package avve.services;

/**
 * This interface abstracts file access to allow for unit testing with dependency injection.
 * 
 * @author "Kai Weber"
 */
public interface FileService
{
	/**
	 * Tests whether the file or directory denoted by this abstract pathname exists.
	 * 
	 * @param file 
	 * @return true if and only if the file or directory denoted by this abstract pathname exists; false otherwise
	 * @throws SecurityException - If a security manager exists and its java.lang.SecurityManager.checkRead(java.lang.String) method denies read access to the file or directory 
	 */
	boolean fileExists(String filepath);
	
	/**
	 * Tests whether the file denoted by this abstract pathname is a directory.
	 * Where it is required to distinguish an I/O exception from the case that the file is not a directory, or where several attributes of the same file are required at the same time, then the Files.readAttributes method may be used.
	 * 
	 * @param file
	 * @return true if and only if the file denoted by this abstract pathname exists and is a directory; false otherwise
	 * @throws SecurityException - If a security manager exists and its java.lang.SecurityManager.checkRead(java.lang.String) method denies read access to the file
	 */
	boolean isDirectory(String filepath);
}