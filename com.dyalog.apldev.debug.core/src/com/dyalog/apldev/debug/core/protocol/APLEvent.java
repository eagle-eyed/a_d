package com.dyalog.apldev.debug.core.protocol;

import org.json.JSONArray;

/**
 * Base class for APL events.
 */
public class APLEvent {
	public final JSONArray fMessage;
//	public final String fName;
	
	public APLEvent(JSONArray message) {
		this.fMessage = message;
	}
//	public APLEvent(String message) {
//		fMessage = message;
//		fName = getName(message);
//	}
	
//	protected String getName(String message) {
//		int nameEnd = message.indexOf(' ');
//		nameEnd = nameEnd == -1 ? message.length() : nameEnd;
//		return message.substring(0, nameEnd);
//	}
	
//	public static APLEvent parseEvent(String message) {
//		if (PDAEvalResultEvent.isEventMessage(message)) {
//			return new PDAEvalResultEvent(message);
//		}
//		
//		
//	}
}
