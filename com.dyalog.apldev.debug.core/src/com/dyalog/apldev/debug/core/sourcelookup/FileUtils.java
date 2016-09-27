package com.dyalog.apldev.debug.core.sourcelookup;

import java.io.File;
import java.io.IOException;

public class FileUtils {

	/**
	 * Get the absolute path in the filesystem for the given file
	 * 
	 * @param absolutePath
	 * @return
	 */
	public static String getFileAbsolutePath(String absolutePath) {
		return getFileAbsolutePath(new File(absolutePath));
	}

	public static String getFileAbsolutePath(File file) {
		try {
			return file.getCanonicalPath();
		} catch (IOException e) {
			return file.getAbsolutePath();
		}
	}

}
