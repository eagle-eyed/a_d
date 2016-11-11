package com.dyalog.apldev.interactive_console.console;

public class ScriptConsoleSession implements IScriptConsoleListener,
	IScriptConsoleSession {
	
	private StringBuffer session;

	public ScriptConsoleSession() {
		this.session = new StringBuffer();
	}

	@Override
	public void userRequest(String text, ScriptConsolePrompt prompt) {
		session.append(prompt.toString());
		session.append(text);
		session.append('\n');
	}

	@Override
	public void interpreterResponse(InterpreterResponse response, ScriptConsolePrompt prompt) {
		// asynchronous added through onStdoutContentsReceived and onStderrContentsReceived
		
	}

	@Override
	public void onStdoutContentsReceived(String o1) {
		session.append(o1);
		
	}

	@Override
	public void onStderrContentsReceived(String o2) {
		session.append(o2);
		
	}

}
