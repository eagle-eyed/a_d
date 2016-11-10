package com.dyalog.apldev.debug.core.console.ui;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

import com.dyalog.apldev.debug.core.console.IScriptConsoleShell;
import com.dyalog.apldev.debug.core.console.IScriptConsoleViewer;
import com.dyalog.apldev.log.Log;

public class DefaultScriptConsoleTextHover implements ITextHover {

	private IScriptConsoleShell interpreterShell;

	public DefaultScriptConsoleTextHover (IScriptConsoleShell interpreterShell) {
		this.interpreterShell = interpreterShell;
	}
	
	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		return getHoverInfoImpl((IScriptConsoleViewer) textViewer, hoverRegion);
	}

	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		return new Region(offset, 0);
	}
	
	protected String getHoverInfoImpl(IScriptConsoleViewer viewer, IRegion hoverRegion) {
		try {
			IDocument document  = viewer.getDocument();
			int cursorPosition = hoverRegion.getOffset();
			
			return interpreterShell.getDescription(document, cursorPosition);
		} catch (Exception e) {
			Log.log(e);
			return null;
		}
	}
}
