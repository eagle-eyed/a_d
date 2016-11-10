package com.dyalog.apldev.debug.core.console;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;

/**
 * Provides info related to what may be edited or not
 */
public interface IScriptConsoleViewer extends ITextViewer {
	/**
	 * @return the contents of the current buffer (text edited still not passed to the shell)
	 */
	public String getCommandLine();
	
	/**
	 * @return the offset where current buffer starts (editable area of the document)
	 */
	public int getCommandLineOffset();
	
	/**
	 * @return the current caret offset
	 */
	public int getCaretOffset();
	
	/**
	 * Sets the new caret offset
	 */
	public void setCaretOffset(int offset, boolean async);
	
	/**
	 * @return the document being viewed by this console viewer
	 */
	@Override
	public IDocument getDocument();
	
	/**
	 * @return the interpreterInfo();
	 */
	public Object getInterpreterInfo();
}
