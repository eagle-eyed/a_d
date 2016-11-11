package com.dyalog.apldev.debug.core.console;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import com.dyalog.apldev.ApldevPlugin;
import com.dyalog.apldev.debug.core.model.APLDebugTarget;
import com.dyalog.apldev.interactive_console.console.ICallback;
import com.dyalog.apldev.interactive_console.console.IScriptConsoleCommunication;
import com.dyalog.apldev.interactive_console.console.IScriptConsoleInterpreter;
import com.dyalog.apldev.interactive_console.console.IScriptConsoleViewer;
import com.dyalog.apldev.interactive_console.console.InterpreterResponse;
import com.dyalog.apldev.interactive_console.content.Tuple;

public class AplDevConsoleInterpreter implements IScriptConsoleInterpreter {

	private ICallback<Object, Tuple<String, String>> onContentsReceived;
//	private DebuggerWriter debuggerWriter;
//	private CommandProcessing commandsProcessing;
	private ICallback<Object, String> onEchoInput;
	private APLDebugTarget debugTarget;

	public AplDevConsoleInterpreter (APLDebugTarget debugTarget) {
		this.debugTarget = debugTarget;
	}
	
//	public void setDebuggerWriter(DebuggerWriter debuggerWriter) {
//		this.debuggerWriter = debuggerWriter;
//	}
//	
//	public void setCommandsProcessing(CommandProcessing commandsProcessing) {
//		this.commandsProcessing = commandsProcessing;
//		if (onContentsReceived != null) {
//			commandsProcessing.setOnContentsReceived(onContentsReceived);
//			commandsProcessing.setOnEchoInput(onEchoInput);
//		}
//	}
	
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
		if (debugTarget != null) {
			debugTarget.getCommandProc().setOnResponseReceived(onResponseReceived);
			debugTarget.getInterpreterWriter().postSessionPrompt(command);
		}
		
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
		if (debugTarget != null) {
			debugTarget.getCommandProc().setOnContentsReceived(onContentsReceived);
		}
	}

	@Override
	public void setOnEchoInput(ICallback <Object, String> onEchoInput) {
		this.onEchoInput = onEchoInput;
		if (debugTarget != null) {
			debugTarget.getCommandProc().setOnEchoInput(onEchoInput);
		}
	}
	
	@Override
	public void setOnResponseReceived(ICallback <Object, InterpreterResponse> onResponseReceived) {
		if (debugTarget != null)
			debugTarget.getCommandProc().setOnResponseReceived(onResponseReceived);
	}
	
	@Override
	public String getName() {
		String name = null;
		if (debugTarget != null) {
			try {
				name = debugTarget.getName();
			} catch (DebugException e) {

			}
		}
		return name;
	}
	
	@Override
	public boolean isRIDEConsole() {
		ILaunch launch = debugTarget.getLaunch();
		ILaunchConfiguration conf = launch.getLaunchConfiguration();
		boolean enableRIDEConsole = false;
		try {
			enableRIDEConsole = conf.getAttribute(ApldevPlugin.ATTR_SHOW_RIDE, false);
		} catch (CoreException e) {
			
		}
		return enableRIDEConsole;
	}
	
	@Override
	public void postRIDECommand(String input) {
		debugTarget.postRIDECommand(input);
	}
	
	@Override
	public void postSessionPrompt(String input) {
		debugTarget.postSessionPrompt(input);
	}
	
	@Override
	public IProcess getProcess() {
		return debugTarget.getProcess();
	}
	
	@Override
	public boolean isTerminated() {
		return debugTarget.isTerminated();
	}
}
