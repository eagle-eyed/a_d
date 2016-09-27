package com.dyalog.apldev.debug.ui.launcher;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.WorkingDirectoryBlock;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;

public class APLWorkingDirectoryBlock extends WorkingDirectoryBlock {
	
	public APLWorkingDirectoryBlock() {
		super(APLDebugCorePlugin.ATTR_WORKING_DIRECTORY);
	}

	@Override
	protected IProject getProject(ILaunchConfiguration configuration) throws CoreException {
		String projectName = configuration.getAttribute(APLDebugCorePlugin.ATTR_PROJECT_NAME, (String) null);
		if ((projectName == null) || (projectName.trim().length() < 1)) {
			return null;
		}
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		if (root == null)
			return null;
		IProject project = root.getProject(projectName);
//		if (!project.exists() && !project.isOpen())
//			return null;
		return project == null ? null : project.getProject();
	}

}
