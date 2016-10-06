package com.dyalog.apldev.debug.core.console;

import java.lang.ref.WeakReference;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.ui.console.IConsoleDocumentPartitioner;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.part.IPageBookViewPage;

import com.dyalog.apldev.debug.core.console.actions.AbstractHandleBackspaceAction;
import com.dyalog.apldev.debug.core.console.prefs.ColorManager;
import com.dyalog.apldev.debug.core.content.Tuple;

public abstract class ScriptConsole extends TextConsole implements ICommandHandler {

	protected ScriptConsolePage page;
	protected ListenerList fConsoleListeners;
	protected ScriptConsoleHistory history;
	protected ScriptConsolePrompt prompt;
	protected ScriptConsoleSession session;
	protected ScriptConsolePartitioner fPartitioner;
	private WeakReference<ScriptConsoleViewer> viewer;
	private AplDevConsoleInterpreter interpreter;
	private boolean fTerminated;
	
	
	public ScriptConsole(String name, ImageDescriptor imageDescriptor,
			AplDevConsoleInterpreter interpreter) {
		super(name, "org.dyalog.APLDev.debug.console", imageDescriptor, true);
		
		this.interpreter = interpreter;
		this.fConsoleListeners = new ListenerList(ListenerList.IDENTITY);
		this.prompt = createConsolePrompt();
		this.history = new ScriptConsoleHistory();
		this.session = new ScriptConsoleSession();
		addListener(this.session);
		fPartitioner = new ScriptConsolePartitioner();
		getDocument().setDocumentPartitioner(fPartitioner);
		fPartitioner.connect(getDocument());
	}

	@Override
	protected IConsoleDocumentPartitioner getPartitioner() {
		// TODO Auto-generated method stub
		return null;
	}

	protected abstract ScriptConsolePrompt createConsolePrompt();
	
	public void addListener(IScriptConsoleListener listener) {
		fConsoleListeners.add(listener);
	}
	
	public void terminate() {
		if (isTerminated())
			return;
		fTerminated = true;
		if (history != null)
			history.close();
		history = null;
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				setName("<terminated> " + getName());
			}
			
		};
		RunInUiThread.sync(runnable);
//		ColorManager colMan = ColorManager.getDefault();
//		colMan.dispose();
	}
	
	public boolean isTerminated() {
		return fTerminated;
	}

	/**
	 * Creates the actual page to be shown to the user.
	 */
	@Override
	public IPageBookViewPage createPage(IConsoleView view) {
		page = new ScriptConsolePage(this, view, createSourceViewerConfiguration());
		return page;
	}
	public abstract SourceViewerConfiguration createSourceViewerConfiguration();

	public ScriptConsoleViewer getViewer() {
		if (this.viewer != null)
			return this.viewer.get();
		return null;
	}

	public void setViewer(ScriptConsoleViewer scriptConsoleViewer) {
		this.viewer = new WeakReference<ScriptConsoleViewer>(scriptConsoleViewer);
	}

	public ScriptConsoleHistory getHistory() {
		return history;
	}

	public ScriptConsolePrompt getPrompt() {
		return prompt;
	}
	
	@Override
	public void beforeHandleCommand(String userInput,
			ICallback<Object, InterpreterResponse> onResponseReceived) {
		final Object[] listeners = fConsoleListeners.getListeners();
		
		// notify about the user request in the UI thread
		for (Object listener : listeners) {
			((IScriptConsoleListener) listener).userRequest(userInput, prompt);
		}
	}

	/**
	 * @return a list of trackers that'll identify links in the console passed
	 */
	public abstract List<IConsoleLineTracker> createLineTrackers(final TextConsole console);

	public abstract IConsoleStyleProvider createSyleProvider();

	/**
	 * @return the commands that should be initially set in the prompt
	 */
	public abstract String getInitialCommands();

	public abstract boolean getFocusOnStart();

	public abstract AbstractHandleBackspaceAction getBackspaceAction();

	public abstract IHandleScriptAutoEditStrategy getAutoEditStrategy();

	public abstract boolean getTabCompletionEnabled();
	
	/**
	 * @return the text hover to be used in the console
	 */
	protected abstract ITextHover createHover();
	
	public IScriptConsoleSession getSession() {
		return session;
	}
	/**
	 * Handles some command that the user entered
	 */
	@Override
	public void handleCommand(String userInput,
			ICallback<Object, InterpreterResponse> onResponseReceived) {
		
		if (isTerminated()) return;
		final Object[] listeners = fConsoleListeners.getListeners();
		
		// executes the user input in the interpreter
		if (interpreter != null) {
			interpreter.exec(userInput,
					new ICallback <Object, InterpreterResponse> () {
				
				@Override
				public Object call (final InterpreterResponse response) {
					// sets the new mode
					prompt.setMode(!response.more);
					prompt.setNeedInput(response.need_input);
					// notify about the console answer (not in the UI thread)
					for (Object listener : listeners) {
						((IScriptConsoleListener) listener).interpreterResponse(response, prompt);
					}
					onResponseReceived.call(response);
					return null;
				}
			});
		}
//		ICallBack<Object, InterpreterResponce> var 
//			= new ICallBack<<Object, InterpreterResponce>() {
//			
//			@Override
//			public Object call(final InterpreterResponce responce) {
				// sets the new mode
//		InterpreterResponse response = new InterpreterResponse(false, false);
//		prompt.setMode(!response.more);
//		prompt.setNeedInput(response.need_input);
//				
//		// notify about the console answer (not in the UI thread)
////		for (Object listener : listeners) {
////			((IScriptConsoleListener) listener).interpreterResponce(response, prompt);
////			((IScriptConsoleListener) listener).onContentsReceived.
////		}
//		onResponseReceived.call(response);
	}
	
	
	@Override
	public void setOnContentsReceivedCallback(
			ICallback<Object, Tuple<String, String>> onContentsReceived) {
		interpreter.setOnContentsReceivedCallback(onContentsReceived);
	}
	
	@Override
	public void setOnEchoInput(ICallback< Object, String > onEchoInput) {
		interpreter.setOnEchoInput(onEchoInput);
	}
	
	@Override
	public void setOnResponseReceived(ICallback <Object, InterpreterResponse> onResponseReceived) {
		interpreter.setOnResponseReceived(onResponseReceived);
	}

	/**
	 * Clear console button action
	 */
	@Override
	public void clearConsole() {
		page.clearConsolePage();
	}

}
