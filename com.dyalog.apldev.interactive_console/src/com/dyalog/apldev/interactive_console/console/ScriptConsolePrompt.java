package com.dyalog.apldev.interactive_console.console;

/**
 * Shows the prompt to the user (using the #toString()) method
 * (e.g.: shows >>> or ... )
 */
public class ScriptConsolePrompt {

	/**
	 * String to be shown when a new command is requested
	 */
	private final String newCommand;
	/**
	 * String to be shown when the command still needs input to finish (e.g.: start class declaration)
	 */
	private final String continueCommand;
	private boolean commandComplete;
	private boolean needInput;
	/**
	 * 0 - no prompt
	 * 1 - the usual 6-space APL prompt
	 * 2 - Quad(⎕) input
	 * 3 - line editor
	 * 4 - Quote-Quad(�?�) input
	 * 5 - any prompt type unforeseen here
	 */
	private int type;
	
	public ScriptConsolePrompt(String newCommand, String appendCommand) {
		this.newCommand = newCommand;
		this.continueCommand = appendCommand;
		this.commandComplete = true;
	}
	
	/**
	 * Set console prompt type (from 0 to 5)
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Get console prompt type (from 0 to 5)
⌈	 */
	public int getType() {
		return this.type;
	}
	/**
	 * Sets the mode for the prompt.
	 * 
	 * @param mode true for a new command, false for 'continue'
	 */
	public void setMode(boolean mode) {
		this.commandComplete = mode;
	}
	
	@Override
	public String toString() {
		if (needInput) {
			return "";
		}
		return commandComplete ? newCommand : continueCommand;
	}
	
	/**
	 * Sets whether the user is waiting for input. If it's, don't show the prompt.
	 */
	public void setNeedInput(boolean needInput) {
		this.needInput = needInput;
	}
	
	public boolean getNeedInput() {
		return this.needInput;
	}
	
	public boolean getNeedMore() {
		return !commandComplete;
	}
}
