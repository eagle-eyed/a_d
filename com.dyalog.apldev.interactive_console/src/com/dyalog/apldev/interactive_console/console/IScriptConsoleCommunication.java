package com.dyalog.apldev.interactive_console.console;

//TODO communication with interpreter
public interface IScriptConsoleCommunication {

	/**
	 * Executes a given command in the interpreter
	 * 
	 * @param command
	 * @param onResponseReceived
	 */
	void execInterpreter(String command, ICallback<Object, InterpreterResponse> onResponseReceived);

}
