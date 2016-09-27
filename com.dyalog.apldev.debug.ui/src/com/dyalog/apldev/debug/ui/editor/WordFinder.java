package com.dyalog.apldev.debug.ui.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Looks for an indentifier in a source file
 */
public class WordFinder {

	/**
	 * Returns the region in the given document that contains an identifier, or
	 * <code>null</code> if none.
	 * 
	 * @param document document to search
	 * @param offset offset at which to look for an identifier
	 * @return region containing an identifier, or <code>null</code>
	 */
	public static IRegion findWord(IDocument document, int offset) {
		
		int start = -1;
		int end = -1;
		
		try {
			
			int pos = offset;
			char c;
			
			while (pos >= 0) {
				c = document.getChar(pos);
				if (!Character.isJavaIdentifierPart(c))
					break;
				--pos;
			}
			
			start = pos;
			
			pos = offset;
			int length = document.getLength();
			
			while (pos < length) {
				c = document.getChar(pos);
				if (!Character.isJavaIdentifierPart(c))
					break;
				++pos;
			}
			
			end = pos;
			
		} catch (BadLocationException x) {
			
		}
		
		if (start > -1 && end > -1) {
			if (start == offset && end == offset)
				return new Region(offset, 0);
			else if (start == offset)
				return new Region(start, end - start);
			else
				return new Region(start + 1, end - start - 1);
		}
		
		return null;
	}
}
