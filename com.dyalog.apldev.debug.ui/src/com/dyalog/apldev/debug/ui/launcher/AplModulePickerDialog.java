package com.dyalog.apldev.debug.ui.launcher;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.ui.APLDebugUIPlugin;

public class AplModulePickerDialog extends ElementTreeSelectionDialog {

	public AplModulePickerDialog(Shell parent, String title, String message,
			IProject project) {
		super(parent, new WorkbenchLabelProvider(), new AplModuleContentProvider());
		this.setEmptyListMessage("No Apl modules in project " + project.getName());
		this.setInput(project);
		this.setTitle(title);
		this.setMessage(message);
		
		// Do not allow multiple selection of modules
		this.setValidator(new ISelectionStatusValidator() {
			
			public IStatus validate(Object selection[]) {
				if (selection.length >= 1) {
					if (selection[0] instanceof IFile) {
						IFile file = (IFile) selection[0];
						return new Status(IStatus.OK, APLDebugUIPlugin.getPluginID(), IStatus.OK,
								"Function " + file.getName() + " selected", null);
					} else if (selection[0] instanceof IFolder) {
						IFolder folder = (IFolder) selection[0];
						
//						if (folder.findMember("src") == null) {
//							return new Status(IStatus.ERROR, APLDebugUIPlugin.getPluginID(), IStatus.ERROR,
//									"Can't find 'src' folder", null);
//						} else
							return new Status(IStatus.OK, APLDebugUIPlugin.getPluginID(), IStatus.OK,
									"Function " + folder.getName() + " selected", null);
					}
				}
				return new Status(IStatus.ERROR, APLDebugUIPlugin.getPluginID(), IStatus.ERROR,
						"No Apl function selected", null);
			}
		});
	}
}

class AplModuleContentProvider implements ITreeContentProvider {

	public void dispose() {
		
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public Object[] getChildren(Object element) {
		
		if (element instanceof IContainer) {
			IContainer container = (IContainer) element;
			
			if (container.isAccessible()) {
				try {
					List <IResource> children = new ArrayList <IResource>();
					IResource[] members = container.members();
					for (int i = 0; i < members.length; i++) {
						if (members[i] instanceof IFile) {
							IFile file = (IFile) members[i];
							// TODO check if valid source file
							if (file.getFileExtension().equals(APLDebugCorePlugin.MODULES_EXTENSION)) {
								children.add(file);
							}
						} else if (members[i] instanceof IContainer) {
							children.add(members[i]);
						}
					}
					return children.toArray();
				} catch (CoreException e) {
					
				}
			}
		}
		return new Object[0];
	}

	public Object getParent(Object element) {
		if (element instanceof IResource) {
			return ((IResource) element).getParent();
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}
	
}