package com.dyalog.apldev.interactive_console.actions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorActionDelegate;

public abstract class AplAction extends BaseAction implements IEditorActionDelegate {

	protected AplAction() {
		super();
	}
	
	protected AplAction(String text, int style) {
		super(text, style);
	}
	
	/**
	 * Returns the position of the last non whitespace char in the current line
	 */
	protected int getLastCharPosition(IDocument doc, int cursorOffset)
			throws BadLocationException {
		IRegion region;
		region = doc.getLineInformationOfOffset(cursorOffset);
		int offset = region.getOffset();
		String src = doc.get(offset, region.getLength());
		
		int i = src.length();
		boolean breaked = false;
		while (i > 0) {
			i--;
			// break if find not a whitespace or a tab
			if (Character.isWhitespace(src.charAt(i)) == false
					&& src.charAt(i) != '\t') {
				breaked = true;
				break;
			}
		}
		if (!breaked) {
			i--;
		}
		return (offset + i);
	}
	
	public static String getDelimiter(IDocument doc) {
		return AplSelection.getDelimiter(doc);
	}
}
