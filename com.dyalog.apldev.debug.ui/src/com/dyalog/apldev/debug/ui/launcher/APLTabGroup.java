package com.dyalog.apldev.debug.ui.launcher;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;

public class APLTabGroup extends AbstractLaunchConfigurationTabGroup {

//	public APLTabGroup() {
//	}
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#createTabs(org.eclipse.debug.ui.ILaunchConfigurationDialog, java.lang.String)
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		// tab to the tab group
		MainModuleTab mainModuleTab = new MainModuleTab(); 
		setTabs(new ILaunchConfigurationTab[] {
				mainModuleTab,
//				new APLMainTab(),
				new InterpreterTab(mainModuleTab),
				new EnvironmentTab(),
				new SourceLookupTab(),
				new CommonTab()
		});
	}

}
