package com.dyalog.apldev.debug.core.console.actions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.PlatformUI;

import com.dyalog.apldev.debug.core.content.StringUtils;
import com.dyalog.apldev.debug.core.content.Tuple;
import com.dyalog.apldev.log.Log;

public class AplBackspace extends AplAction {

	private int dontEraseMoreThan = -1;
	
	public void setDontEraseMoreThan(int offset) {
		this.dontEraseMoreThan = offset;
	}

	public void perform(AplSelection ps) {
		try {
			ITextSelection textSelection = ps.getTextSelection();
			if (textSelection.getLength() != 0) {
				eraseSelection(ps);
				return;
			}
			
			int lastCharPosition = getLastCharPosition(ps.getDoc(), ps.getLineOffset());
			
			int cursorOffset = textSelection.getOffset();
			
			IRegion lastCharRegion = ps.getDoc()
					.getLineInformationOfOffset(lastCharPosition + 1);
			if (cursorOffset == lastCharRegion.getOffset()) {
				if (cursorOffset != 0) {
					// the first line
					eraseLineDelimiter(ps);
				}
			} else if (cursorOffset <= lastCharPosition) {
				eraseToPreviousIndentation(ps, false, lastCharRegion);
			} else if (lastCharRegion.getOffset() == lastCharPosition + 1) {
				eraseToPreviousIndentation(ps, true, lastCharRegion);
			} else {
				if (cursorOffset - lastCharPosition == 1) {
					// situation a|
					eraseSingleChar(ps);
				} else if (cursorOffset -lastCharPosition > 1) {
					// situation a  |
					eraseUntilLastChar(ps, lastCharPosition);
				}
			}
		} catch(Exception e) {
			beep(e);
		}
	}
	
	private void eraseUntilLastChar(AplSelection ps, int lastCharPosition)
			throws BadLocationException {
		ITextSelection textSelection = ps.getTextSelection();
		int cursorOffset = textSelection.getOffset();
		
		int offset = lastCharPosition + 1;
		int length = cursorOffset - lastCharPosition - 1;
		makeDelete(ps.getDoc(), offset, length);
	}

	private void eraseSelection(AplSelection ps) throws BadLocationException {
		ITextSelection textSelection = ps.getTextSelection();
		makeDelete(ps.getDoc(), textSelection.getOffset(),
				textSelection.getLength());
	}
	
	private void makeDelete(IDocument doc, int replaceOffset,
			int replaceLength) throws BadLocationException {
		if (replaceOffset < dontEraseMoreThan) {
			int delta = dontEraseMoreThan - replaceOffset;
			replaceOffset = dontEraseMoreThan;
			replaceLength -= delta;
			if (replaceLength <= 0) {
				return;
			}
		}
		doc.replace(replaceOffset, replaceLength, "");
	}
	
	private void eraseLineDelimiter(AplSelection ps) throws BadLocationException {
		ITextSelection textSelection = ps.getTextSelection();
		int length = getDelimiter(ps.getDoc()).length();
		int offset = textSelection.getOffset() - length;
		makeDelete(ps.getDoc(), offset, length);
	}
	
	private void eraseSingleChar(AplSelection ps) throws BadLocationException {
		ITextSelection textSelection = ps.getTextSelection();
		
		int replaceLength = 1;
		int replaceOffset = textSelection.getOffset() - replaceLength;
		IDocument doc = ps.getDoc();
		
		if (replaceOffset >= 0 && replaceOffset + replaceLength < doc.getLength()) {
			char c = doc.getChar(replaceOffset);
			if (c == '(' || c == '[' || c == '{') {
				// When removing a (, check if we have to delete the corresponding ) too
				char peer = StringUtils.getPeer(c);
				if (replaceOffset + replaceLength < doc.getLength()) {
					char c2 = doc.getChar(replaceOffset + 1);
					if (c2 == peer) {
						// Create a matcher only matching this char
						AplPairMatcher aplPairMatcher = new AplPairMatcher(new char[] { c, peer });
						int openingPeerOffset = aplPairMatcher
								.searchForAnyOpeningPeer(replaceOffset, doc);
						if (openingPeerOffset == -1) {
							replaceLength += 1;
						} else {
							int closingPeerOffset = aplPairMatcher
									.searchForClosingPeer(openingPeerOffset, c, peer, doc);
							if (closingPeerOffset != -1) {
								replaceLength += 1;
							}
						}
					}
				}
			} else if (c == '\'' || c == '"') {
				// when removing a ' or, check if we have to delete another ' or " too
				Tuple<String, String> beforeAndAfterMatchingChars = ps.getBeforeAndAfterMatchingChars(c);
				int matchesBefore = beforeAndAfterMatchingChars.o1.length();
				int matchesAfter = beforeAndAfterMatchingChars.o2.length();
				if (matchesBefore == 1 && matchesBefore == matchesAfter) {
					replaceLength += 1;
				}
			}
		}
		makeDelete(doc, replaceOffset, replaceLength);
	}
	
	private void eraseToPreviousIndentation(AplSelection ps,
			boolean hasOnlyWhitespaces, IRegion lastCharRegion) throws BadLocationException {
		String lineContentsToCursor = ps.getLineContentsToCursor();
		if (hasOnlyWhitespaces) {
			eraseToIndentation(ps, lineContentsToCursor);
		} else {
			if (AplSelection.containsOnlyWhitespaces(lineContentsToCursor)) {
				eraseToIndentation(ps, lineContentsToCursor);
			} else {
				eraseSingleChar(ps);
			}
		}
	}
	
	protected static void beep(Exception e) {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()
				.getDisplay().beep();
		} catch (Throwable x) {
			// workbench has still not been created
		}
		Log.log(e);
	}

	private void eraseToIndentation(AplSelection ps, String lineContentsToCursor)
			throws BadLocationException {
		final int cursorOffset = ps.getAbsoluteCursorOffset();
		final int cursorLine = ps.getCursorLine();
		final int lineContentsToCursorLen = lineContentsToCursor.length();
		
		if (lineContentsToCursorLen > 0) {
			char c = lineContentsToCursor.charAt(lineContentsToCursorLen - 1);
			if (c == '\t') {
				eraseSingleChar(ps);
				return;
			}
		}
		
		String indentationString = "\t"; //getIndentPrefs().getIndentationString();
		
		int replaceLength;
		int replaceOffset;
		
		final int indentationLength = indentationString.length();
		final int modLen = lineContentsToCursorLen % indentationLength;
		
		if (modLen == 0) {
			replaceOffset = cursorOffset - indentationLength;
			replaceLength = indentationLength;
		} else {
			replaceOffset = cursorOffset - modLen;
			replaceLength = modLen;
		}
		
		IDocument doc = ps.getDoc();
		if (cursorLine > 0) {
			IRegion prevLineInfo = doc.getLineInformation(cursorLine - 1);
			int prevLineEndOffset = prevLineInfo.getOffset() + prevLineInfo.getLength();
//			Tuple<Integer, Boolean> tup = 
		}
	}

}
