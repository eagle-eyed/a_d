package com.dyalog.apldev.debug.core.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class APLValue extends APLDebugElement implements IValue {
	
	private String fValue;
	private APLVariable fVariable;
	
	public APLValue(APLDebugTarget target, String value, APLVariable variable) {
		super(target);
		fValue = value;
		// parent
		fVariable = variable;
	}

	public String getReferenceTypeName() throws DebugException {
		try {
			Integer.parseInt(fValue);
		} catch (NumberFormatException e) {
			return "text";
		}
		return "integer";
	}

	public String getValueString() throws DebugException {
		return fValue;
	}

	public boolean isAllocated() throws DebugException {
		return true;
	}

	@Override
	public IVariable[] getVariables() throws DebugException {
//		if (fVariable != null && fVariable.getName().equals("c")) {
//			return new IVariable[]
//					{new PDAVariable(fVariable.getStackFrame(), "ca"),
//						new PDAVariable(fVariable.getStackFrame(), "cb")};
//		}
		return new IVariable[0];
	}

	public boolean hasVariables() throws DebugException {
//		return fValue.split("\\W+").length > 1;
		return false;
	}

	public boolean equals(Object obj) {
		return obj instanceof APLValue && ((APLValue) obj).fValue.equals(fValue);
	}
	
	public int hashCode() {
		return fValue.hashCode();
	}
}
