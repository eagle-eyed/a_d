package com.dyalog.apldev.debug.core.sourcelookup;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

import com.dyalog.apldev.debug.core.model.APLDebugTarget;
import com.dyalog.apldev.debug.core.model.APLStackFrame;
import com.dyalog.apldev.debug.core.model.remote.EntityWindow;
import com.dyalog.apldev.debug.core.model.remote.WorkspaceEditorInput;

/**
 * APL source lookup director. For APL source lookup there is one source
 * lookup participant.
 */
public class APLSourceLookupDirector extends AbstractSourceLookupDirector {

	@Override
	public void initializeParticipants() {
		addParticipants(new ISourceLookupParticipant[] {new APLSourceLookupParticipant()});
	}

	@Override
	public Object getSourceElement(Object element) {
		Object source = super.getSourceElement(element);
		if (source == null && element instanceof APLStackFrame) {
			APLStackFrame stackFrame = (APLStackFrame) element;
			APLDebugTarget target = (APLDebugTarget) stackFrame.getDebugTarget();
			String funName = stackFrame.getFunctionName();
//			EntityWindow entityWin = target.getEntityWindows().getEntity(funName);
			EntityWindow entityWin = target.getEntityWindows()
					.getDebugEntity(stackFrame.getFunctionName(),
							stackFrame.getThread().getIdentifier());
			if (entityWin != null) {
				source = new WorkspaceEditorInput(entityWin, target);
//				source = new IStorage() {
//					String name = new String(entityWin.name);
//
//					@Override
//					public InputStream getContents() throws CoreException {
//						return new ByteArrayInputStream(entityWin.getTextAsSingleStr()
//								.getBytes(StandardCharsets.UTF_8));
//					}
//
//					@Override
//					public IPath getFullPath() {
//						return null;
//					}
//
//					@Override
//					public String getName() {
//						return name;
//					}
//
//					@Override
//					public boolean isReadOnly() {
//						return entityWin.isReadOnly();
//					}
//					
//					@Override
//					public <T> T getAdapter(Class <T> adapter) {
//						return null;
//					}
//				};
			}
		}
		return source;
	}
}
