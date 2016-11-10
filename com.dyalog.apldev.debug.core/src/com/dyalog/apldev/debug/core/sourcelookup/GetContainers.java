package com.dyalog.apldev.debug.core.sourcelookup;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.log.Log;

public class GetContainers {

	public IContainer getContainersForLocation(IPath location, IProject project) {
		boolean stopOnFirst = true;
		IContainer[] filesForLocation = getContainersForLocation(location, project, stopOnFirst);
		if (filesForLocation != null && filesForLocation.length > 0) {
			return filesForLocation[0];
		}
		return null;
	}
	
	public IContainer[] getContainersForLocation(IPath location, IProject project, boolean stopOnFirst) {
		ArrayList <IContainer> lst = new ArrayList <IContainer> ();
		HashSet <IProject> checked = new HashSet <IProject> ();
		IWorkspace w = ResourcesPlugin.getWorkspace();
		if (project != null) {
			checked.add(project);
			IContainer f = getContainerInProject(location, project);
			if (f != null) {
				if (stopOnFirst) {
					return new IContainer[] { f };
				} else {
					lst.add(f);
				}
			}
			try {
				IProject[] referencedProjects = project.getDescription().getReferencedProjects();
				for (int i = 0; i < referencedProjects.length; i++) {
					IProject p = referencedProjects[i];
					checked.add(p);
					f = getContainerInProject(location, p);
					if (f != null) {
						if (stopOnFirst) {
							return new IContainer[] { f };
						} else {
							lst.add(f);
						}
					}
				}
			} catch (CoreException e) {
				Log.log(e);
			}
		}
		
		IProject[] projects = w.getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
		for (int i = 0; i < projects.length; i++) {
			IProject p = projects[i];
			if (checked.contains(p)) {
				continue;
			}
			checked.add(p);
			IContainer f = getContainerInProject(location, p);
			if (f != null) {
				if (stopOnFirst) {
					return new IContainer[] { f };
				} else {
					lst.add(f);
				}
			}
		}
		return lst.toArray(new IContainer[lst.size()]);
	}
	
	protected IContainer getContainerInProject(IPath location, IProject project) {
		IContainer file = getContainerInContainer(location, project);
		if (file != null) {
			return file;
		}
		return null;
	}
	
	protected IContainer getContainerInContainer(IPath location, IContainer container) {
		IPath projectLocation = container.getLocation();
		if (projectLocation != null && projectLocation.isPrefixOf(location)) {
			int segmentsToRemove = projectLocation.segmentCount();
			IPath removeFirstSegments = location.removeFirstSegments(segmentsToRemove);
			if (removeFirstSegments.segmentCount() == 0) {
				return container; // equal to container
			}
			IContainer file = container.getFolder(removeFirstSegments);
			if (file.exists()) {
				return file;
			}
		}
		return null;
	}
}
