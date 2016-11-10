package com.dyalog.apldev.debug.core.console;

public interface IScriptConsoleSession {

	void onStdoutContentsReceived(String o1);
	
	void onStderrContentsReceived(String o2);

}
