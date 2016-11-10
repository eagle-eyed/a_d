package com.dyalog.apldev.debug.ui.launcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
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
import org.eclipse.swt.widgets.Widget;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.sourcelookup.GetContainers;
import com.dyalog.apldev.debug.core.sourcelookup.GetFiles;
import com.dyalog.apldev.log.Log;

public class MainModuleBlock extends AbstractLaunchConfigurationTab {

	private Text fMainModuleText;
	private Button fMainModuleBrowseButton;
	private String fProjectName;
	private ModifyListener fProjectModifyListener;
	
	public void createControl(Composite parent) {
		Font font = parent.getFont();
		
		Group group = new Group(parent, SWT.NONE);
		setControl(group);
		GridLayout topLayout = new GridLayout();
		topLayout.numColumns = 2;
		group.setLayout(topLayout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gd);
		group.setFont(font);
		group.setText("Main function");
		
		fMainModuleText = new Text(group, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fMainModuleText.setLayoutData(gd);
		fMainModuleText.setFont(font);
		fMainModuleText.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		
		final Composite lParent = parent;
		fMainModuleBrowseButton = createPushButton(group, "Search...", null);
		fMainModuleBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IResource[] currentResources = getMainModuleResources();
				IResource resource = workspace.getRoot().findMember(fProjectName);
				
				if (resource instanceof IProject) {
					IProject project = (IProject) resource;
					String title, message;
					title = "Main function";
					message = "Choose Apl function which starts execution";
					AplModulePickerDialog dialog = new AplModulePickerDialog(lParent.getShell(), title,
							message, project);
					if (currentResources != null) {
						dialog.setInitialSelections(currentResources);
					}
					
					int result = dialog.open();
					if (result == AplModulePickerDialog.OK){
						Object results[] = dialog.getResult();
						if ((results != null) && (results.length > 0)) {
							ArrayList<IResource> resResults = new ArrayList <IResource> ();
							
							for (int i = 0; i < results.length; i++) {
								if (results[i] instanceof IResource) {
									if (results[i] instanceof IFile) {
										resResults.add((IFile) results[i]);
									} else {
										resResults.add((IResource) results[i]);
									}
								}
							}
//							fMainModuleText.setText(LaunchConfigurationCreator.getDefaultLocation (
//									FileOrResource.createArray(resResults.toArray(new IResource[resResults.size()])),
//											true));
							IFile file = (IFile) resResults.get(0);
							String name = file.getName();
							name = name.substring(0, name.length() - file.getFileExtension().length() - 1);
							fMainModuleText.setText(name);
						}
					}
				}
			}
		});
		// Crate a ModifyListener for listening project modifications in the ProjectBlock
		fProjectModifyListener = new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				Widget widget = e.widget;
				if (widget instanceof Text) {
					Text text = (Text) widget;
					fProjectName = text.getText();
					IWorkspace workspace = ResourcesPlugin.getWorkspace();
					IResource resource = workspace.getRoot().findMember(fProjectName);
					
					boolean enabled = false;
					if ((resource != null) && (resource instanceof IProject)) {
						IProject project = (IProject) resource;
						// TODO check if present apl nature
						enabled = true;
					}
					fMainModuleBrowseButton.setEnabled(enabled);
				}
			}
		};
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		
	}
	public void initializeFrom(ILaunchConfiguration configuration) {
		String location = "";
		try {
			location = configuration.getAttribute(APLDebugCorePlugin.ATTR_MAINMODULE, "");
		} catch (CoreException e) {
			
		}
		fMainModuleText.setText(location);

		String projectName = "";
		try {
			projectName = configuration.getAttribute(APLDebugCorePlugin.ATTR_PROJECT_NAME, "");
		} catch (CoreException e) {
			
		}
		fProjectName = projectName;
		
		try {
			String identifier = configuration.getType().getIdentifier();
			
		} catch (CoreException e) {
			setErrorMessage("Unable to reslove location");
		}
	}
	
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		String value = fMainModuleText.getText().trim();
		setAttribute(configuration, APLDebugCorePlugin.ATTR_MAINMODULE, value);
		configuration.setMappedResources(getMainModuleResources());
	}
	
	public String getName() {
		return "Main module";
	}
	
	/**
	 * Obtains an IFile that targets the current main module.
	 * Used for initializing the module selection dialog.
	 */
	private IResource[] getMainModuleResources() {
		String path = fMainModuleText.getText();
		ArrayList <IResource> resourceList = new ArrayList <IResource>();
		if (path.length() > 0) {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IPath projectPath = new Path(null, fProjectName).makeAbsolute();
			if (projectPath.segmentCount() != 1) {
				return null;
			}
			
			IResource resource = root.getProject(fProjectName);
			IProject project = null;
			if (resource != null) {
				project = resource.getProject();
			}
			
			StringSubstitution stringSubstitution = getStringSubstitution(root);
			if (stringSubstitution != null) {
				try {
					GetFiles getFiles = new GetFiles();
					GetContainers getContainers = new GetContainers();
					for (String loc : splitAndRemoveEmptyTrimmed(path, '|')) {
						String onepath = stringSubstitution.performStringSubstitution(loc, false);
						IFile f = getFiles.getFileForLocation(Path.fromOSString(onepath), project);
						if (f != null) {
							resourceList.add(f);
							continue;
						}
						IContainer container = getContainers.getContainersForLocation(
								Path.fromOSString(onepath), project);
						if (container != null) {
							resourceList.add(container);
						}
					}
				} catch (CoreException e) {
					Log.log(e);
				}
			}
		}
		if (resourceList.isEmpty()) {
			return null;
		}
		return resourceList.toArray(new IResource[resourceList.size()]);
	}
	
	private List <String> splitAndRemoveEmptyTrimmed(String string, char c) {
		List <String> split = split(string, c);
		for (int i = split.size() - 1; i >= 0; i--) {
			if (split.get(i).trim().length() == 0) {
				split.remove(i);
			}
		}
		return split;
	}
	
	private List <String> split(String string, char toSplit) {
		int len = string.length();
		if (len == 0) {
			return new ArrayList <String>(0);
		}
		ArrayList <String> ret = new ArrayList <String> ();
		int last = 0;
		char c = 0;
		for (int i = 0; i < len; i++) {
			c = string.charAt(i);
			if (c == toSplit) {
				if (last != i) {
					ret.add(string.substring(last, i));
				}
				while (c == toSplit && i < len - 1) {
					i++;
					c = string.charAt(i);
				}
				last = i;
			}
		}
		if (c != toSplit) {
			if (last == 0 && len > 0) {
				ret.add(string); // equal to the original
			} else if (last < len) {
				ret.add(string.substring(last, len));
			}
		}
		return ret;
	}
	
	public StringSubstitution getStringSubstitution(IWorkspaceRoot root) {
		IPath projectPath = new Path(null, fProjectName).makeAbsolute();
		if (projectPath.segmentCount() != 1) {
			// Path for project must have one segment
			return null;
		}
		
		IProject resource = root.getProject(fProjectName);
		// TODO get project local folders
		return null;
	}
	
	private void setAttribute(ILaunchConfigurationWorkingCopy configuration, String name, String value) {
		if (value == null || value.length() == 0) {
			configuration.setAttribute(name, (String) null);
		} else {
			configuration.setAttribute(name, value);
		}
	}
	
	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		return super.isValid(launchConfig);
	}
	
	/**
	 * Obtain a listener, used to detect changes of the currently selected project
	 */
	public ModifyListener getProjectModifyListener() {
		return fProjectModifyListener;
	}
}
