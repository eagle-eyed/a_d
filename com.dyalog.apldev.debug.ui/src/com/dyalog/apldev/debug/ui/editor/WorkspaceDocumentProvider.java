package com.dyalog.apldev.debug.ui.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.editors.text.StorageDocumentProvider;

public class WorkspaceDocumentProvider extends StorageDocumentProvider {

	/** The element's annotation model */
	protected IAnnotationModel fAnnotationModel;
	
	public WorkspaceDocumentProvider() {
		super();
	}
	
	@Override
	protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
		fAnnotationModel = new AnnotationModel();
		return fAnnotationModel;
	}
}
