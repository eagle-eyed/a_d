package com.dyalog.apldev.debug.core.protocol;

import org.json.JSONArray;

public class FocusEvent extends APLEvent {
	public final int tid;
	
	public FocusEvent(JSONArray message, int tid) {
		super(message);
		this.tid = tid;
	}

}
