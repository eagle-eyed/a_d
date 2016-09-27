package com.dyalog.apldev.debug.ui.launcher;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;

import com.dyalog.apldev.debug.core.sourcelookup.FileUtils;

public class LaunchConfigurationCreator {

	public static String getDefaultLocation(FileOrResource[] file, boolean makeRelative) {
		StringBuffer buffer = new StringBuffer();
		
		for (FileOrResource r : file) {
			if (buffer.length() > 0) {
				buffer.append('|');
			}
			String loc;
			if (r.resource != null) {
				if (makeRelative) {
					IStringVariableManager varManager = VariablesPlugin.getDefault()
							.getStringVariableManager();
					loc = makeFileRelativeToWorkspace(r.resource, varManager);
				} else {
					loc = r.resource.getLocation().toOSString();
				}
			} else {
				loc = FileUtils.getFileAbsolutePath(r.file.getAbsolutePath());
			}
			buffer.append(loc);
		}
		return buffer.toString();
	}
	
	private static String makeFileRelativeToWorkspace(IResource r, IStringVariableManager varManager) {
		String m = r.getFullPath().makeRelative().toString();
		m = varManager.generateVariableExpression("workspace_loc", m);
		return m;
	}
	
}
