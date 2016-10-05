package com.dyalog.apldev.debug.core.model;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

//import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.model.remote.EntityWindow;
import com.dyalog.apldev.debug.core.model.remote.EntityWindowsStack;

/**
 * A PDA thread. A PDA VM is single threaded.
 */
public class APLThread extends APLDebugElement implements IThread,
		IAPLEventListener {
	
	/**
	 * ID of this thread as reported by APL
	 */
	private final int fThreadId;
	
	/**
	 * Breakpoint this thread is suspended at or <code>null</code>
	 * if none.
	 */
	private IBreakpoint fBreakpoint;
	
	/**
	 * Whether this thread is stepping
	 */
	private boolean fStepping = false;
	
	/**
	 * Whether this thread is suspended
	 */
	private boolean fSuspended = false;

	/**
	 * Most recent error event or <code>null</code>
	 */
	private String fErrorEvent;
	
	/**
	 * Table mapping stack frames to current variables;
	 */
	private Map<IStackFrame,IVariable[]> fVariables 
		= new HashMap<IStackFrame, IVariable[]>();
	// --
//	private final String name;
	public String name;
//	private final PDAThreadStack stack;
//	private IStackFrame[] theFrames;
	private APLStackFrame[] theFrames;
//	private final static IStackFrame[] NO_FRAMES = new IStackFrame[0];
	// Reduce number of request to interpreter for retrieving StackFrame
	private boolean fUpdate = false;
	// delay for communication
//	private int fStarting;
//	private int finTSF;
	// indicates that was pressed stepOver
	private int fStepOver = -1;

	/**
	 * Set top stack entity window (set by SetHighLightLine command)
	 */
	private int topStackWindowId;

	
	
	
	/**
	 * Constructs a new thread for the given target
	 * 
	 * @param target VM
	 * @throws DebugException 
	 */
	public APLThread(APLDebugTarget target, int threadId, String tname) {
		super(target);
		this.fThreadId = threadId;
//		getPDADebugTarget().addEventListener(this);
		
		this.name = tname;
	}
	
	/**
	 * Called by the debug target after the thread is created.
	 */
	void start() {
		// create "Thread" DebugElement
		fireCreationEvent();
		getAPLDebugTarget().addEventListener(this);
	}
//	public void setUpdateFrame() {
//		fUpdate = true;
//	}
	/**
	 * Called by the debug target before the thread is removed
	 */
	void exit() {
		getAPLDebugTarget().removeEventListener(this);
		fireTerminateEvent();
	}

	// For dropFrame and stepReturn to selected frame
	public int getStackFramesCount() {
		return theFrames.length;
	}
	
	public APLStackFrame getStackFrame(int Id) {
		if (Id < theFrames.length && theFrames[theFrames.length -1 - Id].getIdentifier() == Id) {
			return theFrames[theFrames.length-1-Id];
		}
		return null;
	}
	
	public IStackFrame[] getStackFrames() throws DebugException {

//		if (fUpdate) update();
		if (isSuspended())
			return theFrames != null ? theFrames : new IStackFrame[0];
		else
			return new IStackFrame[0];
	}
	
	public void update() throws DebugException {
		if (isSuspended() && fUpdate) {
			fUpdate = false;
			((APLDebugTarget) getDebugTarget()).setNeedUpdateGlobalVariables();
		}
	}
	
	public IStackFrame getTopStackFrame() throws DebugException {
		if (!isSuspended())
			return null;

//		if (fUpdate) update();
//		IStackFrame[] frames = getStackFrames();
//		if (fUpdate) update();
		IStackFrame[] frames = theFrames;
		if (frames != null && frames.length > 0) {
			return frames[0];
		}
		return null;
	}

	@Override
	public boolean canResume() {
		return isSuspended();
	}

	@Override
	public boolean canSuspend() {
		return !isSuspended();
	}

	@Override
	public boolean isSuspended() {
		return fSuspended && !isTerminated();
	}

	@Override
	public void resume() throws DebugException {
		APLStackFrame frame = (APLStackFrame) getTopStackFrame();
		EntityWindow entity = null;
		if (frame != null) {
			EntityWindowsStack entityWins = ((APLDebugTarget) getDebugTarget()).getEntityWindows();
			entity = entityWins.getDebugEntity(frame.getFunctionName(), fThreadId);
		}
		if (entity != null && entity.isDebug()) {
			JSONArray cmd = new JSONArray();
			cmd.put(0, "Continue");
			JSONObject val = new JSONObject();
			val.put("win", entity.token);
			cmd.put(1, val);
			APLDebugTarget target = (APLDebugTarget) getDebugTarget();
			target.getInterpreterWriter()
				.postCommand(cmd.toString());
			resumed(DebugEvent.CLIENT_REQUEST);
//			APLStackFrame[] frames = (APLStackFrame[]) getStackFrames();
			for (APLStackFrame sFrame : theFrames) {
				sFrame.terminateStack();
			}
			theFrames = new APLStackFrame[0];
		}
	}

	@Override
	public void suspend() throws DebugException {
		((APLDebugTarget) getDebugTarget()).getInterpreterWriter()
			.postCommand("[\"WeakInterrupt\",{}]");
	}

	@Override
	public boolean canStepInto() {
		return isSuspended();
	}

	@Override
	public boolean canStepOver() {
		return isSuspended();
//		return false;
	}

	@Override
	public boolean canStepReturn() {
		return isSuspended();
	}
	
	@Override
	public boolean isStepping() {
		return fStepping;
	}

	@Override
	public void stepInto() throws DebugException {
		APLStackFrame frame = (APLStackFrame) getTopStackFrame();
		EntityWindow entity = null;
		if (frame != null) {
			EntityWindowsStack entityWins = ((APLDebugTarget) getDebugTarget()).getEntityWindows();
			entity = entityWins.getDebugEntity(frame.getFunctionName(), fThreadId);
		}
		if (entity != null && entity.isDebug()) {
//			JSONArray cmd = new JSONArray();
//			cmd.put(0, "StepInto");
//			JSONObject val = new JSONObject();
//			val.put("win", entity.token);
//			cmd.put(1, val);
			((APLDebugTarget) getDebugTarget()).getInterpreterWriter()
				.postStepInto(entity.token);
//				.postCommand(cmd.toString());
			// Check if that is last function command 
			if (entity.getLineNumber() == 0) {
				// if window can close call event which need update stack frame
				resumed(DebugEvent.CLIENT_REQUEST);
			} else
				resumed(DebugEvent.STEP_INTO);
			for (APLStackFrame sframe : theFrames) {
				sframe.fireTerminateEvent();
			}
			theFrames = new APLStackFrame[0];
		}
	}

	@Override
	public void stepOver() throws DebugException {
		APLStackFrame frame = (APLStackFrame) getTopStackFrame();
		EntityWindow entity = null;
		if (frame != null) {
			EntityWindowsStack entityWins = ((APLDebugTarget) getDebugTarget()).getEntityWindows();
			entity = entityWins.getDebugEntity(frame.getFunctionName(), fThreadId);
		}
		if (entity != null && entity.isDebug()) {
//			JSONArray cmd = new JSONArray();
//			cmd.put(0, "RunCurrentLine");
//			JSONObject val = new JSONObject();
//			val.put("win", entity.token);
//			cmd.put(1, val);
//			APLDebugTarget target = (APLDebugTarget) getDebugTarget(); 
//			target.getInterpreterWriter().postCommand(cmd.toString());
			((APLDebugTarget) getDebugTarget()).getInterpreterWriter()
				.postStepOver(entity.token);
			// If it's last function command don't remember stack frame
			if (entity.getLineNumber() == 0) {
				resumed(DebugEvent.CLIENT_REQUEST);
			} else {
				resumed(DebugEvent.STEP_OVER);
			}
		}
	}

	@Override
	public void stepReturn() throws DebugException {
		// step return when thread selected
		APLStackFrame frame = (APLStackFrame) getTopStackFrame();
		EntityWindow entity = null;
		if (frame != null) {
			EntityWindowsStack entityWins = ((APLDebugTarget) getDebugTarget()).getEntityWindows();
			entity = entityWins.getDebugEntity(frame.getFunctionName(), fThreadId);
		}
		if (entity != null && entity.isDebug()) {

//		JSONArray cmd = new JSONArray();
//		cmd.put(0, "ContinueTrace");
//		JSONObject val = new JSONObject();
//		val.put("win", entity.token);
//		cmd.put(1, val);
		((APLDebugTarget) getDebugTarget()).getInterpreterWriter()
			.postStepReturn(entity.token);
//			.postCommand(cmd.toString());
		}	
	}

	/**
	 * Used for stepReturn to selected stack frame
	 * @param Id
	 * @throws DebugException
	 */
	public void stepReturn(int Id) throws DebugException {
		// stepreturn to selected frame 
		theFrames[theFrames.length - Id - 1].stepReturn();
	}

	public void dropToFrame() {
		APLStackFrame frame;
		try {
			frame = (APLStackFrame) getTopStackFrame();
		} catch (DebugException e) {
			APLDebugCorePlugin.log(e);
			return;
		}
		if (frame != null) {
			
			EntityWindowsStack entityWins = ((APLDebugTarget) getDebugTarget()).getEntityWindows();
			EntityWindow entity = entityWins.getDebugEntity(frame.getFunctionName(), fThreadId);
			if (entity != null && entity.isDebug()) {
				((APLDebugTarget) getDebugTarget()).getInterpreterWriter()
					.postCutback(entity.token);
//					.postCommand("[\"Cutback\",{\"win\":" + topStackWindowId + "}]");
//				fireChangeEvent(DebugEvent.RESUME);
//				frame.Changed();
				resumed(DebugEvent.CLIENT_REQUEST);
			}
		}
	}
	
	@Override
	public boolean canTerminate() {
//		if (fThreadId == 0)
//			return false;
		return !isTerminated();
	}

	@Override
	public boolean isTerminated() {
		return getDebugTarget().isTerminated();
	}

	@Override
	public void terminate() throws DebugException {
		if (fThreadId == 0) {
			getDebugTarget().terminate();
		} else {
//			sendRequest("terminate thread " + fThreadId);
		}
		
	}
	
	public void setTerminate() {
		((APLDebugTarget) getDebugTarget()).RemoveThread(fThreadId);
		fireTerminateEvent();
	}
	
	/**
	 * Sets whether this thread is stepping
	 * 
	 * @param stepping whether stepping
	 */
	private void setStepping(boolean stepping) {
		fStepping = stepping;
	}
	
	/**
	 * Sets whether this thread is supended
	 * 
	 * @param suspended whether suspended
	 * @throws DebugException 
	 */
	private void setSuspended(boolean suspended) {
		fSuspended = suspended;
	}
	
	/**
	 * Sets the most recent error event encountered, or <code>null</code>
	 * to clear the most recent error
	 * 
	 * @param event error description or <code>null</code>
	 */
	private void setError(String event) {
		fErrorEvent = event;
	}
	
	/**
	 * Returns the most recent error event encountered since the last
	 * suspend, or <code>null</code> if none.
	 * 
	 * @return the most recent error event encountered since the last
	 * suspend, or <code>null</code> if none
	 */
	public Object getError() {
		return fErrorEvent;
	}

	public void handleEvent(String event) {
		// clear previous state
		fBreakpoint = null;
		setStepping(false);
		
		// handle events
		if (event.startsWith("resumed")) {
			setSuspended(false);
			if (event.endsWith("step")) {
				setStepping(true);
				if (fStepOver > 0) 
					resumed(DebugEvent.STEP_OVER);
				else
					resumed(DebugEvent.STEP_INTO);
			} else if (event.endsWith("client")) {
				resumed(DebugEvent.CLIENT_REQUEST);
			} else if (event.endsWith("drop")) {
				resumed(DebugEvent.STEP_RETURN);
			}
		} else if (event.startsWith("suspended")) {
			setSuspended(true);
			fUpdate = true;
//			try {update();} catch (DebugException e) {}
//			fireCreationEvent();
//			System.out.print("H");
			
			if (event.endsWith("client")) {
				suspended(DebugEvent.CLIENT_REQUEST);
			} else if (event.endsWith("step")) {
//				suspended(DebugEvent.STEP_END);
				suspended(DebugEvent.STEP_INTO);
			} else if (event.startsWith("suspended event") && getError() != null) {
				exceptionHit();
			} else if (event.endsWith("drop")) {
				suspended(DebugEvent.STEP_END);
			} else if (event.startsWith("suspended breakpoint")) {
				suspended(DebugEvent.BREAKPOINT);
//				System.out.print("B");
			}
		} else if (event.equals("started")) {
//			fireCreationEvent();
//			System.out.print("!");
		} else {
//			setError(event);
		}
	}
	
	/**
	 * Notification the target has resumed for the given reason.
	 * Clears any error condition that was last encountered and
	 * fires a resume event, and clears all cached variables
	 * for stack frames.
	 * 
	 * @param detail reason for the resume
	 */
	private void resumed(int detail) {
		setError(null);
		synchronized (fVariables) {
			fVariables.clear();
		}
		fireResumeEvent(detail);
		setSuspended(false);
	}
	
	/**
	 * Notification the target has suspended for the given reason
	 * 
	 * @param detail reason for the suspend
	 */
	private void suspended(int detail) {
		fireSuspendEvent(detail);
		setSuspended(true);
	}
	
	/**
	 * Notification an error was encountered.Fires a breakpoint
	 * suspend event.
	 */
	private void exceptionHit() {
		suspended(DebugEvent.BREAKPOINT);
	}
	
	/**
	 * Sets the current variables for the given stack frame. Called
	 * by PDA stack frame when it is created.
	 * 
	 * @param frame
	 * @param variables
	 */
	protected void setVariables(IStackFrame frame, IVariable[] variables) {
		synchronized (fVariables) {
			fVariables.put(frame,  variables);
		}
	}
	
	/**
	 * Returns the current variables for the given stack frame, or
	 * <code>null</code> if none.
	 * 
	 * @param frame stack frame
	 * @return variables or <code>null</code>
	 */
	@SuppressWarnings("unused")
	protected IVariable[] getVariables(IStackFrame frame) {
		synchronized (fVariables) {
//			return (IVariable[]) fVariables.get(frame);
			IVariable[] frameVar = (IVariable[]) fVariables.get(frame);
			if( fVariables.size() > 0 && frameVar == null) {
				// TODO debug: check if frame updated by debug platform
				Set<IStackFrame> theThreads = fVariables.keySet();
				Iterator<IStackFrame> curFrames = theThreads.iterator();
				IStackFrame curFrame = curFrames.next();
				try {
					if (curFrame.getLineNumber() != frame.getLineNumber()) {
						Collection<IVariable[]> theVars = fVariables.values();
						frameVar = fVariables.get(curFrame);
					}
				} catch (DebugException e) {
					
				}
			}
			return frameVar;
		}
	}
	
	public IVariable[] getVariables() {
		return new IVariable[0];
	}
	
	/**
	 * Pops the top frame off the callstack.
	 * 
	 * @throws DebugException
	 */
	public void pop() throws DebugException {
		sendRequest("drop");
	}
	
	/**
	 * Returns whether this thread can the top stack frame.
	 * 
	 * @return whether this thread can pop the top stack frame
	 */
	public boolean canPop() {
		try {
			return getStackFrames().length > 1;
		} catch (DebugException e) {
			
		}
		return false;
	}
	
	@Override
	public boolean hasStackFrames() throws DebugException {
		return isSuspended();
	}

	@Override
	public int getPriority() throws DebugException {
		return 0;
	}

	@Override
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public IBreakpoint[] getBreakpoints() {
		if (fBreakpoint == null) {
			return new IBreakpoint[0];
		}
		return new IBreakpoint[] {fBreakpoint};
//		try {
//			if (fBreakpoint == null) {
//				return new IBreakpoint[0];
//			}
//			return new IBreakpoint[] {fBreakpoint};
//			
//		} catch (CoreException e) {
//			DebugCorePlugin.log(e);
//			return new IBreakpoint[0];
//		}
	}
	
	/**
	 * Notifies this thread it has been suspended by the given breakpoint.
	 * 
	 * @param brakpoint breakpoint
	 */
	public void suspendedBy(IBreakpoint breakpoint) {
		fBreakpoint = breakpoint;
		suspended(DebugEvent.BREAKPOINT);
	}
	
	// -- 
	void throwDebugException(IOException e) throws DebugException {
		throw new DebugException(new Status(IStatus.ERROR,
				APLDebugCorePlugin.getUniqueIdentifier(), IStatus.OK,
				"An error occurred during communication with the debugger process",
				e));
	}

//	Not needed implemented in PDADebugElement
//	public String getModelIdentifier() {
//		return DebugCorePlugin.getUniqueIdentifier();
//	}
	
	/**
	 * Returns this thread's unique identifier
	 * 
	 * @return this thread's unique identifier
	 */
	public int getIdentifier() {
		return fThreadId;
	}

	public void setTopStackWindowId(int token) {
		this.topStackWindowId = token;
	}
	
	/**
	 * Top stack window token
	 */
	public int getTopStackWindowId() {
		return topStackWindowId;
	}
	
	public void setStackFrame(JSONArray stack) {
		try {
			int len = stack.length();
			APLStackFrame[] theFrames = new APLStackFrame[len];
			for (int i = 0; i < len; i++) {
				String data = stack.getString(i);
				int posStartLineNum = data.indexOf('[');
				int posEndLineNum = data.indexOf(']');
				int lineNum;
				try {
					lineNum = Integer.parseInt(data.substring(posStartLineNum + 1, posEndLineNum)) + 1;
				} catch (NumberFormatException e) {
					lineNum = 1;
				} catch (StringIndexOutOfBoundsException e2) {
					lineNum = 1;
				}
				String namespace = data.substring(0, posStartLineNum);
				int indexFunName = namespace.lastIndexOf('.');
				String funName = namespace.substring(indexFunName + 1);
				theFrames[i] = new APLStackFrame(this, data, lineNum, funName, i);
			}
			this.theFrames = theFrames;
			fireChangeEvent(DebugEvent.CONTENT);
			
		} catch(JSONException e) {
			
		}
	}

	
}
