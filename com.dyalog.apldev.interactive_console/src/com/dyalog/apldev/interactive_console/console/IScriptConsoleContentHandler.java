package com.dyalog.apldev.interactive_console.console;

/**
 * Used to handle content assist requests.
 */
public interface IScriptConsoleContentHandler {

	void contentAssistRequired();
	
	void quickAssistRequired();
}
