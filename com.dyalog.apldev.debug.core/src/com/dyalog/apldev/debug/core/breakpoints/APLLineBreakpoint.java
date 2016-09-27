package com.dyalog.apldev.debug.core.breakpoints;

import java.util.Iterator;

import org.eclipse.core.internal.resources.File;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.LineBreakpoint;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.model.APLDebugTarget;
import com.dyalog.apldev.debug.core.model.APLThread;
import com.dyalog.apldev.debug.core.model.IAPLEventListener;
import com.dyalog.apldev.debug.core.model.remote.EntityWindow;
import com.dyalog.apldev.debug.core.model.remote.EntityWindowsStack;
import com.dyalog.apldev.debug.core.model.remote.ReplyRequest;

public class APLLineBreakpoint extends LineBreakpoint implements
		IAPLEventListener {

	// target currently installed in
	private APLDebugTarget fDebugTarget;
	/**
	 * Function name
	 */
	private String fName;
	private int lineNumber;
	
	/**
	 * Default constructor is required for the breakpoint manager
	 * to re-create persisted breakpoints. After instantiating a breakpoint,
	 * the <code>setMarker(...)</code> method is called to restore
	 * this breakpoint's attributes.
	 */
	public APLLineBreakpoint() {
		
	}
	
	/**
	 * Constructs a line breakpoint on the given resource at the given
	 * line number. The line number is 1-based (i.e. the first line of a
	 * file is line number 1). The PDA VM uses 0-based line number,
	 * so this line number translation is done at breakpoint install time.
	 * 
	 * @param resource file on which to set the breakpoint
	 * @param lineNumber 1-based line number of the breakpoint
	 * @throws CoreException if unable to create the breakpoint
	 */
	public APLLineBreakpoint(final IResource resource, final int lineNumber)
			throws CoreException {
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IMarker marker = resource.createMarker(
						"example.debug.core.pda.markerType.lineBreakpoint");
				setMarker(marker);
				marker.setAttribute(IBreakpoint.ENABLED, Boolean.TRUE);
				marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
				marker.setAttribute(IBreakpoint.ID, getModelIdentifier());
				marker.setAttribute(IMarker.MESSAGE, "Line Breakpoint: " + resource.getName()
						+ " [line: " + lineNumber + "]");
			}
		};
		run(getMarkerRule(resource), runnable);
		this.lineNumber = lineNumber;
