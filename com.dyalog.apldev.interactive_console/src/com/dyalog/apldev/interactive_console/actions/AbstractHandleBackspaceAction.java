package com.dyalog.apldev.interactive_console.actions;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;

public abstract class AbstractHandleBackspaceAction {

	public abstract void execute(IDocument doc, ITextSelection selection,
			int commandLineOffset);

}
