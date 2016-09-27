package com.dyalog.apldev.debug.core.console;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;

public interface IHandleScriptAutoEditStrategy extends IAutoEditStrategy {

	void customizeParenthesis(IDocument doc, DocumentCommand docCmd) throws BadLocationException;
	
	boolean canSkipCloseParenthesis(IDocument parenDoc, DocumentCommand docCmd) throws BadLocationException;
	
	void customizeNewLine(IDocument historyDoc, DocumentCommand docCmd) throws BadLocationException;
	
	String convertTabs(String cmd);
}
