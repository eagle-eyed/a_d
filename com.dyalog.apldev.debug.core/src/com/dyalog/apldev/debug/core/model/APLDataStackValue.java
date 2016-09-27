package com.dyalog.apldev.debug.core.model;

public class APLDataStackValue extends APLValue {

	private int fIndex;
	/**
	 * Class type
	 * -1 invalid name
	 * 0 unused name
	 * 1 Label
	 * 2 Variable
	 * 3 Function
	 * 4 Operator
	 * 9 Object (GUI, namespace, COM, .NET)
	 * 2.1 Variable
	 * 2.2 Field
	 * 2.3 Property
	 * 2.6 External Shared
	 * 3.1 Traditional function
	 * 3.2 dfns
	 * 3.3 Derived Primitive
	 * 3.6 External
	 * 4.1 Traditional Operator
	 * 4.2 dops
	 * 4.3 Derived Privitive
	 * 9.1 Namespace (created ⎕NS, )NS, :Namespace)
	 * 9.2 Instance
	 * 9.4 Class
	 * 9.5 Interface
	 * 9.6 External Class
	 * 9.7 External Interface
	 */
	private int nodeId;
	private int parentNodeId;
	private int fType;
	private APLDebugTarget fDebugTarget;
	
	/**
	 * Constructs a value that appears on the data stack
	 * 
	 * @param target debug target
	 * @param value value on the stack
	 * @param index index on the stack
	 * @param type workspace element class
	 */
	public APLDataStackValue(APLDebugTarget target, String value, int index, int type,
			int nodeId, int parentNodeId) {
		super(target, value, null);
		fDebugTarget = target;
		fIndex = index;
		fType = type;
		this.nodeId = nodeId;
		this.parentNodeId = parentNodeId;
	}
	
	/**
	 * @return WS element class
	 */
	public int getType() {
		return fType;
	}
	
	public int getNodeId() {
		return nodeId;
	}
	
	public boolean equals(Object obj) {
		return super.equals(obj) && ((APLDataStackValue) obj).fIndex == fIndex;
	}
	
	public int hashCode() {
		return super.hashCode() + fIndex;
	}
}
