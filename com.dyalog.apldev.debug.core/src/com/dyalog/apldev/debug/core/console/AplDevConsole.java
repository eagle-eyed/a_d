package com.dyalog.apldev.debug.core.console;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.ui.console.TextConsole;

import com.dyalog.apldev.debug.core.console.actions.AbstractHandleBackspaceAction;
import com.dyalog.apldev.debug.core.console.actions.AplAutoIndentStrategy;
import com.dyalog.apldev.debug.core.console.actions.HandleBackspaceAction;
import com.dyalog.apldev.debug.core.console.prefs.ColorManager;
import com.dyalog.apldev.debug.core.console.ui.DefaultScriptConsoleTextHover;
import com.dyalog.apldev.debug.core.content.Tuple;

public class AplDevConsole extends ScriptConsole {

	public static final String CONSOLE_NAME = "APLDev Console";
	IProcess process = null;
	private IScriptConsoleInterpreter interpreter;
	
	public AplDevConsole(String name, ImageDescriptor imageDescriptor,
			AplDevConsoleInterpreter interpreter) {
		super(name, imageDescriptor, interpreter);
		
		this.interpreter = interpreter;
//		boolean runNow = true;
//		RunInUiThread.async(new Runnable() {
//			
//			@Override
//			public void run() {
//				setAPLDevConsoleBackground(getPreferenceColor(CONSOLE_BACKGROUND_COLOR) );
//			}
//			
//		}, runNow);
	}

	@Override
	public SourceViewerConfiguration createSourceViewerConfiguration() {
		AplContentAssistant contentAssist = new AplContentAssistant();
		AplCorrectionAssistant quickAssist = new AplCorrectionAssistant();
		SourceViewerConfiguration cfg 
			= new ApldevScriptConsoleSourceViewerConfiguration(createHover(),
				contentAssist, quickAssist);
		return cfg;
	}
	
	/**
	 * @return the text hover to be used in the console
	 */
	@Override
	protected ITextHover createHover() {
		return new DefaultScriptConsoleTextHover(this.interpreter);
	}
	
	public ScriptConsolePrompt createConsolePrompt() {
		return new ScriptConsolePrompt(">>>   ","...");
	}
	
	/**
	 * add hyperlinks to the console
	 */
	@Override
	public List<IConsoleLineTracker> createLineTrackers(final TextConsole console) {
		return staticCreateLineTrackers(console);
	}

	private List<IConsoleLineTracker> staticCreateLineTrackers(final TextConsole console) {
		List<IConsoleLineTracker> lineTrackers = new ArrayList<IConsoleLineTracker>();
		AplConsoleLineTracker lineTracker = new AplConsoleLineTracker();
		return null;
	}

	@Override
	public IConsoleStyleProvider createSyleProvider() {
		return new ConsoleStyleProvider();
	}

	@Override
	public String getInitialCommands() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getFocusOnStart() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AbstractHandleBackspaceAction getBackspaceAction() {
		return new HandleBackspaceAction();
	}

	@Override
	public IHandleScriptAutoEditStrategy getAutoEditStrategy() {
		return new AplAutoIndentStrategy(new IAdaptable() {
			@Override
			public <T> T getAdapter(Class<T> adapter) {
				return null;
			}
		});
	}

	@Override
	public boolean getTabCompletionEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ICompletionProposal[] getTabCompletions(String commandLine, int cursorPosition) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void handleEdit(String text, int pos) {
		if (interpreter != null)
			interpreter.edit(text, pos);
	}

}
