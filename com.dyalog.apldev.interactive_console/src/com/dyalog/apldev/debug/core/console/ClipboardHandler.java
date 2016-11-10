package com.dyalog.apldev.debug.core.console;

import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import com.dyalog.apldev.log.Log;

/**
 * Add text to the clipboard
 */
public class ClipboardHandler {

	/**
	 * Adds text form the given document to the clipboard, but without the text
	 * related to the prompt (gotten from the document partitioner).
	 * 
	 * @param doc the document from where the text should be gotten
	 * @param selectedRange the range selected for saving
	 * @param clipboardType the type of the clipboard
	 * @param display the display to be used
	 */
	public void putIntoClipboard(IDocument doc, Point selectedRange,
			int clipboardType, Display display) {
		String plainText = getPlainText(doc, selectedRange);
		if (plainText.length() == 0) {
			return;
		}
		putIntoClipboard(clipboardType, display, plainText);
	}
	
	public void putIntoClipboard(int clipboardType, Display display, String plainText)
			throws SWTError {
		Clipboard clipboard = new Clipboard(display);
		try {
			TextTransfer plainTextTransfer = TextTransfer.getInstance();
			String[] data = new String[] { plainText };
			Transfer[] types = new Transfer[] { plainTextTransfer };
			
			try {
				clipboard.setContents(data, types, clipboardType);
			} catch (SWTError error) {
				/* Copy to clipboard failed. This happens when another application
				 * is accessing the clipboard while we copy. Ignore the error
				 */
				if (error.code != DND.ERROR_CANNOT_SET_CLIPBOARD) {
					throw error;
				}
			}
		} finally {
			clipboard.dispose();
		}
	}
	
	/**
	 * @return the text in the given range that's related to actual code / output
	 * (but no the prompt)
	 */
	public String getPlainText(IDocument doc, Point selectedRange) {
		StringBuffer plainText = new StringBuffer();
		
		ScriptConsolePartitioner scriptConsolePartitioner =
				(ScriptConsolePartitioner) doc.getDocumentPartitioner();
		ScriptStyleRange[] ranges = scriptConsolePartitioner
				.getStyleRanges(selectedRange.x, selectedRange.y);
		if (ranges.length == 0) {
			return "";
		}
		
		try {
			int currentRange = 0;
			int minOffset = selectedRange.x;
			int maxOffset = selectedRange.x + selectedRange.y;
			
			/* note: we must iterate through the document and not through
			 * the ranges because new lines can have no range associated
			 */
			for (int i = minOffset; i < maxOffset; i++) {
				char c = doc.getChar(i);
				if (c == '\r' || c == '\n') {
					// new lines should be added for any style
					plainText.append(c);
				} else {
					ScriptStyleRange current = null;
					while (true) {
						if (currentRange >= ranges.length) {
							break;
						}
						current = ranges[currentRange];
						if (current.start <= i && i < current.start + current.length) {
							break;
						}
						currentRange++;
					}
					if (current == null) {
						continue;
					}
					if (current.scriptType == ScriptStyleRange.PROMPT) {
						continue;
					}
					plainText.append(c);
				}
			}
		} catch (Exception e) {
			Log.log(e);
		}
		return plainText.toString();
	}
	
	public static String getClipboardContents() {
		Clipboard cb = null;
		try {
			cb = new Clipboard(Display.getDefault());
			return cb.getContents(TextTransfer.getInstance()).toString();
		} finally {
			if (cb != null) {
				cb.dispose();
			}
		}
	}
}
