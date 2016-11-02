package com.dyalog.apldev.debug.core.model;

//import java.util.Calendar;

//import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDropToFrame;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
//import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.json.JSONArray;
import org.json.JSONObject;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.model.remote.EntityWindow;

public class APLStackFrame extends APLDebugElement implements IStackFrame, IDropToFrame {
	
	private APLThread fThread;
	private String fName;
	private int fPC;
	private long fTime;
	private String fFileName;
	private String fFileSource;
	private int fId;
	private String funName;
	private int lineNumber;
	/**
	 * This frame's depth in the call stack (0 == bottom of stack). A new is
	 * indicated by -2. An invalid frame is indicated by -1.
	 */
	private int fDepth = -2;
	/**
	 * Stack frame value received from interpreter
	 */
	private String fStackFrameData;
	/**
	 * Whether the variables need refreshing
	 */
	private boolean fRefreshVariables = true;
	
	/**
	 * Constructs a stack frame in the given thread with the given
	 * frame data.
	 * 
	 * @param thread
	 * @param data frame data
	 * @param id stack frame id (0 is the bottom of the stack)
	 */
	public APLStackFrame(APLThread thread, String frame, int depth) {
//		super(thread.getAPLDebugTarget());
		super(thread.getDebugTarget());

		this.fThread = thread;
		bind(frame, depth);
	}

	/**
	 * Creates a new stack frame in the given thread
	 */
//	public APLSTackFrame(APLThread thread, String frame, int depth) {
//		super((APLDebugTarget) thread.getDebugTarget())
//	}
	
	public APLStackFrame(APLThread thread, String data,
				int lineNumber, String funName, int id) {
		super(thread.getAPLDebugTarget());
		
		this.fId = id;
		this.fThread = thread;
		this.lineNumber = lineNumber;
		this.funName = funName;
		this.fFileSource = funName + ".apl";
		this.fFileName = fFileSource;
		this.fName = data;
	}

	public String getFunctionName() {
		return funName;
	}
	/**
	 * Updates Stack frame with new parameters
	 * 
	 * @param data
	 * @param id
	 */
	public void update(String data, int id) {
		this.fId = id;
		init(data);
	}
	
	public boolean canStepInto() {
		return getThread().canStepInto();
	}

	public boolean canStepOver() {
		return getThread().canStepOver();
	}

	public boolean canStepReturn() {
		
//		return fId != 0 ? getThread().canStepReturn()
//				: false;
//		return fId > 0 && fThread.isSuspended() ? true : false;
		return fThread.isSuspended() ? true : false;
	}

	public boolean isStepping() {
		return getThread().isStepping();
	}

	public void stepInto() throws DebugException {
		fThread.stepInto();
	}

	public void stepOver() throws DebugException {
		getThread().stepOver();
	}

	public void stepReturn() throws DebugException {
		if (fId > 0 && 
				fThread.getStackFramesCount()-1 > fId) {
			APLStackFrame frame = fThread.getStackFrame(fId+1);
			if (frame != null)
				frame.stepReturn();
		}
		fThread.stepReturn();;
	}

	public boolean canResume() {
		return getThread().canResume();
	}

	public boolean canSuspend() {
		return getThread().canSuspend();
	}

	public boolean isSuspended() {
		return getThread().isSuspended();
	}

	public void resume() throws DebugException {
		getThread().resume();
	}

	public void suspend() throws DebugException {
		getThread().suspend();
	}

	public boolean canTerminate() {
		return getThread().canTerminate();
	}

	public boolean isTerminated() {
		return getThread().isTerminated();
	}

	public void terminate() throws DebugException {
		getThread().terminate();
	}
	
	/**
	 * Returns the name of the source file this stack frame is associated
	 * with.
	 * 
	 * @return the name of the source file this stack frame is associated
	 * with
	 */
	public String getSourceName() {
		return funName + "." + APLDebugCorePlugin.MODULES_EXTENSION;
//		return fFileName;
	}
	
//	public IPath getSourcePath() {
//		return fFilePath;
//	}
	
	public String getSourceFile() {
		return fFileSource;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof APLStackFrame) {
			APLStackFrame sf = (APLStackFrame) obj;
			return sf.getThread().equals(getThread()) &&
					sf.getSourceName().equals(getSourceName()) &&
					sf.fId == fId;
		}
		return false;
	}

	// Used by Platform for buffering StackFrames and
	// for creating unique keys in HashMap
