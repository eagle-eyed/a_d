package com.dyalog.apldev.debug.core.console;

import com.dyalog.apldev.debug.core.content.Tuple;

public interface IScriptConsoleInterpreter extends IScriptConsoleShell, IConsoleRequest {

	void exec(String command, ICallback<Object, InterpreterResponse> onResponseReceived);
	
	void setOnContentsReceivedCallback(ICallback<Object, Tuple<String, String>> onContentsReceived);
	
	void setOnEchoInput(ICallback< Object, String> onEchoInput);

	void setOnResponseReceived(ICallback<Object, InterpreterResponse> onResponseReceived);

	void edit(String text, int pos);
	
	
}
