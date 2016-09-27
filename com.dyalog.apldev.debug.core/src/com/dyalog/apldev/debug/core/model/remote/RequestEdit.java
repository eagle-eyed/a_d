package com.dyalog.apldev.debug.core.model.remote;

import org.json.JSONObject;

public class RequestEdit {
	private boolean replyEdit = true;
	JSONObject val = null;
	private DebuggerWriter fWriter;
	private String text;
	private int win;
//	private List<String> text;
	
	public RequestEdit (DebuggerWriter fWriter) {
		this.fWriter = fWriter;
	}
	
	public synchronized JSONObject get(int win, int pos, String text) {
		this.win = win;
		this.text = text;
		val = null;
		if(!fWriter.postEdit(win, pos, text, this))
			return null;
		try {
			wait(2000);
		} catch (InterruptedException e) {
			
		}
		
		return val;
	}

	public synchronized void put (JSONObject val) {
		// check if called by mistake
		if (this.val != null)
			return;
		this.val = val;
		replyEdit = true;
		notify();
	}

	public int getRequestWin() {
		return win;
	}
	
	public String getRequestText() {
		return text;
	}
}
