package com.dyalog.apldev.debug.ui.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.dyalog.apldev.debug.ui.breakpoints.APLBreakpointAdapter;

/**
 * Action to toggle a breakpoint
 */
public class ToggleBreakpointAction extends Action {

	private ITextEditor fEditor;
	private IVerticalRulerInfo fRulerInfo;
	
	/**
	 * Constructs a new action to toggle a PDA breakpoint
	 * 
	 * @param editor the editor in which to toggle the breakpoint
	 * @param rulerInfo specifies breakpoint location
	 */
	public ToggleBreakpointAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
		super("Toggle Line Breakpoint");
		fEditor = editor;
		fRulerInfo = rulerInfo;
	}
	
	public void run() {
		
		APLBreakpointAdapter adapter = new APLBreakpointAdapter();
		int line = fRulerInfo.getLineOfLastMouseButtonActivity();
		IDocumentProvider  provider = fEditor.getDocumentProvider();
		ITextSelection selection = null;
		try {
			provider.connect(this);
			IDocument document = provider.getDocument(fEditor.getEditorInput());
			IRegion region = document.getLineInformation(line);
			selection = new TextSelection(document, region.getOffset(), region.getLength());
		} catch (CoreException e1) {
			
		} catch (BadLocationException e) {
			
		} finally {
			provider.disconnect(this);
		}
		if (selection != null) {
			try {
				if (adapter.canToggleWatchpoints(fEditor, selection)) {
					adapter.toggleWatchpoints(fEditor, selection);
				} else if (adapter.canToggleLineBreakpoints(fEditor, selection)) {
					adapter.toggleLineBreakpoints(fEditor, selection);
				}
			} catch (CoreException e) {
				
			}
		}
	}
}
