package com.dyalog.apldev.debug.core.console;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Shell;

public class AplContentAssistant extends DefaultContentAssist {

	/**
	 * Shows the completions available and sets the lastAutoActivated flag
	 * and updates the lastActivationCount.
	 */
	@Override
	public String showPossibleCompletions() {
		try {
			return super.showPossibleCompletions();
		} catch (RuntimeException e) {
			Throwable e1 = e;
			while (e1.getCause() != null) {
				e1 = e1.getCause();
			}
			throw e;
		}
	}
	
	public static IInformationControlCreator createInformationControlCreator(ISourceViewer sourceViewer) {
		return new IInformationControlCreator() {
			@Override
			public IInformationControl createInformationControl(Shell parent) {
//				return new DefaultInformationControl(parent, new AplInformationPresenter());
				return null;
			}
		};
	}
}
