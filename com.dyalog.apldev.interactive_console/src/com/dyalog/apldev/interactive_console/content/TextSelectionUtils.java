package com.dyalog.apldev.interactive_console.content;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;

public class TextSelectionUtils {

	protected IDocument doc;
	protected ITextSelection textSelection;
	
	/**
	 * @param document the document we are using to make the selection
	 * @param offset the offset where the selection will happen 
	 * (0 characters will be selected)
	 */
	public TextSelectionUtils(IDocument doc, int offset) {
		this(doc, new TextSelection(doc, offset, 0));
	}

	public TextSelectionUtils(IDocument doc, TextSelection selection) {
		this.doc = doc;
		this.textSelection = selection;
	}

	/**
	 * @return the offset of the specified line
	 */
	public final int getLineOffset(int line) {
		try {
			return getDoc().getLineInformation(line).getOffset();
		} catch (Exception e) {
			return 0;
		}
	}
	
	/**
	 * @return the doc
	 */
	public final IDocument getDoc() {
		return doc;
	}
	
	/**
	 * @return the corsorLine
	 */
	public final int getCursorLine() {
		return getTextSelection().getEndLine();
	}
	
	/**
	 * @return the textSelection
	 */
	public final ITextSelection getTextSelection() {
		return textSelection;
	}

	/**
	 * @return the offset mapping to the end of the line passed as parameter
	 * @throws BadLocationException
	 */
	public int getEndLineOffset(int line) throws BadLocationException {
		IRegion lineInformation = doc.getLineInformation(line);
		return lineInformation.getOffset() + lineInformation.getLength();
	}

	public int getAbsoluteCursorOffset() {
		return getTextSelection().getOffset();
	}
	
	public final int getLineOffset() {
		return getLineOffset(getCursorLine());
	}

	public static int getFirstCharPosition(String src) {
		int i = 0;
		boolean breaked = false;
		int len = src.length();
		while (i < len) {
			if (!Character.isWhitespace(src.charAt(i))) {
				i++;
				breaked = true;
				break;
			}
			i++;
		}
		if (!breaked) {
			i++;
		}
		return (i - 1);
	}
}
