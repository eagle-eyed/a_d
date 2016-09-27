package com.dyalog.apldev.debug.core.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IPersistableSourceLocator;
import org.eclipse.debug.core.model.IStackFrame;

public class SourceLocator implements IPersistableSourceLocator {

	public Object getSourceElement(IStackFrame stackFrame) {
		return stackFrame;
	}

	public String getMemento() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public void initializeFromMemento(String memento) throws CoreException {
		// TODO Auto-generated method stub

	}

	public void initializeDefaults(ILaunchConfiguration configuration) throws CoreException {
		// TODO Auto-generated method stub

	}

}
