package com.dyalog.apldev.debug.ui.breakpoints;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.ui.actions.IRunToLineTarget;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.ui.texteditor.ITextEditor;

import com.dyalog.apldev.debug.ui.editor.APLEditor;

/**
 * Creates a toggle breakpoint adapter
 */
public class APLEditorAdapterFactory implements IAdapterFactory {

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof APLEditor) {
			ITextEditor editorPart = (ITextEditor) adaptableObject;
			IResource resource = (IResource) editorPart.getEditorInput().getAdapter(IResource.class);
			if (resource != null) {
				String extension = resource.getFileExtension();
				if (extension != null && extension.equals("apl")) {
					if (adapterType.equals(IToggleBreakpointsTarget.class)) {
						return new APLBreakpointAdapter();
					}
					if (adapterType.equals(IRunToLineTarget.class)) {
						return new APLRunToLineAdapter();
					}
				}
			}
		}
		return null;
	}

	public Class[] getAdapterList() {
		return new Class[] {IToggleBreakpointsTarget.class};
	}

}