//		name = ;
	}
	
	public String getModelIdentifier() {
		return APLDebugCorePlugin.ID_APL_DEBUG_MODEL;
	}
	
	/**
	 * Returns whether this breakpoint is a run to line breakpoint
	 * 
	 * @return whether this breakpoint is a run to line breakpoint
	 */
	public boolean isRunToLineBreakpoint() {
		return false;
	}

	/**
	 * Installs this breakpoint in the given interpreter.
	 * Registers this breakpoint as an event listener in the
	 * given target and creates the breakpoint specific request.
	 * 
	 * @param target PDA interpreter
	 * @throws CoreException if installation fails
	 */
	public void install(APLDebugTarget debugTarget) throws CoreException {
		fDebugTarget = debugTarget;
		debugTarget.addEventListener(this);
		createRequest(debugTarget);
	}
	
	/**
	 * Create the breakpoint specific request in the target. Subclasses
	 * should override.
	 * 
	 * @param target PDA interpreter
	 * @throws CoreException if request creation fails
	 */
	protected void createRequest(APLDebugTarget target) throws CoreException {
		IResource resource = getMarker().getResource();
		String name = resource.getName();
		if (resource instanceof IFile) {
			int extLen = resource.getFileExtension().length();
			if (extLen > 0)
				extLen ++;
			name = name.substring(0, name.length() - extLen);
		}
		
		// Check if function already opened by interpreter
		EntityWindowsStack entityWins = target.getEntityWindows();
		EntityWindow entityWin = entityWins.getEntity(name);
		if (entityWin == null) {
			entityWin = entityWins.getDebugEntity(name);
		}
		if (entityWin == null) {
			// Open function
			JSONArray cmd = new JSONArray();
			cmd.put(0, "Edit");
			JSONObject val = new JSONObject();
			val.put("win", 0);
			val.put("pos", 0);
			val.put("text", name);
			val.put("unsaved", new JSONObject());
			cmd.put(1, val);
			JSONArray ans = new ReplyRequest(target).get(cmd);
			if (ans == null) {
				// can't open entity window
				return;
			}
			try {
				JSONObject ansVal = ans.getJSONObject(1);
				String ansCmd = ans.getString(0);
				if (ansCmd.equals("OpenWindow") || ansCmd.equals("UpdateWindow")) {
					// Set breakpoints using SaveChanges
					int win = ansVal.getInt("token");
					// get entity window from hash table
					entityWin = target.getEntityWindows().getEntity(win);
					installBreakpoint(target, entityWin, getLineNumber() - 1);
					
					// Close opened window
					cmd = new JSONArray();
					cmd.put(0, "CloseWindow");
					val = new JSONObject();
					val.put("win", entityWin.token);
					cmd.put(1, val);
					ans = new ReplyRequest(target).get(cmd);
//					if (ans != null) {
//						ansVal = ans.getJSONObject(1);
//						int replyWin = ansVal.getInt("win");
//						// Check if window closed correctly
//						if (win != replyWin) {
//
//						}
//					}
				} else if (ansCmd.equals("GotoWindow")) {
					// interpreter window opened, but not stored in map "name - win id"
					int win = ansVal.getInt("win");
					System.out.println("Set line breakpoint: can't obtain win id \""+ win +
							"\" id by entity \"" + name + "\"");
					entityWin = target.getEntityWindows().getEntity(win);
					installBreakpoint(target, entityWin, getLineNumber() -1);
				}
			} catch (JSONException e) {
				
			}
		} else {
			installBreakpoint(target, entityWin, getLineNumber() - 1);
		}
	}
	
	private void installBreakpoint(APLDebugTarget target, EntityWindow entityWin, int line) {
		if (entityWin == null || target == null)
			return;
		// Check if line breakpoint already present
		if (entityWin.addStop(line)) {
			if (entityWin.getDebugger()) {
				// Set using SetLineAttributes
					JSONArray cmd = new JSONArray();
					cmd.put(0, "SetLineAttributes");
					JSONObject val = new JSONObject();
					val.put("win", entityWin.token);
					val.put("nLines", entityWin.getTextAsArray().length);
					val.put("stop", entityWin.getStop());
					val.put("trace", new int[0]);
					val.put("monitor", new int[0]);
					cmd.put(1, val);
					target.getInterpreterWriter().postCommand(cmd.toString());
			} else {
				JSONArray cmdSave = new JSONArray();
				cmdSave.put(0, "SaveChanges");
				JSONObject valSave = new JSONObject();
				valSave.put("win", entityWin.token);
				valSave.put("text", entityWin.getTextAsArray());
				valSave.put("stop", entityWin.getStop());
				cmdSave.put(1, valSave);
				JSONArray ans = new ReplyRequest(target).get(cmdSave);
				if (ans != null) {
					try {
//						JSONObject ansVal = ans.getJSONObject(1);
//						int err = ansVal.getInt("err");
						// TODO Check if saved correctly
					} catch (JSONException e) {
						
					}
				}
			}
		}
	}
	
	/**
	 * Removes this breakpoint's event request from the target. Subclasses
	 * should override.
	 * 
	 * @param target PDA interpreter
	 * @throws CoreException if clearing the request fails
	 */
	protected void clearRequest(APLDebugTarget target) throws CoreException {
		IResource resource = getMarker().getResource();
		String name = resource.getName();
		if (resource instanceof IFile) {
			int extLen = resource.getFileExtension().length();
			if (extLen > 0)
				extLen ++;
			name = name.substring(0, name.length() - extLen);
		}
		// Check if function already opened by interpreter
		EntityWindowsStack entityWins = target.getEntityWindows();
		EntityWindow entityWin = entityWins.getEntity(name);
		if (entityWin == null) {
			entityWin = entityWins.getDebugEntity(name);
		}
		
		if (entityWin == null) {
			// Open function
			JSONArray cmd = new JSONArray();
			cmd.put(0, "Edit");
			JSONObject val = new JSONObject();
			val.put("win", 0);
			val.put("pos", 0);
			val.put("text", name);
			val.put("unsaved", new JSONObject());
			cmd.put(1, val);
			JSONArray ans = new ReplyRequest(target).get(cmd);
			if (ans != null) {
				try {
					JSONObject ansVal = ans.getJSONObject(1);
					String ansCmd = ans.getString(0);
					if (!ansCmd.equals("GotoWindow")) {
						// set line breakpoint using "SaveChanges"
						int win = ansVal.getInt("token");
						entityWin = target.getEntityWindows().getEntity(win);
						if (entityWin != null && entityWin.removeStop(getLineNumber() - 1)) {
							cmd = new JSONArray();
							cmd.put(0, "SaveChanges");
							val = new JSONObject();
							val.put("win", entityWin.token);
							val.put("text", entityWin.getTextAsArray());
							val.put("stop", entityWin.getStop());
							cmd.put(1, val);
							ans = new ReplyRequest(target).get(cmd);
							if (ans != null) {
								try {
									ansVal = ans.getJSONObject(1);
									int err = ansVal.getInt("err");
									// TODO Check if saved correctly, else don't remove line breakpoint
								} catch (JSONException e) {
									
								}
							}
							// Close opened window
							cmd = new JSONArray();
							cmd.put(0, "CloseWindow");
							val = new JSONObject();
							val.put("win", 1);
							cmd.put(1, val);
							ans = new ReplyRequest(target).get(cmd);
							if (ans != null) {
								// Check if window closed correctly
								ansVal = ans.getJSONObject(1);
								int replyWin = ansVal.getInt("win");
								if (win == replyWin) {
									// TODO Handle closed window
								}
							}
						}
					} else {
						// interpreter window opened, but not stored in map "name - win id"
						int win = ansVal.getInt("win");
						System.out.println("Remove line breakpoint: can't obtain win id \""+ win +
								"\" id by entity \"" + name + "\"");
						entityWin = target.getEntityWindows().getEntity(win);
						if (entityWin != null) {
							removeBreakpoint(target, entityWin, getLineNumber() - 1);
						}
					}
				} catch (JSONException e) {

				}
			}
		} else {
			removeBreakpoint(target, entityWin, getLineNumber() - 1);
		}
	}
	
	private void removeBreakpoint(APLDebugTarget target, EntityWindow entityWin, int line) {
		if (entityWin.removeStop(line)) {
			if (entityWin.getDebugger()) {
				// interpreter open debugger window
				JSONArray cmdSet = new JSONArray();
				JSONObject valSet = new JSONObject();
				cmdSet.put(0, "SetLineAttributes");
				valSet.put("win", entityWin.token);
				valSet.put("nLines", entityWin.getTextAsArray().length);
				valSet.put("stop", entityWin.getStop());
				valSet.put("trace", new int[0]);
				valSet.put("monitor", new int[0]);
				cmdSet.put(1, valSet);
				target.getInterpreterWriter().postCommand(cmdSet.toString());
			} else {
				JSONArray cmdSave = new JSONArray();
				JSONObject valSave = new JSONObject();
				cmdSave.put(0,"SaveChanges");
				valSave.put("win", entityWin.token);
				valSave.put("text", entityWin.getTextAsArray());
				valSave.put("stop", entityWin.getStop());
				cmdSave.put(1, valSave);
				JSONArray ans = new ReplyRequest(target).get(cmdSave);
				if (ans != null) {
					try {
//						JSONObject ansVal = ans.getJSONObject(1);
//						int err = ansVal.getInt("err");
						// TODO if error don't remove line breakpoint
					} catch (JSONException e) {
						
					}
				}
			}
		}
	}

	/**
	 * Removes this breakpoint from the given interpreter.
	 * Removes this breakpoint as an event listener and clears
	 * the request for the interpreter.
	 * 
	 * @param target PDA interpreter
	 * @throws CoreException if removal fails
	 */
	public void remove(APLDebugTarget target) throws CoreException {
		target.removeEventListener(this);
		clearRequest(target);
		fDebugTarget = null;
	}
	
	/**
	 * Returns the target this breakpoint is installed in or <code>null</code>.
	 */
	public APLDebugTarget getDebugTarget() {
		return fDebugTarget;
	}
	
	/**
	 * Notify's the APLThread that it was suspended by this breakpoint
	 */
	protected void notifyThread(int threadId) {
		if (fDebugTarget != null) {
			APLThread thread = fDebugTarget.getThread(threadId);
			if (thread != null) {
				thread.suspendedBy(this);
			}
		}
	}
	
	public void handleEvent(String event) {
		if (event.startsWith("suspended breakpoint")) {
			handleHit(event);
		}
	}
	
	/**
	 * Determines if this breakpoint was hit notifies the thread.
	 * 
	 * @param event breakpoint event
	 */
	private void handleHit(String event) {
		int lastSpace = event.lastIndexOf(' ');
		if (lastSpace > 0) {
			String line = event.substring(lastSpace + 1);
			int lineNumber = Integer.parseInt(line);
			// breakpoints event line numbers are 0 based, model objects are 1 based
			lineNumber++;
			try {
				if (getLineNumber() == lineNumber) {
					notifyThread(1);
//					notifyThread(event.fThreadId);
				}
			} catch (CoreException e) {
				
			}
		}
	}
}
