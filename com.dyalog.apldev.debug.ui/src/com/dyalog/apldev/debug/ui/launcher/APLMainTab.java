package com.dyalog.apldev.debug.ui.launcher;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.ui.APLDebugUIPlugin;

/**
 * Tab to specify the PDA program to run/debug.
 */
public class APLMainTab extends AbstractLaunchConfigurationTab {
	
	// Widgets
	private Text fProgramText;
	private Button fProgramButton;
	
	public APLMainTab() {
	}

	public void createControl(Composite parent) {
		Font font = parent.getFont();
		
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		GridLayout topLayout = new GridLayout();
		topLayout.verticalSpacing = 0;
		topLayout.numColumns = 3;
		comp.setLayout(topLayout);
		comp.setFont(font);
		
		createVerticalSpacer(comp, 3);
		
		Label programLabel = new Label(comp, SWT.NONE);
		programLabel.setText("&Program:"); //$NON-NLS-1$
		GridData gd = new GridData(GridData.BEGINNING);
		programLabel.setLayoutData(gd);
		programLabel.setFont(font);
		
		fProgramText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fProgramText.setLayoutData(gd);
		fProgramText.setFont(font);
		fProgramText.addModifyListener(new ModifyListener() {
//			@Override
			public void modifyText(ModifyEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
		
		fProgramButton = createPushButton(comp, "&Browse...", null); //$NON-NLS-1$
		fProgramButton.addSelectionListener(new SelectionAdapter() {
//			@Override
			public void widgetSelected(SelectionEvent e) {
				browsePDAFiles();
			}
		});
	}

	/**
	 * Open a resource chooser to select a PDA program
	 */
	protected void browsePDAFiles() {
		ResourceListSelectionDialog dialog = new ResourceListSelectionDialog(getShell(),
				ResourcesPlugin.getWorkspace().getRoot(), IResource.FILE);
		dialog.setTitle("PDA Program");
		dialog.setMessage("Select PDA Program");
		if (dialog.open() == Window.OK) {
			Object[] files = dialog.getResult();
			IFile file = (IFile) files[0];
			fProgramText.setText(file.getFullPath().toString());
		}
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {

	}

	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			String program = null;
			// Retrieve the program path attribute from the launch configuration
			program = configuration.getAttribute(APLDebugCorePlugin.ATTR_PDA_PROGRAM,
					(String) null);
			if (program != null) {
				fProgramText.setText(program);
			}
		} catch (CoreException e) {
			setErrorMessage(e.getMessage());
		}
	}

//	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		//
		String program = fProgramText.getText().trim();
		if (program.length() == 0) {
			program = null;
		}
		// Update the launch configuration with path to currently specified program
		configuration.setAttribute(APLDebugCorePlugin.ATTR_PDA_PROGRAM, program);
		
		// perform resource mapping for contextual launch
		IResource[] resources = null;
		if (program != null) {
			IPath path = new Path(program);
			IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
			if (res != null) {
				resources = new IResource[] {res};
			}
		}
		configuration.setMappedResources(resources);
	}

	public String getName() {
		return "Main_"; //$NON-NLS-1$
	}

	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		setMessage(null); // since 3.9: setWarningMessage(null);
		String text = fProgramText.getText();
		// Validate the currently specified program exists 
		// and is not empty, providing the user with feedback.
		if (text.length() > 0) {
			IPath path = new Path(text);
			IResource member = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
			if (member == null) {
				setErrorMessage("Specified program does not exist");
				return false;
			} else {
				if (member.getType() != IResource.FILE) {
					setMessage("Specified program is not a file.");
				}
			}
		} else {
			setMessage("Specify a program");
		}
		return true;
	}
	
	public Image getImage() {
		return APLDebugUIPlugin.getDefault().getImageRegistry().get(APLDebugUIPlugin.IMG_OBJ_APL);
	}
}
