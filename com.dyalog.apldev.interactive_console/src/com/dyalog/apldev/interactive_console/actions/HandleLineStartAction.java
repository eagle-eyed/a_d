package com.dyalog.apldev.interactive_console.actions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import com.dyalog.apldev.interactive_console.console.IScriptConsoleViewer;
import com.dyalog.apldev.interactive_console.console.ScriptConsolePartitioner;
import com.dyalog.apldev.interactive_console.console.ScriptStyleRange;
import com.dyalog.apldev.interactive_console.content.TextSelectionUtils;
import com.dyalog.apldev.log.Log;

/**
 * Go to the strt of the line (Home)
 */
public class HandleLineStartAction {

	public boolean execute(IDocument doc, int caretOffset, int commandLineOffset,
			IScriptConsoleViewer viewer) {
		try {
			TextSelectionUtils ps = new TextSelectionUtils(doc, caretOffset);
			int lineOffset = ps.getLineOffset();
			int promptEndOffset = lineOffset;
			ScriptConsolePartitioner partitioner
				= (ScriptConsolePartitioner) doc.getDocumentPartitioner();
			int docLen = doc.getLength();
			
			for (; promptEndOffset < docLen; promptEndOffset++) {
				ScriptStyleRange[] range = partitioner
						.getStyleRanges(promptEndOffset, 1);
				if (range.length >= 1) {
					if (range[0].scriptType != ScriptStyleRange.PROMPT){
						break;
					}
				}
			}
			
			int absoluteCursorOffset = ps.getAbsoluteCursorOffset();
			
			IRegion lineInformation = doc.getLineInformationOfOffset(absoluteCursorOffset);
			String contentsFromPrompt = doc.get(promptEndOffset,
					lineInformation.getOffset() + lineInformation.getLength() - promptEndOffset);
			int firstCharPosition = TextSelectionUtils.getFirstCharPosition(contentsFromPrompt);
			int firstCharOffset = promptEndOffset + firstCharPosition;
			
			if (lineOffset == absoluteCursorOffset || firstCharOffset < absoluteCursorOffset) {
				viewer.setCaretOffset(firstCharOffset, false);
				return true;
			}
			
			if (promptEndOffset < absoluteCursorOffset) {
				viewer.setCaretOffset(promptEndOffset, false);
				return true;
			}
			
			if (promptEndOffset < absoluteCursorOffset) {
				viewer.setCaretOffset(promptEndOffset, false);
				return true;
			}
			
			viewer.setCaretOffset(lineOffset, false);
			return true;
			
		} catch (BadLocationException e) {
			Log.log(e);
		}
		return false;
	}
}
