package com.dyalog.apldev.debug.core.protocol;

import org.json.JSONArray;

/**
 * Set current instruction pointer event
 * @author Alex
 *
 */
public class CIPEvent extends APLEvent {
	public final int win;
	public final int line;
	
	public CIPEvent(JSONArray message, int win, int line) {
		super(message);
		this.win = win;
		this.line = line;
	}

}
