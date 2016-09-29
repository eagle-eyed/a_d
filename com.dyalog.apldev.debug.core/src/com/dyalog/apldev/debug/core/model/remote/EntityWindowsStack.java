package com.dyalog.apldev.debug.core.model.remote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private Map <String, Runnable[]> fOnOpenAction = Collections.synchronizedMap(
			new LinkedHashMap <String, Runnable[]> ());
	private Map <String, Runnable[]> fOnOpenActionWithClose = Collections.synchronizedMap(
			new LinkedHashMap <String, Runnable[]> ());
	
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
		Runnable[] actions = fOnOpenAction.remove(entityWin.name);
		if (actions != null && actions.length > 0) {
			for (Runnable action : actions) {
				action.run();
			}
		}
		actions = fOnOpenActionWithClose.remove(entityWin.name);
		if (actions != null && actions.length > 0) {
			for (Runnable action : actions) {
				action.run();
			}
			debugTarget.getInterpreterWriter().postCloseWindow(entityWin.token);
		}
	}
	
	/**
	 * Add action after opening window
	 */
	public synchronized void addOnOpenAction(String name, Runnable onOpenAction) {
		Runnable[] actions = fOnOpenAction.get(name);
		if (actions != null) {
			Runnable[] moreActions = new Runnable[actions.length + 1];
			System.arraycopy(actions, 0, moreActions, 0, actions.length);
			actions = moreActions;
		} else {
			actions = new Runnable[1];
		}
		actions[actions.length - 1] = onOpenAction;
		fOnOpenAction.put(name, actions);
	}

	/**
	 * Add action after opening window
	 */
	public synchronized void addOnOpenActionWithClose(String name, Runnable onOpenAction) {
		Runnable[] actions = fOnOpenActionWithClose.get(name);
		if (actions != null) {
			Runnable[] moreActions = new Runnable[actions.length + 1];
			System.arraycopy(actions, 0, moreActions, 0, actions.length);
			actions = moreActions;
		} else {
			actions = new Runnable[1];
		}
		actions[actions.length - 1] = onOpenAction;
		fOnOpenActionWithClose.put(name, actions);
	}
	
	/**
	 * Remove entity edit/view window
	 */
	public void remove (int win) {
		try {
			synchronized (EntityWindowsLock) {
				EntityWindow entityWin = fEntityWindows.remove(win);
				if (entityWin.getDebugger()) {
					fDebugWindowNames.remove(entityWin.getName());
					APLThread thread = debugTarget.getThread(entityWin.getThreadId());
					// check if interpreter close window with recursive call function
					boolean checkStackFrame = false;
					if (thread.getStackFramesCount() > 1)
						checkStackFrame = true;
					
					if (thread.getIdentifier() != 0)
						thread.setTerminate();

//					if (checkStackFrame)
//						debugTarget.getInterpreterWriter()
//								.postGetSIStack();
						
				} else {
					fWindowNames.remove(entityWin.getName());
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
				// search in all opened windows
				Set <Integer> set = fEntityWindows.keySet();
				for (int i : set) {
					EntityWindow entityWin = fEntityWindows.get(i);
					if (entityWin.getName().equals(name)
							&& ! entityWin.getDebugger()) {
						return entityWin;
					}
				}
				return null;
			}
			return fEntityWindows.get(win);
		}
	}
	
	public EntityWindow getDebugEntity (String name) {
		synchronized (EntityWindowsLock) {
			Integer win = fDebugWindowNames.get(name);
			if (win == null) {
				// search in all opened windows
				Set <Integer> set = fEntityWindows.keySet();
				for (int i : set) {
					EntityWindow entityWin = fEntityWindows.get(i);
					if (entityWin.getName().equals(name)
							&& entityWin.getDebugger()) {
						return entityWin;
					}
				}
				return null;
			}
			return fEntityWindows.get(win);
		}
	}
}

