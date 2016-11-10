package com.dyalog.apldev.debug.core.console.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.ITextEditorExtension2;

public class BaseAction extends Action implements IEditorActionDelegate {

	public BaseAction() {
		super();
	}
	
	public BaseAction(String text, int style) {
		super(text, style);
	}
	
	public static boolean canModifyEditor(ITextEditor editor) {
		
		if (editor instanceof ITextEditorExtension2) {
			return ((ITextEditorExtension2) editor).isEditorInputModifiable();
		} else if (editor instanceof ITextEditorExtension) {
			return !((ITextEditorExtension) editor).isEditorInputReadOnly();
		} else if (editor != null) {
			return editor.isEditable();
		}
		// If we don't have the editor, let's just say it's ok (working on document)
		return true;
	}
	
	// Always points to the current editor
	protected volatile IEditorPart targetEditor;
	
	public void setEditor(IEditorPart targetEditor) {
		this.targetEditor = targetEditor;
	}
	
	@Override
	public void run(IAction action) {
		
	}

	/**
	 * Activate action (if we are getting text)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		action.setEnabled(true);
	}

	/**
	 * This is an IEditorActionDelegate override
	 */
	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		setEditor(targetEditor);
	}

	/**
	 * This function returns the text editor
	 */
	protected ITextEditor getTextEditor() {
		if (targetEditor instanceof ITextEditor) {
			return (ITextEditor) targetEditor;
		} else {
			throw new RuntimeException("Expection text editor. Found:"
					+ targetEditor.getClass().getName());
		}
	}
	
	/**
	 * Helper for setting caret
	 * @param pos
	 * @throws BadLocationException
	 */
	protected void setCaretPosition(int pos) throws BadLocationException {
		getTextEditor().selectAndReveal(pos, 0);
	}
}
