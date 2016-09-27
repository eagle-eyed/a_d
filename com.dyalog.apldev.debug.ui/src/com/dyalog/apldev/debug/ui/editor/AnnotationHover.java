package com.dyalog.apldev.debug.ui.editor;

import java.util.Iterator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;

/**
 * Returns hover for breakpoints.
 */
public class AnnotationHover implements IAnnotationHover {

	public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
		IAnnotationModel annotationModel = sourceViewer.getAnnotationModel();
		Iterator<Annotation> iterator = annotationModel.getAnnotationIterator();
		while (iterator.hasNext()) {
			Annotation annotation = (Annotation) iterator.next();
			Position position = annotationModel.getPosition(annotation);
			if (position == null) {
				return null;
			}
			try {
				int lineOfAnnotation = sourceViewer.getDocument()
						.getLineOfOffset(position.getOffset());
				if (lineNumber == lineOfAnnotation) {
					return annotation.getText();
				}
			} catch (BadLocationException e) {
				
			}
		}
		return null;
	}

}
