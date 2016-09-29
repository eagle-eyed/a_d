package com.dyalog.apldev.debug.core.breakpoints;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.model.IBreakpoint;
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
	 * file is line number 1). The APL VM uses 0-based line number,
	 * so this line number translation is done at breakpoint install time.
	 * 
	 * @param resource file on which to set the breakpoint
	 * @param lineNumber 1-based line number of the breakpoint
	 * @throws CoreException if unable to create the breakpoint
	 */
	public APLLineBreakpoint(final IResource resource, final int lineNumber)
			throws CoreException {
		ICoreRunnable runnable = new ICoreRunnable() {
			@Override
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
		run(getMarkerRule(resource), (IWorkspaceRunnable) runnable);
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
	 * @param target debug target for APL interpreter
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
	 * @param target debug target for Interpreter
	 * @throws CoreException if request creation fails
	 */
	protected synchronized void createRequest(APLDebugTarget target) throws CoreException {
		IResource resource = getMarker().getResource();
		String resName = resource.getName();
		final String[] text;
		if (resource instanceof IFile) {
			int extLen = resource.getFileExtension().length();
			if (extLen > 0)
				extLen ++;
			resName = resName.substring(0, resName.length() - extLen);
			// read file content
			text = ReadFile((IFile) resource);
		} else {
			text = new String[0];
		}
		final String name = resName;
		// Check if function already opened by interpreter
		EntityWindowsStack entityWins = target.getEntityWindows();
		EntityWindow entityWin = entityWins.getEntity(name);
		if (entityWin != null) {
			if (entityWin.addStop(getLineNumber() - 1)) {
				setBreakpointList(target, entityWin, text);
			}
		} else {
			entityWin = entityWins.getEntity(name);
			if (entityWin != null) {
				if (entityWin.addStop(getLineNumber() - 1)) {
					setBreakpointList(target, entityWin, text);
				}
			} else {
				// open window with function
				lineNumber = getLineNumber();
				Runnable addBPonOpen = new Runnable() {
					@Override
					public void run() {
						EntityWindow entity = entityWins.getEntity(name);
						if (entity == null) {
							entity = entityWins.getDebugEntity(name);
						}
						if (entity != null && entity.addStop(lineNumber - 1)) {
							setBreakpointList(target, entity, text);
						}
						
					}
				};
				entityWins.addOnOpenActionWithClose(name, addBPonOpen); 

				JSONArray cmd = new JSONArray();
				cmd.put(0, "Edit");
				JSONObject val = new JSONObject();
				val.put("win", 0);
				val.put("pos", 0);
				val.put("text", name);
				val.put("unsaved", new JSONObject());
				cmd.put(1, val);
//				JSONArray ans = new ReplyRequest(target).get(cmd);
				target.getInterpreterWriter().postCommand(cmd.toString());
			}
		}
	}
	
	private String[] ReadFile(IFile file) throws CoreException {
		try {
			List <String> fileText = new ArrayList <String>();
			InputStream in;
			in = file.getContents();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = reader.readLine()) != null) {
				fileText.add(line);
			}
			String[] text = new String[fileText.size()];
			fileText.toArray(text);
			return text;
		} catch (IOException e) {
			APLDebugCorePlugin.log(e);
			return null;
		}
	}

	/**
	 * Removes this breakpoint's event request from the target. Subclasses
	 * should override.
	 * 
	 * @param target APL interpreter
	 * @throws CoreException if clearing the request fails
	 */
	protected synchronized void clearRequest(APLDebugTarget target) throws CoreException {
		IResource resource = getMarker().getResource();
		String resName = resource.getName();
		final String[] text;
		if (resource instanceof IFile) {
			int extLen = resource.getFileExtension().length();
			if (extLen > 0)
				extLen ++;
			resName = resName.substring(0, resName.length() - extLen);
			// read file content
			text = ReadFile((IFile) resource);
		} else {
			text = new String[0];
		}
		final String name = resName;
		
		// Check if function already opened by interpreter
		EntityWindowsStack entityWins = target.getEntityWindows();
		EntityWindow entityWin = entityWins.getDebugEntity(name);
		if (entityWin != null) {
			if (entityWin.removeStop(getLineNumber() - 1)) {
				setBreakpointList(target, entityWin, text);
			}
		} else {
			entityWin = entityWins.getEntity(name);
			if (entityWin != null) {
				if (entityWin.removeStop(getLineNumber() - 1)) {
					setBreakpointList(target, entityWin, text);
				}
			} else {
				// open window with function
				lineNumber = getLineNumber();
				Runnable removeBPonOpen = new Runnable() {
					@Override
					public void run() {
						EntityWindow entity = entityWins.getEntity(name);
						if (entity == null) {
							entity = entityWins.getDebugEntity(name);
						}
						if (entity != null && entity.removeStop(lineNumber - 1)) {
							setBreakpointList(target, entity, text);
						}
						
					}
				};
				entityWins.addOnOpenActionWithClose(name, removeBPonOpen); 

				JSONArray cmd = new JSONArray();
				cmd.put(0, "Edit");
				JSONObject val = new JSONObject();
				val.put("win", 0);
				val.put("pos", 0);
				val.put("text", name);
				val.put("unsaved", new JSONObject());
				cmd.put(1, val);
//				JSONArray ans = new ReplyRequest(target).get(cmd);
				target.getInterpreterWriter().postCommand(cmd.toString());
			}
		}
	}
	
	private void setBreakpointList(APLDebugTarget target, EntityWindow entityWin, String[] text) {
		if (target != null && entityWin != null) {
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
//				JSONArray cmdSave = new JSONArray();
//				JSONObject valSave = new JSONObject();
//				cmdSave.put(0,"SaveChanges");
//				valSave.put("win", entityWin.token);
//				valSave.put("text", entityWin.getTextAsArray());
//				valSave.put("stop", entityWin.getStop());
//				cmdSave.put(1, valSave);
	//			JSONArray ans = new ReplyRequest(target).get(cmdSave);
				target.getInterpreterWriter().postSave(entityWin.token, text, entityWin.getStop());
			}
		}
	}
	
	/**
	 * Removes this breakpoint from the given interpreter.
	 * Removes this breakpoint as an event listener and clears
	 * the request for the interpreter.
	 * 
	 * @param target APL interpreter
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
