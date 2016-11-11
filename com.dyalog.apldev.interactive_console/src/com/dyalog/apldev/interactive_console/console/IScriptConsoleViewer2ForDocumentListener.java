package com.dyalog.apldev.interactive_console.console;

import org.eclipse.jface.text.IDocument;

/**
 * Interface created just so that we can test the ScriptConsoleDocument listener
 * (with the interfaces it relies from the IScriptConsoleViewer2)
 */
public interface IScriptConsoleViewer2ForDocumentListener {

	int getCommandLineOffset();
	
	int getConsoleWidthInCharacters();
	
	int getCaretOffset();
	
	void setCaretOffset(int length,boolean async);
	
	IConsoleStyleProvider getStyleProvider();
	
	IDocument getDocument();
	
	void revealEndOfDocument();
	
	IScriptConsoleSession getConsoleSession();
}
