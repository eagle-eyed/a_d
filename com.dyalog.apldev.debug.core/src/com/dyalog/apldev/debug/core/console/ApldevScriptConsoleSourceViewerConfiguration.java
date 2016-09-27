package com.dyalog.apldev.debug.core.console;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

/**
 * configuration for the source viewer.
 */
public class ApldevScriptConsoleSourceViewerConfiguration extends SourceViewerConfiguration {

	public static final String PARTITiON_TYPE = IDocument.DEFAULT_CONTENT_TYPE;
	
	private ITextHover hover;
	
	private AplContentAssistant contentAssist;
	
	private IQuickAssistAssistant quickAssist;
	

	public ApldevScriptConsoleSourceViewerConfiguration(ITextHover hover,
			AplContentAssistant contentAssist, IQuickAssistAssistant quickAssist) {
		this.hover = hover;
		this.contentAssist = contentAssist;
		this.quickAssist = quickAssist;
	}
}
