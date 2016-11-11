package com.dyalog.apldev.interactive_console.console;

import org.eclipse.debug.core.model.IProcess;

import com.dyalog.apldev.interactive_console.content.Tuple;

public interface IScriptConsoleInterpreter extends IScriptConsoleShell, IConsoleRequest {

	void exec(String command, ICallback<Object, InterpreterResponse> onResponseReceived);
	
	void setOnContentsReceivedCallback(ICallback<Object, Tuple<String, String>> onContentsReceived);
	
	void setOnEchoInput(ICallback< Object, String> onEchoInput);

	void setOnResponseReceived(ICallback<Object, InterpreterResponse> onResponseReceived);

	void edit(String text, int pos);
	
	public String getName();
	
	public boolean isRIDEConsole();
	
	void postRIDECommand(String input);
	
	void postSessionPrompt(String input);
	
	IProcess getProcess();
	
	boolean isTerminated();
}
