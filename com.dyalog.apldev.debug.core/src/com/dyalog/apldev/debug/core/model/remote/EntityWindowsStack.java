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
//	/**
//	 * Opened entity windows name and token pairs
//	 */
//	private Map <String, Integer> fWindowNames = Collections.synchronizedMap(
//			new LinkedHashMap <String, Integer> ());
//	/**
//	 * Opened debug entity windows 
//	 */
//	private Map <String, Integer> fDebugWindowNames = Collections.synchronizedMap(
//			new LinkedHashMap <String, Integer> ());

	/**
	 * Lock for suppressing access during modification fEntityWindows and fWindowsNames
	 */
	private Object EntityWindowsLock = new Object();
	private APLDebugTarget debugTarget;
	private Map <String, Runnable[]> fOnOpenAction = Collections.synchronizedMap(
			new LinkedHashMap <String, Runnable[]> ());
	private Map <String, Runnable[]> fOnOpenActionWithClose = Collections.synchronizedMap(
			new LinkedHashMap <String, Runnable[]> ());
	private List<String> functionWaitsForOpen = Collections.synchronizedList(
			new ArrayList <String> ());

	public EntityWindowsStack(APLDebugTarget aplDebugTarget) {
		this.debugTarget = aplDebugTarget;
	}

	/**
	 * Add entity edit/view window
	 * 
	 * @param token window ID
	 * @param entityWin new EntityWindow
	 * @param update if that's change opened window
	 */
	public void addEntityWindow(int token, EntityWindow entityWin, boolean update) {
		synchronized (EntityWindowsLock) {
			functionWaitsForOpen.remove(entityWin.name);
			EntityWindow oldEntity = fEntityWindows.remove(entityWin.token);
			if ( ! update && oldEntity != null && ! oldEntity.isClosed()) {
					System.out.println("Closed window not removed from WindowsStack");
			}
			fEntityWindows.put(token, entityWin);
//			if (entityWin.isDebug()) {
//				fDebugWindowNames.put(entityWin.name, token);
//			} else {
//				fWindowNames.put(entityWin.name, token);
//			}
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

	public void GotoWindowAction(int win) {
		EntityWindow entity = getEntity(win);
		if ( ! entity.isTracer()) {
			Runnable[] actions = fOnOpenAction.remove(entity.name);
			if (actions != null && actions.length > 0) {
				for (Runnable action : actions) {
					action.run();
				}
			}
			actions = fOnOpenActionWithClose.remove(entity.name);
			if (actions != null && actions.length > 0) {
				for (Runnable action : actions) {
					action.run();
				}
				debugTarget.getInterpreterWriter().postCloseWindow(entity.token);
			}
		}
	}

	/**
	 * Add action after opening window
	 */
	public synchronized void addOnOpenAction(String name, Runnable onOpenAction) {
		synchronized (fOnOpenAction) {
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
	}

	/**
	 * Add action after opening window
	 */
	public synchronized void addOnOpenActionWithClose(String name, Runnable onOpenAction) {
		synchronized (fOnOpenActionWithClose) {
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
	}

	/**
	 * Return if edit name already called
	 */
	public boolean isOpening(String name) {
		synchronized (EntityWindowsLock) {
			return functionWaitsForOpen.contains(name);
		}
	}
	
	public void addEditname(String name) {
		synchronized (EntityWindowsLock) {
			if ( ! functionWaitsForOpen.contains(name)) {
				functionWaitsForOpen.add(name);
			}
		}
	}
	
	/**
	 * Remove entity edit/view window
	 */
	public void remove (int win) {
		try {
			synchronized (EntityWindowsLock) {
				EntityWindow entityWin = fEntityWindows.remove(win);
				if (entityWin.isDebug()) {
//					fDebugWindowNames.remove(entityWin.getName());
					APLThread thread = debugTarget.getThread(entityWin.getThreadId());
					// check if interpreter close window with recursive call function
//					boolean checkStackFrame = false;
//					if (thread.getStackFramesCount() > 1)
//						checkStackFrame = true;
					
					if (thread.getIdentifier() == 0) {
						thread.resumeThread();
					} else {
						thread.setTerminate();
					}

//					if (checkStackFrame)
//						debugTarget.getInterpreterWriter()
//								.postGetSIStack();
						
				} else {
//					fWindowNames.remove(entityWin.getName());
				}
			}
		} catch (NullPointerException e) {
			
		}
	}

//	/**
//	 * Remove entity edit/view window
//	 */
//	public void remove (String name) {
//		try {
//			synchronized (EntityWindowsLock) {
//				int win = fWindowNames.remove(name);
//				fEntityWindows.remove(win);
//			}
//		} catch (NullPointerException e) {
//			
//		}
//	}
	
	public EntityWindow getEntity (int win) {
		synchronized (EntityWindowsLock) {
			return fEntityWindows.get(win);
		}
	}
	
//	public Integer getToken (String name) {
//		synchronized (EntityWindowsLock) {
//			return fWindowNames.get(name);
//		}
//	}
	
	/**
	 * Return opened non debug or without tracer entity window with specified name
	 */
	public EntityWindow getEntity (String name) {
		synchronized (EntityWindowsLock) {
//			Integer win = fWindowNames.get(name);
//			if (win == null) {
				// search in all opened windows
				Set <Integer> set = fEntityWindows.keySet();
				for (int i : set) {
					EntityWindow entityWin = fEntityWindows.get(i);
					if (entityWin.getName().equals(name)
							&& ( ! entityWin.isDebug() || ! entityWin.isTracer())) {
						return entityWin;
					}
				}
				return null;
			}
//			return fEntityWindows.get(win);
//		}
	}
	
	public EntityWindow getDebugEntity (String name, int threadId) {
		synchronized (EntityWindowsLock) {
//			Integer win = fDebugWindowNames.get(name);
//			if (win == null) {
				// search in all opened windows
				Set <Integer> set = fEntityWindows.keySet();
				for (int i : set) {
					EntityWindow entityWin = fEntityWindows.get(i);
					if (entityWin.getName().equals(name)
							&& entityWin.isDebug()
							&& (threadId == -1 || threadId == entityWin.getThreadId())) {
						return entityWin;
					}
				}
				return null;
			}
//			return fEntityWindows.get(win);
//		}
	}

	/**
	 * Get entity by thread id number
	 */
	public EntityWindow getThreadEntity(int threadId) {
		synchronized (fEntityWindows) {
			Set <Integer> set = fEntityWindows.keySet();
			for (int i : set) {
				EntityWindow entityWin = fEntityWindows.get(i);
				if (entityWin.isDebug() && entityWin.getThreadId() == threadId) {
					return entityWin;
				}
			}
		}
		return null;
	}

	/**
	 * Change debug window tracer state
	 */
	public void changeTracer(int chgWin, int tracer) {
		synchronized (fEntityWindows) {
			EntityWindow entity = getEntity(chgWin);
			entity.setTracer(tracer);
		}
	}

}

