package com.dyalog.apldev.interactive_console.actions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.dyalog.apldev.log.Log;

/**
 * Deletes the previous word (Ctrl + Backspace)
 */
public class HandleDeletePreviousWord {

	public void execute(IDocument doc, int caretPosition, int commandLineOffset) {
		int initialCaretPosition = caretPosition;
		// remove all whitespaces
		while (caretPosition > commandLineOffset) {
			try {
				char c = doc.getChar(caretPosition - 1);
				if (!Character.isWhitespace(c)) {
					break;
				}
				caretPosition -= 1;
			} catch (BadLocationException e) {
				break;
			}
		}
		
		// remove a word
		while (caretPosition > commandLineOffset) {
			try {
				char c = doc.getChar(caretPosition - 1);
				if (!Character.isJavaIdentifierPart(c)) {
					break;
				}
				caretPosition -= 1;
			} catch (BadLocationException e) {
				break;
			}
		}
		
		if (initialCaretPosition == caretPosition
				&& initialCaretPosition > commandLineOffset) {
			caretPosition = initialCaretPosition - 1;
		}
		
		try {
			doc.replace(caretPosition,  initialCaretPosition - caretPosition, "");
		} catch (BadLocationException e) {
			Log.log(e);
		}
	}
}
