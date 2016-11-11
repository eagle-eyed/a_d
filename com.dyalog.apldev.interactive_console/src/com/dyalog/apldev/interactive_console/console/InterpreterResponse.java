package com.dyalog.apldev.interactive_console.console;

public class InterpreterResponse {

	public final int prompt;
	public final boolean more;
	public final boolean need_input;
	
	public InterpreterResponse(boolean more, boolean need_input,
			int prompt) {
		this.prompt = prompt;
		this.more = more;
		this.need_input = need_input;
	}
	
	public InterpreterResponse(int prompt) {
		this.prompt = prompt;
		this.more = false;
		this.need_input = false;
	}
}
