package com.dyalog.apldev.debug.ui.breakpoints;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.breakpoints.APLLineBreakpoint;
import com.dyalog.apldev.debug.core.model.remote.EntityWindow;
import com.dyalog.apldev.debug.core.model.remote.WorkspaceEditorInput;


/**
 * Adapter to create breakpoints in APL files.
 */
public class APLBreakpointAdapter implements IToggleBreakpointsTarget {

	public void toggleLineBreakpoints(IWorkbenchPart part, ISelection selection)
			throws CoreException {
		ITextEditor textEditor = getEditor(part);
		if (textEditor != null) {
			ITextSelection textSelection = (ITextSelection) selection;
			int lineNumber = textSelection.getStartLine();
			IEditorInput input = textEditor.getEditorInput();

			if (input instanceof WorkspaceEditorInput) {
				// function is interpreter WS resource
				// TODO Toggle breakpoint for interpreter WS function
				WorkspaceEditorInput inputWS = (WorkspaceEditorInput) input;
				EntityWindow entity = inputWS.getEntityWindow();
				if (entity.isStop(lineNumber)) {

				} else {
					// create line breakpoint (doc line numbers start at 0)
//					APLLineBreakpoint lineBreakpoint = new APLLineBreakpoint(resource, lineNumber + 1);
//					DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(lineBreakpoint);
				}
				
				
				System.out.println("Togle breakpoint for WS function");
			} else {
				IResource resource = (IResource) textEditor.getEditorInput().getAdapter(IResource.class);
				IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager()
						.getBreakpoints(APLDebugCorePlugin.ID_APL_DEBUG_MODEL);
				for (int i = 0; i < breakpoints.length; i++) {
					IBreakpoint breakpoint = breakpoints[i];
					if (breakpoint instanceof ILineBreakpoint
							&& resource.equals(breakpoint.getMarker().getResource())) {
						if (((ILineBreakpoint) breakpoint).getLineNumber() == (lineNumber + 1)) {
							// remove
							breakpoint.delete();
							return;
						}
					}
				}
				// create line breakpoint (doc line numbers start at 0)
				APLLineBreakpoint lineBreakpoint = new APLLineBreakpoint(resource, lineNumber + 1);
				DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(lineBreakpoint);
			}
		}
	}

	public boolean canToggleLineBreakpoints(IWorkbenchPart part,
			ISelection selection) {
		return getEditor(part) != null;
	}

	/**
	 * Returns the editor being used to edit a APL file, associated with the
	 * given part, of <code>null</code> if none.
	 * 
	 * @param part workbench part
	 * @return the editor being used to edit a APL file, associated with the
	 * given part, or <code>null</code> if none
	 */
	private ITextEditor getEditor(IWorkbenchPart part) {
		if (part instanceof ITextEditor) {
			ITextEditor editorPart = (ITextEditor) part;
			IEditorInput input = editorPart.getEditorInput();
			IResource resource = (IResource) input.getAdapter(IResource.class);
			
			if (resource != null) {
				String extension = resource.getFileExtension();
				if (extension != null && extension.equals("apl")) {
					return editorPart;
				}
			} else if (input instanceof WorkspaceEditorInput) {
					// Interpreter WS resource
					return editorPart;
			}
		}
		return null;
	}
	
	public void toggleMethodBreakpoints(IWorkbenchPart part,
			ISelection selection) throws CoreException {

	}

	public boolean canToggleMethodBreakpoints(IWorkbenchPart part,
			ISelection selection) {
		return false;
	}

	public void toggleWatchpoints(IWorkbenchPart part, ISelection selection)
			throws CoreException {

	}

	public boolean canToggleWatchpoints(IWorkbenchPart part,
			ISelection selection) {
		return false;
	}
	
}
