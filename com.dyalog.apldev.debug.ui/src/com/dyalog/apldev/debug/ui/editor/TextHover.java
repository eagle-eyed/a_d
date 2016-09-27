package com.dyalog.apldev.debug.ui.editor;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.dyalog.apldev.debug.core.model.APLDebugTarget;
import com.dyalog.apldev.debug.core.model.APLStackFrame;
import com.dyalog.apldev.debug.core.model.APLThread;

/**
 * Produces debug hover for the APL debugger.
 */
public class TextHover implements ITextHover {

	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		// define debug target
		APLDebugTarget debugTarget = null;
		IAdaptable debugContext = DebugUITools.getDebugContext();

		if (debugContext instanceof APLStackFrame) {
			debugTarget = (APLDebugTarget) ((APLStackFrame) debugContext).getDebugTarget();
		} else if (debugContext instanceof APLThread) {
			debugTarget = (APLDebugTarget) ((APLThread) debugContext).getDebugTarget();
		} else if (debugContext instanceof APLDebugTarget) {
			debugTarget = (APLDebugTarget) debugContext;
		}

		if (debugTarget != null) {
			try {
				String line = textViewer.getDocument()
						.get(hoverRegion.getOffset(),hoverRegion.getLength());
				String functionName = "ex";
//				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				if (window == null) {
					IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
					if (PlatformUI.getWorkbench().getWorkbenchWindowCount() > 0)
						window = windows[0];
					else
						return null;
				}

				IEditorPart editorPart = window.getActivePage().getActiveEditor();
				if (editorPart instanceof APLEditor) {
					functionName = editorPart.getEditorInput().getName();
					int extPos = functionName.lastIndexOf(".apl");
					if (extPos != -1)
						functionName = functionName.substring(0, extPos);
				}
				String value = debugTarget.getValueTip(functionName, line, 0, 64, 32);
				return value;
			} catch (BadLocationException e) {
				return null;
			} catch (NullPointerException e2) {
				
			}
		}
		
		return null;
	}

	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		return WordFinder.findWord(textViewer.getDocument(), offset);
	}

}
