package com.dyalog.apldev.debug.core.console;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IHyperlink;

public interface ILinkContainer {

	void addLink(IHyperlink link, int offset, int length);
	
	String getContents(int lineOffset, int lineLength) throws BadLocationException;
	
}
