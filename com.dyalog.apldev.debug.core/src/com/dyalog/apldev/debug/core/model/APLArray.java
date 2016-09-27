package com.dyalog.apldev.debug.core.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;

public class APLArray extends APLValue {

	/**
	 * an array splits a value into its words
	 * 
	 * @param value existing value
	 * @throws DebugException
	 */
	public APLArray(APLValue value) throws DebugException {
		super(value.getAPLDebugTarget(), value.getValueString(), null);
	}
	
	public boolean hasVariables() throws DebugException {
		return true;
	}
	
	public IVariable[] getVariables() throws DebugException {
		String string = getValueString();
		String[] words = string.split("\\W+");
		IVariable[] variables = new IVariable[words.length];
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			variables[i] = new APLArrayEntry(getAPLDebugTarget(), i,
					new APLValue(getAPLDebugTarget(), word, null));
		}
		return variables;
	}
}
