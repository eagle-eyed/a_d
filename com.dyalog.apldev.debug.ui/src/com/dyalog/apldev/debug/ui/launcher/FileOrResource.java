package com.dyalog.apldev.debug.ui.launcher;

import java.io.File;

import org.eclipse.core.resources.IResource;

public class FileOrResource {

	public final IResource resource;
	public final File file;

	public FileOrResource(IResource resource) {
		this.resource = resource;
		this.file = null;
	}
	
	public FileOrResource(File file) {
		this.resource = null;
		this.file = file;
	}
	
	public static FileOrResource[] createArray(IResource[] array) {
		FileOrResource[] ret = new FileOrResource[array.length];
		for (int i = 0; i < array.length; i++) {
			ret[i] = new FileOrResource(array[i]);
		}
		return ret;
	}
	
	public static IResource[] createIResourceArray(FileOrResource[] array) {
		IResource[] ret = new IResource[array.length];
		for (int i = 0; i < array.length; i++) {
			ret[i] = array[i].resource;
		}
		return ret;
	}
}
