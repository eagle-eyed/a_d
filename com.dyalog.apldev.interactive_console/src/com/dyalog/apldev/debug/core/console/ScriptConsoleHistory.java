package com.dyalog.apldev.debug.core.console;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import com.dyalog.apldev.log.Log;

public class ScriptConsoleHistory {

	/**
	 * Holds the history as List of Strings
	 */
	private final List<String> lines;
	
	/**
	 * Set to true once history has been closed
	 */
	private volatile boolean closed = false;
	/**
	 * Holds the position of the current line in the history
	 */
	private int currLine;
	/**
	 * Holds the history as a document
	 */
	private Document historyAsDoc;
	/**
	 * Index of the starting point of local history
	 */
	private int localHistoryStart;
	/**
	 * When getting previous or next, this string must be matched
	 */
	private String matchStart = "";
	
	public ScriptConsoleHistory() {
		this.lines = new ArrayList<String>();
//		this.lines = ScriptConsoleGlobalHistory.INSTANCE.get();
		// TODO add support of GlobalHistory
		StringBuilder globalHistory = new StringBuilder();
		for (String line : this.lines){
			globalHistory.append(line);
			globalHistory.append("\n");
		}
		
		if (this.lines.size() ==  0
				|| this.lines.get(this.lines.size() - 1).length() != 0) {
			this.lines.add("");
		}
		
		localHistoryStart = this.lines.size() - 1;
		this.currLine = this.lines.size() - 1;
		
		this.historyAsDoc = new Document(globalHistory.toString());
	}

	/**
	 * Close the current history, appending to the global history any new commands
	 */
	public synchronized void close() {
		if (closed)
			return;
		closed = true;
//		ScriptConsoleGlobalHistory.INSTANCE.append(lines.subList(localHistoryStart, lines.size() - 1));
	}
	
	/**
	 * Updates the current line in the buffer for the history
	 * 
	 * @param line contents to be added to the top of the command history
	 */
	public void update(String line) {
//		if (lines.size() > 1)
			lines.set(lines.size() - 1, line);
	}
	
	/**
	 * @return all the elements from the command history except last
	 */
	public List<String> getAsList() {
		ArrayList<String> list = new ArrayList<String>(lines);
		if (list.size() > 0) {
			list.remove(list.size() - 1); // remove the last ""
		}
		return list;
	}

	/**
	 * @return the contents of the line that wasn't added to the history yet
	 */
	public String getBufferLine() {
		return lines.get(lines.size() - 1);
	}
	
	/**
	 * Commits the currently added line (last called in update) to the history
	 * and keeps it there
	 */
	public void commit() {
		String lineToAddToHistory = getBufferLine();
		try {
			historyAsDoc.replace(historyAsDoc.getLength(), 0, lineToAddToHistory + "\n");
		} catch (BadLocationException e) {
			Log.log(e);
		}
		
		if (lineToAddToHistory.length() == 0) {
			currLine = lines.size() - 1;
			return;
		}
		
		lines.set(lines.size() - 1, lineToAddToHistory);
		
		lines.add("");
		currLine = lines.size() - 1;
	}
	
	/**
	 * Contents of the current line in the history
	 */
	public String get() {
		if (lines.isEmpty()) {
			return "";
		}
		return lines.get(currLine);
	}
	
	/**
	 * The document with the contents of this history. Should not be changed
	 * externally
	 */
	public IDocument getAsDoc() {
		return historyAsDoc;
	}
	
	/**
	 * Delete the local history
	 */
	public void clear() {
//		ScriptConsoleGlobalHistory.INSTANCE.clear();
		lines.clear();
		lines.add("");
		localHistoryStart = 0;
		historyAsDoc.set("");
		currLine = 0;
	}
	
	/**
	 * Jump to next line in the history which begins with matchStart
	 */
	public boolean next() {
		int initialCurrLine = currLine;
		while (true) {
			if (currLine >= lines.size() - 2) {
				// don't add 'current' line
				break;
			}
			currLine++;
			String curr = get();
			if (curr.startsWith(this.matchStart))
				return true;
		}
		currLine = initialCurrLine;
		return false;
	}
	
	/**
	 * Jump to previous line which begins with matchStart
	 */
	public boolean prev() {
		int initialCurrLine = currLine;
		while (true) {
			if (currLine <= 0)
				break;
			currLine--;
			String curr = get();
			if (curr.startsWith(this.matchStart))
				return true;
		}
		currLine = initialCurrLine;
		return false;
	}
	
	/**
	 * Set beginning string for prev() and next() functions
	 */
	public void setMatchStart(String matchStart) {
		this.matchStart = matchStart;
	}
}
