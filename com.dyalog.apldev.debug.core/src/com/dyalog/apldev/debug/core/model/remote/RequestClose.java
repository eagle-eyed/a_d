package com.dyalog.apldev.debug.core.model.remote;

public class RequestClose {

	private DebuggerWriter fWriter;
	private boolean replyClose;
	private int win;

	public RequestClose (DebuggerWriter fWriter) {
		this.fWriter = fWriter;
	}
	
	public synchronized boolean get(int win) {
		this.win = win;
		replyClose = false;
		if(!fWriter.postClose(win, this))
			return false;
		try {
			wait(2000);
		} catch (InterruptedException e) {
			
		}
		if (!replyClose)
			return false;

		return true;
	}

	public synchronized void put () {
		// check if called by mistake
		if (replyClose)
			return;
		replyClose = true;
		notify();
	}
	
	public int getRequestWin() {
		return win;
	}

}
