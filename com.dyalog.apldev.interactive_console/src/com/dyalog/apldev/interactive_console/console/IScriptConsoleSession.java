package com.dyalog.apldev.interactive_console.console;

public interface IScriptConsoleSession {

	void onStdoutContentsReceived(String o1);
	
	void onStderrContentsReceived(String o2);

}
