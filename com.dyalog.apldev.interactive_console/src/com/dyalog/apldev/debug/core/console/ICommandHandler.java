package com.dyalog.apldev.debug.core.console;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import com.dyalog.apldev.debug.core.content.Tuple;

public interface ICommandHandler {

	void handleCommand(String userInput, ICallback<Object, InterpreterResponse> onResonseReceived);
	
	public ICompletionProposal[] getTabCompletions(String commandLine, int cursorPosition);

	void setOnContentsReceivedCallback(ICallback<Object, Tuple<String, String>> onContentsReceived);
	
	void setOnEchoInput(ICallback< Object, String > onEchoInput);
	
	void beforeHandleCommand(String userInput, ICallback<Object, InterpreterResponse> onResponceReceived);

	void setOnResponseReceived(ICallback<Object, InterpreterResponse> onResponseReceived);

	void handleEdit(String str, int pos);

}
