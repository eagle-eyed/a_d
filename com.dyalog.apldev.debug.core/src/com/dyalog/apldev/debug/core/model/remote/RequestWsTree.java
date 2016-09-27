package com.dyalog.apldev.debug.core.model.remote;

import org.json.JSONObject;
/**
 * Interchange between threads. Method get() waits before called put(val), or after 2 seconds returns null
 */
public class RequestWsTree {

	boolean valueSet = false;
	JSONObject val = null;
	DebuggerWriter fWriter;
	private int requestNode;
	
	public RequestWsTree (DebuggerWriter fWriter) {
		this.fWriter = fWriter;
	}

	/**
	 * Get node children
	 * @param node 
	 * @return JSONObject from command ReplyTreeList
	 */
	public synchronized JSONObject get(int node) {
		// send TreeList command to interpreter
		requestNode = node;
		valueSet = false;
		if(!fWriter.postGetTree(node, this))
			return null;

//			while(!valueSet)
			try {
				wait(2000);
			} catch(InterruptedException e) {
				
			}
		if (!valueSet)
			return null;
		valueSet = false;
		return val;
	}
	
	public synchronized void put (JSONObject val) {
		// check if put called by mistake
		if (valueSet)
			return;
//			while (valueSet)
//				try {
//					wait();
//				} catch(InterruptedException e) {
//					
//				}
		this.val = val;
		valueSet = true;
		notify();
	}
	
	public int getRequestNode() {
		return requestNode;
	}
}
