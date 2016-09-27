package com.dyalog.apldev.debug.core.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate;
import org.eclipse.debug.core.model.IValue;

/**
 * Logical structure to translate a string into its words.
 */
public class WordStructureDelegate implements ILogicalStructureTypeDelegate {

	public boolean providesLogicalStructure(IValue value) {
		try {
			String string = value.getValueString();
			String[] words = string.split("\\W+");
			return words.length > 1;
		} catch (DebugException e) {
			
		}
		return false;
	}

	public IValue getLogicalStructure(IValue value) throws CoreException {
		return new APLArray((APLValue) value);
	}

}
