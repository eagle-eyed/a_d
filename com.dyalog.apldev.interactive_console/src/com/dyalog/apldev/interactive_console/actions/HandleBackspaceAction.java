package com.dyalog.apldev.interactive_console.actions;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;

public class HandleBackspaceAction extends AbstractHandleBackspaceAction {

	@Override
	public void execute(IDocument doc, ITextSelection selection, int commandLineOffset) {
		
		AplBackspace aplBackspace = new AplBackspace();
		aplBackspace.setDontEraseMoreThan(commandLineOffset);
//		aplBackspace.setIndentPrefs(DefaultIndentPrefs.get(null));
		AplSelection ps = new AplSelection(doc, selection);

		aplBackspace.perform(ps);
	}

}
