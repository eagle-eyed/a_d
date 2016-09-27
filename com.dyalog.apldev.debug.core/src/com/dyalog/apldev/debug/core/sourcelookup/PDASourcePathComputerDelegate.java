package com.dyalog.apldev.debug.core.sourcelookup;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.debug.core.sourcelookup.containers.FolderSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.WorkspaceSourceContainer;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;

/**
 * Computes the default source lookup path for a PDA launch configuration.
 * The default source lookup path is the folder or project containing
 * the PDA program being launched. If the program is not specified, the workspace
 * is searched by default.
 */
public class PDASourcePathComputerDelegate implements
		ISourcePathComputerDelegate {

	public ISourceContainer[] computeSourceContainers(
			ILaunchConfiguration configuration, IProgressMonitor monitor)
			throws CoreException {

		String name = configuration.getAttribute(APLDebugCorePlugin.ATTR_PROJECT_NAME,
				(String) null);
		ISourceContainer sourceContainer = null;
		if (name != null) {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
			if (project != null) {
				sourceContainer = new ProjectSourceContainer(project, false);
			}
//				if (container.getType() == IResource.FOLDER){
//					sourceContainer = new FolderSourceContainer(container, false);
//				}
		}
		if (sourceContainer == null) {
			sourceContainer = new WorkspaceSourceContainer();
		}
		return new ISourceContainer[] {sourceContainer};
	}

}
