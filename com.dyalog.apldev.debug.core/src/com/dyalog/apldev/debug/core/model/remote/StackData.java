package com.dyalog.apldev.debug.core.model.remote;

public class StackData {
	String[] data;
	int threadId;
	
	public StackData (String[] data, int tid) {
		this.data = data;
		this.threadId = tid;
	}
	
	public int getThreadId() {
		return threadId;
	}
	
	public String[] getData() {
		return data;
	}
}
