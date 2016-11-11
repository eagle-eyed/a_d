package com.dyalog.apldev.interactive_console.console;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;

/**
 * Mostly a documentCommand, but with a custom constructor, a proper toSring
 * method and a way to pass an IDocument and have it applied.
 */
public class DocCmd extends DocumentCommand {

	public DocCmd(int offset, int length, String text) {
		this.offset = offset;
		this.length = length;
		this.text = text;
		this.caretOffset = -1;
		this.shiftsCaret = true;
		this.doit = true;
	}
	
	@Override
	public String toString() {
		// TODO implement custom toString()
		return this.toString();
	}
	
	public void doExecute(IDocument document) throws BadLocationException {
		document.replace(offset, length, text);
	}
}
