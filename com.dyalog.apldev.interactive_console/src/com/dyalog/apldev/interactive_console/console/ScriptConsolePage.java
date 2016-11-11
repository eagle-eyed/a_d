package com.dyalog.apldev.interactive_console.console;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.console.TextConsolePage;
import org.eclipse.ui.console.TextConsoleViewer;

public class ScriptConsolePage extends TextConsolePage
	implements IScriptConsoleContentHandler {

	
	private SourceViewerConfiguration cfg;
	private ScriptConsoleViewer viewer;
	
	
	public ScriptConsolePage(TextConsole console, IConsoleView view,
			SourceViewerConfiguration cfg) {
		super(console, view);
		this.cfg = cfg;
	}

	@Override
	protected void contextMenuAboutToShow(IMenuManager menuManager) {
		super.contextMenuAboutToShow(menuManager);
	}
	
	@Override
	public void contentAssistRequired() {
//		proposalsAction.run();
	}

	@Override
	public void quickAssistRequired() {
//		quickAssistAction.run();
	}
	
	@Override
	protected TextConsoleViewer createViewer(Composite parent) {
		ScriptConsole console = (ScriptConsole) getConsole();
		viewer = new ScriptConsoleViewer(parent, console, this, console.createSyleProvider(),
				console.getInitialCommands(), console.getFocusOnStart(), console.getBackspaceAction(),
				console.getAutoEditStrategy(), console.getTabCompletionEnabled(), true);
		viewer.configure(cfg);
		return viewer;
	}

	
	public void clearConsolePage() {
		viewer.clear(false);
		
	}
	
}
