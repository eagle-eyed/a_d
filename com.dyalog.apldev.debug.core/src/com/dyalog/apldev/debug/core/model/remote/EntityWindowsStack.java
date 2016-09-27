package com.dyalog.apldev.debug.core.model.remote;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.dyalog.apldev.debug.core.model.APLDebugTarget;
import com.dyalog.apldev.debug.core.model.APLThread;

public class EntityWindowsStack {
	/**
	 * Opened entity windows token and EntityWindow pairs
	 */
	private Map <Integer, EntityWindow> fEntityWindows = Collections.synchronizedMap(
			new LinkedHashMap <Integer, EntityWindow> ());
	/**
	 * Opened entity windows name and token pairs
	 */
	private Map <String, Integer> fWindowNames = Collections.synchronizedMap(
			new LinkedHashMap <String, Integer> ());
	/**
	 * Opened debug entity windows 
	 */
	private Map <String, Integer> fDebugWindowNames = Collections.synchronizedMap(
			new LinkedHashMap <String, Integer> ());

	/**
	 * Lock for suppressing access during modification fEntityWindows and fWindowsNames
	 */
	private Object EntityWindowsLock = new Object();
	private APLDebugTarget debugTarget;
	
	public EntityWindowsStack(APLDebugTarget aplDebugTarget) {
		this.debugTarget = aplDebugTarget;
	}

	/**
	 * Add entity edit/view window
	 */
	public void addEntityWindow(int token, EntityWindow entityWin) {
		synchronized (EntityWindowsLock) {
			fEntityWindows.put(token, entityWin);
			if (entityWin.getDebugger()) {
				fDebugWindowNames.put(entityWin.name, token);
			} else {
				fWindowNames.put(entityWin.name, token);
			}
		}
		debugTarget.updateThread(entityWin.getThreadId(), entityWin.getThreadName());
	}
	
	/**
	 * Remove entity edit/view window
	 */
	public void remove (int win) {
		try {
			synchronized (EntityWindowsLock) {
				EntityWindow entityWin = fEntityWindows.remove(win);
				fWindowNames.remove(entityWin.name);
				if (entityWin.getDebugger()) {
					APLThread thread = debugTarget.getThread(entityWin.getThreadId());
					// check if interpreter close window with recursive call function
					boolean checkStackFrame = false;
					if (thread.getStackFramesCount() > 1)
						checkStackFrame = true;
					
					if (thread.getIdentifier() != 0)
						thread.setTerminate();

					if (checkStackFrame)
						debugTarget.getInterpreterWriter()
								.postCommand("[\"GetSIStack\",{}]");
						
				}
			}
		} catch (NullPointerException e) {
			
		}
	}

	/**
	 * Remove entity edit/view window
	 */
	public void remove (String name) {
		try {
			synchronized (EntityWindowsLock) {
				int win = fWindowNames.remove(name);
				fEntityWindows.remove(win);
			}
		} catch (NullPointerException e) {
			
		}
	}
	
	public EntityWindow getEntity (int win) {
		synchronized (EntityWindowsLock) {
			return fEntityWindows.get(win);
		}
	}
	
	public Integer getToken (String name) {
		synchronized (EntityWindowsLock) {
			return fWindowNames.get(name);
		}
	}
	
	public EntityWindow getEntity (String name) {
		synchronized (EntityWindowsLock) {
			Integer win = fWindowNames.get(name);
			if (win == null) {
				return null;
			}
			return fEntityWindows.get(win);
		}
	}
	
	public EntityWindow getDebugEntity (String name) {
		synchronized (EntityWindowsLock) {
			Integer win = fDebugWindowNames.get(name);
			if (win == null) {
				return null;
			}
			return fEntityWindows.get(win);
		}
	}
}

