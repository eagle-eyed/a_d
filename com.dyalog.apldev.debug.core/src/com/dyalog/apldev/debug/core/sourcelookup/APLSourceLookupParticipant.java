package com.dyalog.apldev.debug.core.sourcelookup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;

import com.dyalog.apldev.debug.core.model.APLStackFrame;

public class APLSourceLookupParticipant extends AbstractSourceLookupParticipant {

	public String getSourceName(Object object) throws CoreException {
		if (object instanceof APLStackFrame) {
			return ((APLStackFrame) object).getSourceName();
		}
		return null;
	}

}
