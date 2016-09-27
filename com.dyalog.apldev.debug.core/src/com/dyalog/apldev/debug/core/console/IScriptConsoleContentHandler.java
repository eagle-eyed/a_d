package com.dyalog.apldev.debug.core.console;

/**
 * Used to handle content assist requests.
 */
public interface IScriptConsoleContentHandler {

	void contentAssistRequired();
	
	void quickAssistRequired();
}
