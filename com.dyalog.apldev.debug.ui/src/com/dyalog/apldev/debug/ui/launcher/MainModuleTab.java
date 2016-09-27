package com.dyalog.apldev.debug.ui.launcher;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.dyalog.apldev.debug.ui.APLDebugUIPlugin;

public class MainModuleTab extends AbstractLaunchConfigurationTab {

	ProjectBlock fProjectBlock;
	MainModuleBlock fMainModuleBlock;
	
	public MainModuleTab() {
		fProjectBlock = new ProjectBlock();
		fMainModuleBlock = new MainModuleBlock();
	}
	public void createControl(Composite parent) {
		
		Composite composite = new Composite(parent, SWT.NONE);
		setControl(composite);
		GridLayout gridLayout = new GridLayout();
		composite.setLayout(gridLayout);
		
		fProjectBlock.createControl(composite);
		fMainModuleBlock.createControl(composite);
		
		// add modify listener for main module block
		fProjectBlock.addModifyListener(fMainModuleBlock.getProjectModifyListener());
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {

	}

	public void initializeFrom(ILaunchConfiguration configuration) {
		fProjectBlock.initializeFrom(configuration);
		fMainModuleBlock.initializeFrom(configuration);
	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		fProjectBlock.performApply(configuration);
		fMainModuleBlock.performApply(configuration);
	}

	public String getName() {
		return "Main";
	}

	@Override
	public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
		super.setLaunchConfigurationDialog(dialog);
		
		fProjectBlock.setLaunchConfigurationDialog(dialog);
		fMainModuleBlock.setLaunchConfigurationDialog(dialog);
	}
	
	@Override
	public String getErrorMessage() {
		String result = super.getErrorMessage();
		
		if (result == null) {
			result = fProjectBlock.getErrorMessage();
		}
		
		if (result == null) {
			result = fMainModuleBlock.getErrorMessage();
		}
		
		return result;
	}
	
	@Override
	public String getMessage() {
		String result = super.getMessage();
		
		if (result == null) {
			result = fProjectBlock.getMessage();
		}
		if (result == null) {
			result = fMainModuleBlock.getMessage();
		}
		
		return result;
	}
	
	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		boolean result = super.isValid(launchConfig);
		
		if (result) {
			result = fProjectBlock.isValid(launchConfig);
		}
		if (result) {
			result = fMainModuleBlock.isValid(launchConfig);
		}
		return result;
	}
	
	@Override
	public Image getImage() {
//		return APLDebugUIPlugin.getDefault().getImageRegistry().get(APLDebugUIPlugin.IMG_OBJ_PDA);
		return APLDebugUIPlugin.getImageCache().get(APLDebugUIPlugin.MAIN_TAB_ICON);
	}
}
