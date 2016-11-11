package com.dyalog.apldev.interactive_console.console;

public interface IScriptConsoleListener {

	// Called in the UI thread before command is entered.
	void userRequest(String text, ScriptConsolePrompt promt);
	
	// Called out of the UI thread.
	void interpreterResponse(InterpreterResponse response, ScriptConsolePrompt prompt);
}
