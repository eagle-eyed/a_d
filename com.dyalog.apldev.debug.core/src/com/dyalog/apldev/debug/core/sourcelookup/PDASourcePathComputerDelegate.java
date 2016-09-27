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
		String path = configuration.getAttribute(APLDebugCorePlugin.ATTR_PDA_PROGRAM,
				(String) null);
		ISourceContainer sourceContainer = null;
		if (path != null) {
			IResource resource = ResourcesPlugin.getWorkspace().getRoot()
					.findMember(new Path(path));
			if (resource != null) {
				IContainer container = resource.getParent();
				if (container.getType() == IResource.PROJECT) {
					sourceContainer = new ProjectSourceContainer((IProject) container, false);
				} else if (container.getType() == IResource.FOLDER){
					sourceContainer = new FolderSourceContainer(container, false);
				}
			}
		}
		if (sourceContainer == null) {
			sourceContainer = new WorkspaceSourceContainer();
		}
		return new ISourceContainer[] {sourceContainer};
	}

}
