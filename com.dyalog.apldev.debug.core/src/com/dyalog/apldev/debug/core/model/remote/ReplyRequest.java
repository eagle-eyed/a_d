package com.dyalog.apldev.debug.core.model.remote;

import org.eclipse.debug.core.model.IDebugTarget;
import org.json.JSONArray;

import com.dyalog.apldev.debug.core.model.APLDebugTarget;

public class ReplyRequest {
	JSONArray ans;
	private APLDebugTarget fDebugTarget;
	private String cmdName;
	
	public ReplyRequest(IDebugTarget debugTarget) {
		this.fDebugTarget = (APLDebugTarget) debugTarget;
	}
	
	/**
	 * Send command to Interpreter and wait for request, but not longer than 2 seconds
	 */
	public synchronized JSONArray get(JSONArray cmd) {
		if (fDebugTarget == null)
			return null;
		ans = null;
		cmdName = cmd.getString(0);
		try {
			fDebugTarget.getInterpreterWriter().postCommand(cmd.toString(), this);
		} catch (Exception e) {
			return null;
		}
		try {
			wait(2000);
		} catch (InterruptedException e) {
			
		}
		if (ans == null)
			fDebugTarget.getCommandProc().removeReplyHandler(this);
		return ans;
	}
	
	/**
	 * Set Interpreter response
	 */
	public synchronized void put(JSONArray cmdJ) {
		if (ans != null)
			return;
		ans = cmdJ;
		try {
			notify();
		} catch (IllegalMonitorStateException e) {
			
		}
	}
	
	/**
	 * @return command name which wait for request
	 */
	public String getCmdName() {
		return cmdName;
	}
}
