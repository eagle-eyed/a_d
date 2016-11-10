package com.dyalog.apldev.debug.core.console.actions;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;

import com.dyalog.apldev.debug.core.console.IHandleScriptAutoEditStrategy;

public class AplAutoIndentStrategy implements IAutoEditStrategy, IHandleScriptAutoEditStrategy {

	private final IAdaptable projectAdaptable;
	
	public AplAutoIndentStrategy(IAdaptable projectAdaptable) {
		this.projectAdaptable = projectAdaptable;
	}
	
	@Override
	public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
		return;
//		if (blockSelection || !command.doit){
//			// in block selection, leave all as is and just change tabs/spaces
//			getIndentPrefs().convertToStd(document, command);
//			return;
//		}
//		char c;
//		if (command.text.length() == 1) {
//			c = command.text.charAt(0);
//		} else {
//			c = '\0';
//		}
//		String contentType = ParsingUtils.getContentType(document, command.offset);
	}

	@Override
	public void customizeParenthesis(IDocument doc, DocumentCommand docCmd) throws BadLocationException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean canSkipCloseParenthesis(IDocument parenDoc, DocumentCommand docCmd) throws BadLocationException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void customizeNewLine(IDocument historyDoc, DocumentCommand docCmd) throws BadLocationException {
		// TODO Auto-generated method stub

	}

	@Override
	public String convertTabs(String cmd) {
		// TODO Auto-generated method stub
		return cmd;
	}

}
