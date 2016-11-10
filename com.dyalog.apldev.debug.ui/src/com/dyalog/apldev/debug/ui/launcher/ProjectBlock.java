package com.dyalog.apldev.debug.ui.launcher;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.ui.APLDebugUIPlugin;
import com.dyalog.apldev.log.Log;
/**
 * Control for selection a Apl project
 */
public class ProjectBlock extends AbstractLaunchConfigurationTab {

	private Text fProjectText;
	private Button fProjectBrowseButton;
	List <ModifyListener> waitingForProjectTextToExist = new ArrayList <ModifyListener> ();

	public void createControl(Composite parent) {
		Font font = parent.getFont();
		Group group = new Group(parent, SWT.NONE);
		group.setText("Project");
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gd);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setFont(font);
		
		// Project chooser
		fProjectText = new Text(group, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fProjectText.setLayoutData(gd);
		fProjectText.setFont(font);
		fProjectText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		
		fProjectBrowseButton = createPushButton(group, "Browse...", null);
		fProjectBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				// Filter out project by apl nature
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IProject[] projects = workspace.getRoot().getProjects();
				ArrayList <IProject> aplProjects = new ArrayList <IProject>();
				for (IProject project : projects) {
					try {
//						if (project.isOpen() && project.hasNature(APLPluginUI.APL_NATURE_ID)) {
						if (project.isOpen()) {
							if (project.hasNature("apl.nature")) {
							}
							aplProjects.add(project);
						}
					} catch (CoreException ex) {
						Log.log(ex);
					}
				}
				projects = aplProjects.toArray(new IProject[aplProjects.size()]);
				
//				ILabelProvider labelProvider = new AplLabelProvider();
				ILabelProvider labelProvider = new LabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof IProject) {
							return ((IProject) element).getName();
						}
						return super.getText(element);
					}
				};
				ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), labelProvider);
				dialog.setTitle("Project selection");
				dialog.setMessage("Chose a project for the run");
				dialog.setElements(projects);
				
				dialog.open();
				
				Object object = dialog.getFirstResult();
				if ((object != null) && (object instanceof IProject)) {
					IProject project = (IProject) object;
					// TODO Check if project has apl nature
//					String title = "Invalid project (no apl nature associated)";
//					String message = "The selected project must have the apl nature associated.";
//					ErrorDialog.openError(getShell(), title, message, status);
					
					String projectName = project.getName();
					fProjectText.setText(projectName);
				}
				updateLaunchConfigurationDialog();
			}
		});
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {

	}

	public void initializeFrom(ILaunchConfiguration configuration) {
		String projectName = "";
		try {
			projectName = configuration.getAttribute(APLDebugCorePlugin.ATTR_PROJECT_NAME, "");
		} catch (CoreException e) {
			
		}
		fProjectText.setText(projectName);
	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		String value = fProjectText.getText().trim();
		setAttribute(configuration, APLDebugCorePlugin.ATTR_PROJECT_NAME, value);
	}

	public String getName() {
		return "Project";
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		boolean result = super.isValid(launchConfig);
		
		if (result) {
			setErrorMessage(null);
			setMessage(null);
			
			String projectName = fProjectText.getText();
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IResource resource = workspace.getRoot().findMember(projectName);
			
			if (resource == null) {
				setErrorMessage("Invalid project");
				result = false;
			} else if (resource instanceof IProject) {
				IProject project = (IProject) resource;
				// TODO check if correct nature
//				if (nature == null)
//					result = false;
			}
		}
		return result;
	}

	/**
	 * Sets attributes in the working copy
	 */
	private void setAttribute(ILaunchConfigurationWorkingCopy configuration, String name, String value) {
		if (value == null || value.length() == 0) {
			configuration.setAttribute(name, (String) null);
		} else {
			configuration.setAttribute(name, value);
		}
	}
	
	/**
	 * Adds a modification listener to the current control
	 */
	public void addModifyListener(ModifyListener listener) {
		if (fProjectText == null) {
			waitingForProjectTextToExist.add(listener);
		} else {
			fProjectText.addModifyListener(listener);
			for (ModifyListener l : waitingForProjectTextToExist) {
				fProjectText.addModifyListener(l);
			}
			waitingForProjectTextToExist.clear();
		}
	}
	
}
