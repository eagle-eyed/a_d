package com.dyalog.apldev.debug.core.model.remote;

import com.dyalog.apldev.debug.core.model.APLDebugTarget;

public class ReplyEditRequest {

	private APLDebugTarget debugTarget;
	private EntityWindow ans;

	public ReplyEditRequest(APLDebugTarget debugTarget) {
		this.debugTarget =  debugTarget;
	}

	/**
	 * Send command to Interpreter and wait for request, but not longer than 2 seconds
	 */
	public synchronized EntityWindow get(int win, int pos, String text) {
		if (debugTarget == null)
			return null;
		ans = null;
		debugTarget.getInterpreterWriter().postEdit(win, pos, text);
		try {
			wait(2000);
		} catch (InterruptedException e) {
			
		}
		return ans;
	}
	
	/**
	 * Set Interpreter response
	 */
	public synchronized void put(EntityWindow obj) {
		if (ans != null)
			return;
		ans = obj;
		try {
			notify();
		} catch (IllegalMonitorStateException e) {
			
		}
	}

}