//	public int hashCode() {
////		return (fName + fTime).hashCode() + fId;
////		return (int) fTime;
//		return fFileName.hashCode()+fId+fPC;
//	}
	
	/**
	 * Returns this stack frame's unique identifier within its thread
	 * 
	 * @return this stack frame's unique identifier within its thread
	 */
	protected int getIdentifier() {
		return fId;
	}
	
	/**
	 * Returns the stack frame's thread's unique identifier
	 * 
	 * @return this stack frame's thread's unique identifier
	 */
	protected int getThreadIdentifier() {
		return fThread.getIdentifier();
	}
	
	public APLThread getThread() {
		return fThread;
	}

	/*
	 * Must return non <code>null</code>, or
	 * org.eclipse.debug.internal.ui.model.elements.StackFrameContentProviderElementContentProvider
	 * can induce NullPointerException
	 */
	public IVariable[] getVariables() throws DebugException {
//		if (!fThread.isSuspended()) return new IVariable[0];
//		return fThread.getVariables(this);
//		IVariable[] ThreadVar = fThread.getVariables(this);
		IVariable[] globVars = ((APLDebugTarget) getDebugTarget()).getGlobalVariables();
		if (globVars == null) return new IVariable[0];
		return globVars;
	}

	public boolean hasVariables() throws DebugException {
		return getVariables().length > 0;
	}

	public int getLineNumber() throws DebugException {
		return this.lineNumber;
	}

	public int getCharStart() throws DebugException {
		return -1;
	}

	public int getCharEnd() throws DebugException {
		return -1;
	}

	/**
	 * Stack frame label in Debug View
	 */
	public String getName() throws DebugException {
//		return fName;
		return fStackFrameData;
	}

	public IRegisterGroup[] getRegisterGroups() throws DebugException {
		return null;
	}

	public boolean hasRegisterGroups() throws DebugException {
		return false;
	}

	public boolean canDropToFrame() {
//		return getThread().canDropToFrame(fId);
//		return fId > 0 && fThread.isSuspended() ? true : false;
		return fThread.isSuspended() ? true : false;
	}

	public void dropToFrame() throws DebugException {
		// if current isn't top stack frame drop other
//		if (fId > 0 &&
//				fThread.getStackFramesCount()-1 > fId) {
//			APLStackFrame frame = fThread.getStackFrame(fId + 1);
//			if (frame != null)
//				frame.dropToFrame();
////			fThread.dropToFrame(fId + 1);
//		}
		fThread.dropToFrame();
	}

	/**
	 * Binds this frame to the given underlying frame or returns a new frame
	 * representing the given frame. A frame can only be re-bound to an underlying
	 * frame if it refers to the same depth on the stack in the same method.
	 */
	public APLStackFrame bind(String frame, int depth) {
		synchronized(fThread) {
			if (fDepth == -2) {
				// first initialization
				fStackFrameData = frame;
				fDepth = depth;
				fId = depth;
				init(frame);
				return this;
			} else if (depth == -1) {
				// mark as invalid
				fDepth = -1;
				fStackFrameData = null;
				return null;
			} else if (fDepth == depth) {
				fStackFrameData = frame;
				init(frame);
				return this;
			}
			// invalidate this frame
			bind(null, -1);
			// return a new frame
			return new APLStackFrame(fThread, frame, depth);
		}
	}

	/**
	 * Initializes this frame based on its data
	 * 
	 * @param data
	 */
	private void init(String data) {
		int posStartLineNum = data.indexOf('[');
		int posEndLineNum = data.indexOf(']');
		int lineNum;
		try {
			lineNum = Integer.parseInt(
					data.substring(posStartLineNum + 1, posEndLineNum)) + 1;
		} catch (NumberFormatException e) {
			lineNum = 1;
		} catch (StringIndexOutOfBoundsException e2) {
			lineNum = 1;
		}
		this.lineNumber = lineNum;
		String namespace = data.substring(0, posStartLineNum);
		int indexFunName = namespace.lastIndexOf('.');
		funName = namespace.substring(indexFunName + 1);
	}

	/**
	 * Sets the underlying stack frame data. Called by a thread when incrementally
	 * updating after a step has completed.
	 */
	protected void setUnderlyingStackFrame(String frame) {
		synchronized (fThread) {
			fStackFrameData = frame;
			if (frame == null) {
				fRefreshVariables = true;
			}
		}
	}

}
