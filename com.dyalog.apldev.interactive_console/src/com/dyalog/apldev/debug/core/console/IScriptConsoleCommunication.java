package com.dyalog.apldev.debug.core.console;

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
