package com.dyalog.apldev.debug.core.model;

//import java.util.Calendar;

//import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDropToFrame;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
//import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.json.JSONArray;
import org.json.JSONObject;

import com.dyalog.apldev.debug.core.model.remote.EntityWindow;

public class APLStackFrame extends APLDebugElement implements IStackFrame, IDropToFrame {
	
	private APLThread fThread;
	private String fName;
	private int fPC;
	private long fTime;
	// for debug
	public String timeStamp;
//	private int lineNumber;
	private String fFileName;
//	private IPath fFilePath;
	private String fFileSource;
	private int fId;
	private String funName;
	private int lineNumber;
	
	/**
	 * Constructs a stack frame in the given thread with the given
	 * frame data.
	 * 
	 * @param thread
	 * @param data frame data
	 * @param id stack frame id (0 is the bottom of the stack)
	 */
	public APLStackFrame(APLThread thread, String data, int id) {
		super(thread.getAPLDebugTarget());

		this.fId = id;
		this.fThread = thread;
		init(data);
//		fireCreationEvent();

	}

	public APLStackFrame(APLThread thread, String data,
				int lineNumber, String funName, int id) {
		super(thread.getAPLDebugTarget());
		
		this.fId = id;
		this.fThread = thread;
		this.lineNumber = lineNumber;
		this.funName =funName;
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
	
	/**
	 * Initializes this frame based on its data
	 * 
	 * @param data
	 */
	private void init(String data) {
		String[] strings = data.split("\\|");
		String pc = strings[1];
		this.fPC = Integer.parseInt(pc) + 1;
		this.fTime = System.nanoTime();

		
		String fileName = strings[0];
//		fFilePath = new Path(fileName);
		fFileSource = fileName;
		fFileName = (new Path(fileName)).lastSegment();
		fName = strings[2];
		int numVars = strings.length - 3;
		IVariable[] vars = new IVariable[numVars];
		for (int i = 0; i < numVars; i++) {
			vars[i] = new APLVariable(this, strings[i + 3]);
		}
		fThread.setVariables(this, vars);
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
		return fId > 0 && fThread.isSuspended() ? true : false;
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
		return fFileName;
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

	/**
	 * !!! Must return non <code>null</code>, or
	 * org.eclipse.debug.internal.ui.model.elements.StackFrameContentProviderElementContentProvider
	 * can induce NullPointerException
	 */
	public IVariable[] getVariables() throws DebugException {
		if (!fThread.isSuspended()) return new IVariable[0];
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

	public String getName() throws DebugException {
		return fName;
	}

	public IRegisterGroup[] getRegisterGroups() throws DebugException {
		return null;
	}

	public boolean hasRegisterGroups() throws DebugException {
		return false;
	}

	public boolean canDropToFrame() {
//		return getThread().canDropToFrame(fId);
		return fId > 0 && fThread.isSuspended() ? true : false;
	}

	public void dropToFrame() throws DebugException {
		// if current isn't top stack frame drop other
		if (fId > 0 &&
				fThread.getStackFramesCount()-1 > fId) {
			APLStackFrame frame = fThread.getStackFrame(fId + 1);
			if (frame != null)
				frame.dropToFrame();
//			fThread.dropToFrame(fId + 1);
		}
		fThread.dropToFrame();
	}
}
