package com.dyalog.apldev.debug.ui.launcher;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;

/**
 * Launches a APL file
 */
public class APLLaunchShortcut implements ILaunchShortcut {

	public void launch(ISelection selection, String mode) {
		// must be a structured selection with one file selected
		IFile file = (IFile) ((IStructuredSelection) selection).getFirstElement();
		
		// check for an existing launch config for the pda file
//		String path = file.getFullPath().toString();
		String projectName = file.getProject().getName();
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = launchManager.getLaunchConfigurationType(
				APLDebugCorePlugin.ID_APL_LAUNCH_CONFIGURATION_TYPE);
		try {
			ILaunchConfiguration[] configurations = launchManager
					.getLaunchConfigurations(type);
			// check if configuration already created
			for (int i = 0; i < configurations.length; i++) {
				ILaunchConfiguration configuration = configurations[i];
				String attribute = configuration.getAttribute(APLDebugCorePlugin.ATTR_PROJECT_NAME,
						(String) null);
				if (projectName.equals(attribute)) {
					DebugUITools.launch(configuration, mode);
					return;
				}
			}
		} catch (CoreException e) {
			return;
		}
		
		try {
			// create a new configuration for the pda file
			ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, projectName);
			workingCopy.setAttribute(APLDebugCorePlugin.ATTR_PROJECT_NAME, projectName);
			String name = file.getName();
			name = name.substring(0, name.length() - 1 - file.getFileExtension().length());
			workingCopy.setAttribute(APLDebugCorePlugin.ATTR_MAINMODULE, name);
			workingCopy.setMappedResources(new IResource[]{file});
			ILaunchConfiguration configuration = workingCopy.doSave();
			DebugUITools.launch(configuration,  mode);
		} catch (CoreException e1) {
			
		}
	}

	public void launch(IEditorPart editor, String mode) {
		IEditorInput input = editor.getEditorInput();
		System.out.println("Short cut from editor: " + input.getName());
		// TODO find to which project belong this editor 
	}

}
