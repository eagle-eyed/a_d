package com.dyalog.apldev.interactive_console.console;

import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.console.IHyperlink;

public class AplConsoleLineTracker implements IConsoleLineTracker {

	private ILinkContainer linkContainer;

	@Override
	public void init(IConsole console) {
		this.linkContainer = new ILinkContainer() {
			
			@Override
			public void addLink(IHyperlink link, int offset, int length) {
				console.addLink(link, offset, length);
			}
			
			@Override
			public String getContents(int offset, int length) throws BadLocationException {
				return console.getDocument().get(offset, length);
			}

		};
	}

	@Override
	public void lineAppended(IRegion line) {
		System.out.println("AplConsoleLineTracker: "+ line);
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {

	}

}
