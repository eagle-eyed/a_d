package com.dyalog.apldev.debug.core.sourcelookup;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;

public class GetFiles {
	
	public IFile getFileForLocation(IPath location, IProject project) {
		boolean stopOnFirst = true;
		IFile[] filesForLocation = getFilesForLocation(location, project, stopOnFirst);
		if (filesForLocation != null && filesForLocation.length > 0) {
			return filesForLocation[0];
		}
		return null;
	}
	
	public IFile[] getFilesForLocation(IPath location, IProject project, boolean stopOnFirst) {
		ArrayList <IFile> lst = new ArrayList <IFile> ();
		HashSet <IProject> checked = new HashSet <IProject> ();
		IWorkspace w = ResourcesPlugin.getWorkspace();
		if (project != null) {
			checked.add(project);
			IFile f = getFileInProject(location, project);
			if (f != null) {
				if (stopOnFirst) {
					return new IFile[] { f };
				} else {
					lst.add(f);
				}
			}
			try {
				IProject[] referencedProjects = project.getDescription().getReferencedProjects();
				for (int i = 0; i < referencedProjects.length; i++) {
					IProject p = referencedProjects[i];
					checked.add(p);
					f = getFileInProject(location, p);
					if (f != null) {
						if (stopOnFirst) {
							return new IFile[] { f };
						} else {
							lst.add(f);
						}
					}
				}
			} catch (CoreException e) {
				APLDebugCorePlugin.log(e);
			}
		}
		
		IProject[] projects = w.getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
		for (int i = 0; i < projects.length; i++) {
			IProject p = projects[i];
			if (checked.contains(p)) {
				continue;
			}
			checked.add(p);
			IFile f = getFileInProject(location, p);
			if (f != null) {
				if (stopOnFirst) {
					return new IFile[] { f };
				} else {
					lst.add(f);
				}
			}
		}
		return lst.toArray(new IFile[0]);
	}

	/**
	 * Tries to get a file from a project. Considers source folders
	 * (which could be linked) or resources directly beneath the project.
	 * 
	 * @param location
	 * @param project
	 * @return the file found or null if it was not found
	 */
	protected IFile getFileInProject(IPath location, IProject project) {
		IFile file = getFileInContainer(location, project);
		if (file != null) {
			return file;
		}
		return null;
	}
	
	protected IFile getFileInContainer(IPath location, IContainer container) {
		IPath projectLocation = container.getLocation();
		if (projectLocation != null) {
			if (projectLocation.isPrefixOf(location)) {
				int segmentsToRemove = projectLocation.segmentCount();
				IPath removingFirstSegments = location.removeFirstSegments(segmentsToRemove);
				if (removingFirstSegments.segmentCount() == 0) {
					return null;
				}
				IFile file = container.getFile(removingFirstSegments);
				if (file.exists()) {
					return file;
				}
			}
		} else {
			if (container instanceof IProject) {
				APLDebugCorePlugin.logInfo("Info: Project: " + container + " has no associated location.");
			}
		}
		return null;
	}
}
