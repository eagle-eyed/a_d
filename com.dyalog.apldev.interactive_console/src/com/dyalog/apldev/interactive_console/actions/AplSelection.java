package com.dyalog.apldev.interactive_console.actions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextUtilities;

import com.dyalog.apldev.interactive_console.content.Tuple;

public class AplSelection {

	protected IDocument doc;
	protected ITextSelection textSelection;

	public AplSelection(IDocument doc, ITextSelection selection) {
		this.doc = doc;
		this.textSelection = selection;
	}

	public ITextSelection getTextSelection() {
		return textSelection;
	}

	public final IDocument getDoc() {
		return doc;
	}

	/**
	 * 
	 * @return the offset of the line where the cursor is
	 */
	public final int getLineOffset() {
		return getLineOffset(getCursorLine());
	}
	
	public final int getCursorLine() {
		return getTextSelection().getEndLine();
	}
	
	/**
	 * 
	 * @param line
	 * @return the offset of the specified line
	 */
	public final int getLineOffset(int line) {
		try {
			return getDoc().getLineInformation(line).getOffset();
		} catch (Exception e) {
			return 0;
		}
	}

	public String getLineContentsToCursor() throws BadLocationException {
		int offset = getAbsoluteCursorOffset();
		return getLineContentsToCursor(offset);
	}
	
	public String getLineContentsToCursor(int offset) throws BadLocationException {
		return getLineContentsToCursor(doc, offset);
	}
	
	public static String getLineContentsToCursor(IDocument doc, int offset)
			throws BadLocationException {
		int lineOfOffset = doc.getLineOfOffset(offset);
		IRegion lineInformation = doc.getLineInformation(lineOfOffset);
		String lineToCursor = doc.get(lineInformation.getOffset(),
				offset - lineInformation.getOffset());
		return lineToCursor;
	}
	
	public final int getAbsoluteCursorOffset() {
		return getTextSelection().getOffset();
	}

	public static String getDelimiter(IDocument doc) {
		return TextUtilities.getDefaultLineDelimiter(doc);
	}

	public Tuple<String, String> getBeforeAndAfterMatchingChars(char c) {
		final int initial = getAbsoluteCursorOffset();
		int curr = initial - 1;
		IDocument doc = getDoc();
		StringBuffer buf = new StringBuffer(10);
		int length = doc.getLength();
		
		while (curr >= 0 && curr < length) {
			char gotten;
			try {
				gotten = doc.getChar(curr);
			} catch (BadLocationException e) {
				break;
			}
			if (gotten == c) {
				buf.append(c);
			} else {
				break;
			}
			curr--;
		}
		String before = buf.toString();
		buf = new StringBuffer(10);
		curr = initial;
		
		while (curr >= 0 && curr < length) {
			char gotten;
			try {
				gotten = doc.getChar(curr);
			} catch (BadLocationException e) {
				break;
			}
			if (gotten == c) {
				buf.append(c);
			} else {
				break;
			}
			curr++;
		}
		String after = buf.toString();
		return new Tuple<String, String>(before, after);
	}

	public static boolean containsOnlyWhitespaces(String string) {
		for (int i = 0; i < string.length(); i++) {
			if (Character.isWhitespace(string.charAt(i)) == false) {
				return false;
			}
		}
		return true;
	}
}
