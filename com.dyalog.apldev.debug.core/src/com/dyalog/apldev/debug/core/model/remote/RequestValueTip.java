package com.dyalog.apldev.debug.core.model.remote;

import org.json.JSONObject;

public class RequestValueTip {
	boolean valueSet = false;
	JSONObject val = null;
	DebuggerWriter fWriter;
	private int startCol;
	
	public RequestValueTip (DebuggerWriter fWriter) {
		this.fWriter = fWriter;
	}

	public synchronized JSONObject get(int win, String line, int pos,
			int token, int maxWidth, int maxHeight) {
		// send TreeList command to interpreter
		startCol = pos;
		valueSet = false;
		
		if(!fWriter.postGetValueTip(win, line, pos, token,
				maxWidth, maxHeight, this))
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
	
	public int getRequestStartCol() {
		return startCol;
	}

}
