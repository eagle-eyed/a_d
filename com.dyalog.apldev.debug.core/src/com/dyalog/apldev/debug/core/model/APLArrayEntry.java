package com.dyalog.apldev.debug.core.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class APLArrayEntry extends APLDebugElement implements IVariable {
	
	private IValue fValue;
	private int fIndex;

	/**
	 * Constructs a new array entry
	 * 
	 * @param target debug target
	 * @param index index in the array
	 * @param value value of the entry
	 */
	public APLArrayEntry(IDebugTarget target, int index, IValue value) {
		super(target);
		fValue = value;
		fIndex = index;
	}

	public void setValue(String expression) throws DebugException {

	}

	public void setValue(IValue value) throws DebugException {

	}

	public boolean supportsValueModification() {
		return false;
	}

	public boolean verifyValue(String expression) throws DebugException {
		return false;
	}

	public boolean verifyValue(IValue value) throws DebugException {
		return false;
	}

	public IValue getValue() throws DebugException {
		return fValue;
	}

	public String getName() throws DebugException {
		return "[" + fIndex + "]";
	}

	public String getReferenceTypeName() throws DebugException {
		return "String";
	}

	public boolean hasValueChanged() throws DebugException {
		return false;
	}

}
