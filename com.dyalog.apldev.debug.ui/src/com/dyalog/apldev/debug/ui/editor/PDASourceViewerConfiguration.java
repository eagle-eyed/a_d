package com.dyalog.apldev.debug.ui.editor;

import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

/**
 * Source view configuration for the PDA editor
 */
public class PDASourceViewerConfiguration extends TextSourceViewerConfiguration {

	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		return new TextHover();
	}
	
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new AnnotationHover();
	}
}
