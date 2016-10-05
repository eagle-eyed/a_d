package com.dyalog.apldev.debug.core.model.remote;

import com.dyalog.apldev.debug.core.model.APLDebugTarget;

public class ReplyStackRequest {
	private APLDebugTarget debugTarget;
	private final int threadId;
	private Boolean ans;

	public ReplyStackRequest(APLDebugTarget debugTarget, int threadId) {
		this.debugTarget =  debugTarget;
		this.threadId = threadId;
	}

	/**
	 * Send command to Interpreter and wait for request, but not longer than 2 seconds
	 */
	public synchronized Boolean get() {
		if (debugTarget == null)
			return null;
		ans = null;
		debugTarget.getInterpreterWriter().postGetSIStack();
		try {
			wait(2000);
		} catch (InterruptedException e) {
			
		}
		return ans;
	}
	
	/**
	 * Set Interpreter response
	 */
	public synchronized void put(Boolean obj) {
		if (ans != null)
			return;
		ans = obj;
		try {
			notify();
		} catch (IllegalMonitorStateException e) {
			
		}
	}

	public int getThreadId () {
		return threadId;
	}
}
