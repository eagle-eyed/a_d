package com.dyalog.apldev.debug.core.console.actions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.console.IScriptConsoleViewer;
import com.dyalog.apldev.debug.core.console.ScriptConsolePartitioner;
import com.dyalog.apldev.debug.core.console.ScriptStyleRange;
import com.dyalog.apldev.debug.core.content.TextSelectionUtils;

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
			APLDebugCorePlugin.log(e);
		}
		return false;
	}
}
