package com.dyalog.apldev.debug.core.console;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.handlers.IHandlerService;

import com.dyalog.apldev.debug.core.content.Tuple;
import com.dyalog.apldev.debug.core.model.APLDebugTarget;
import com.dyalog.apldev.debug.core.model.remote.CommandProcessing;
import com.dyalog.apldev.debug.core.model.remote.DebuggerWriter;

public class AplDevConsoleInterpreter implements IScriptConsoleInterpreter {

	private ICallback<Object, Tuple<String, String>> onContentsReceived;
	private DebuggerWriter debuggerWriter;
	private CommandProcessing commandsProcessing;
	private ICallback<Object, String> onEchoInput;
	private APLDebugTarget debugTarget;

	AplDevConsoleInterpreter (APLDebugTarget debugTarget) {
		this.debugTarget = debugTarget;
	}
	
	public void setDebuggerWriter(DebuggerWriter debuggerWriter) {
		this.debuggerWriter = debuggerWriter;
	}
	
	public void setCommandsProcessing(CommandProcessing commandsProcessing) {
		this.commandsProcessing = commandsProcessing;
		if (onContentsReceived != null) {
			commandsProcessing.setOnContentsReceived(onContentsReceived);
			commandsProcessing.setOnEchoInput(onEchoInput);
		}
	}
	
	@Override
	public ICompletionProposal[] getCompletions(IScriptConsoleViewer viewer, String commandLine, int position,
			int offset, int whatToShow) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription(IDocument doc, int offset) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setConsoleCommunication(IScriptConsoleCommunication protocol) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exec(String command, ICallback<Object, InterpreterResponse> onResponseReceived) {
		if (commandsProcessing != null)
			commandsProcessing.setOnResponseReceived(onResponseReceived);
		if (debuggerWriter != null)
			debuggerWriter.postSessionPrompt(command);
		
	}

	@Override
	public void edit(String text, int pos) {
		if (debugTarget != null) {
//			debugTarget.getInterpreterWriter().postEdit(0, pos, text);
			debugTarget.EditEntity(0, pos, text);
//			IHandlerService handlerService = getSite().getService(IHandlerService.class);
//			try {
//				handlerService.executeCommand("example.editor.commands.OpenEditor", null);
//			} catch (Exception ex) {
//				throw new RuntimeException (ex.getMessage());
//			}

		}
	}
	
	@Override
	public void setOnContentsReceivedCallback(ICallback<Object, Tuple<String, String>> onContentsReceived) {
		this.onContentsReceived = onContentsReceived;
		if (commandsProcessing != null)
			commandsProcessing.setOnContentsReceived(onContentsReceived);
	}

	@Override
	public void setOnEchoInput(ICallback <Object, String> onEchoInput) {
		this.onEchoInput = onEchoInput;
		if (commandsProcessing != null)
			commandsProcessing.setOnEchoInput(onEchoInput);
	}
	
	@Override
	public void setOnResponseReceived(ICallback <Object, InterpreterResponse> onResponseReceived) {
		if (commandsProcessing != null)
			commandsProcessing.setOnResponseReceived(onResponseReceived);
	}
}
