package com.dyalog.apldev.debug.core.console;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

public interface IScriptConsoleShell {

	/**
	 * 
	 * @param viewer
	 * @param commandLine
	 * @param position
	 * @param offset
	 * @param whatToShow 
	 * 
	 * @return the proposals to be applied
	 * @throws Exception
	 */
	ICompletionProposal[] getCompletions(IScriptConsoleViewer viewer,
			String commandLine, int position, int offset, int whatToShow) throws Exception;
	
	/**
	 * 
	 * @param doc
	 * @param offset
	 * @return description to be shown to the user (hover)
	 * @throws Exception
	 */
	String getDescription(IDocument doc, int offset) throws Exception;
	
	/**
	 * Close the shell
	 * 
	 * @throws Exception
	 */
	void close() throws Exception;
}
