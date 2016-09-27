package com.dyalog.apldev.debug.core.model.remote;

import java.util.List;

import org.json.JSONObject;

/**
 * Load or change name content
 */
public class RequestSave {

	private boolean replySave = true;
	JSONObject val = null;
	private DebuggerWriter fWriter;
	private int win;
//	private List<String> text;
	
	public RequestSave (DebuggerWriter fWriter) {
		this.fWriter = fWriter;
	}
	
	public synchronized JSONObject get(int win, List<String> text, int[] lineBreakpoints) {
		this.win = win;
		replySave = false;
		if(!fWriter.postSave(win, text, lineBreakpoints, this))
			return null;
		try {
			wait(2000);
		} catch (InterruptedException e) {
			
		}
		if (!replySave)
			return null;

		return val;
	}

	public synchronized void put (JSONObject val) {
		// check if called by mistake
		if (replySave)
			return;
		this.val = val;
		replySave = true;
		notify();
	}
	
	public int getRequestWin() {
		return win;
	}
	
}
