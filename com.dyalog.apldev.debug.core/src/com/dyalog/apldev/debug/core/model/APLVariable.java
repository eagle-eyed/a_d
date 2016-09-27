package com.dyalog.apldev.debug.core.model;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class APLVariable extends APLDebugElement implements IVariable {
	
	// name & stack frame
	private String fName;
	private APLStackFrame fFrame;
	private boolean global;

	/**
	 * Constructs a variable contained in the given stack frame
	 * with the given name.
	 * 
	 * @param frame owning stack frame
	 * @param name variable name
	 */
	public APLVariable(APLStackFrame frame, String name) {
		super(frame.getAPLDebugTarget());
		fFrame = frame;
		fName = name;
		global = false;
	}
	
	public APLVariable(APLDebugTarget fDebugTarget, String name) {
		super(fDebugTarget);
		fName = name;
		global = true;
	}

	@Override
	public void setValue(String expression) throws DebugException {
		sendRequest("setvar " + getStackFrame().getIdentifier() + " " + getName() 
				+ " " + expression);
		fireChangeEvent(DebugEvent.CONTENT);
	}

	@Override
	public void setValue(IValue value) throws DebugException {
	
	}

	@Override
	public boolean supportsValueModification() {
		return true;
	}

	@Override
	public boolean verifyValue(String expression) throws DebugException {
		return true;
	}

	@Override
	public boolean verifyValue(IValue value) throws DebugException {
		return false;
	}
	
	/**
	 * Returns the stack frame owning this variable.
	 * 
	 * @return the stack frame owning this variable
	 */
	protected APLStackFrame getStackFrame() {
		return fFrame;
	}

	@Override
	public IValue getValue() throws DebugException {
		IValue value;
		if (global) {
			value = ((APLDebugTarget) getDebugTarget()).getGlobalVariableValue(this);
		} else {
			return new APLValue(this.getAPLDebugTarget(), "local variable (not implemented)", this);
		}
		return value;
	}

	@Override
	public String getName() throws DebugException {
		return fName;
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		return "Thing";
	}

	@Override
	public boolean hasValueChanged() throws DebugException {
//		if (fName.equals("b"))
//			return true;
		return false;
	}

}
