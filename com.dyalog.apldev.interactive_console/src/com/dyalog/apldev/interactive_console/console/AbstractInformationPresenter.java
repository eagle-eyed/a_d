package com.dyalog.apldev.interactive_console.console;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.widgets.Display;
/**
 * Base implementation for an information presenter.
 */
public class AbstractInformationPresenter
implements
	DefaultInformationControl.IInformationPresenter, 
//	DefaultInformationControl.IInfrormationPresenterExtension,
	InformationPresenterAsTooltip {

	@Override
	public String updatePresentation(Display display, String hoverInfo, TextPresentation presentation, int maxWidth,
			int maxHeight) {
		// TODO Auto-generated method stub
		return null;
	}

}
