package com.dyalog.apldev.interactive_console.console;

/**
 * For extraction an object as a snippet of APL code suitable for dropping
 * into a Session console.
 */
interface IScriptConsoleCodeGenerator {
	/**
	 * @return APL code
	 */
	public String getAplCode();
	
	/**
	 * @return whether code is available from getAplCode()
	 */
	public boolean hasAplCode();
}