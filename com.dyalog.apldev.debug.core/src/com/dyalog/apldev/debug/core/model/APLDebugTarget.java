package com.dyalog.apldev.debug.core.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointManagerListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.LineBreakpoint;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.console.IOConsolePartition;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.breakpoints.APLLineBreakpoint;
import com.dyalog.apldev.debug.core.breakpoints.APLRunToLineBreakpoint;
import com.dyalog.apldev.debug.core.console.AplDevConsoleInterpreter;
import com.dyalog.apldev.debug.core.console.SessionConsole;
import com.dyalog.apldev.debug.core.model.remote.CommandProcessing;
import com.dyalog.apldev.debug.core.model.remote.DebuggerReader;
import com.dyalog.apldev.debug.core.model.remote.DebuggerWriter;
import com.dyalog.apldev.debug.core.model.remote.EntityWindow;
import com.dyalog.apldev.debug.core.model.remote.EntityWindowsStack;
import com.dyalog.apldev.debug.core.model.remote.RemoteDebugger;
import com.dyalog.apldev.debug.core.model.remote.ReplyEditRequest;
import com.dyalog.apldev.debug.core.model.remote.ReplyRequest;
import com.dyalog.apldev.debug.core.model.remote.ReplyStackRequest;
import com.dyalog.apldev.debug.core.model.remote.RequestValueTip;
import com.dyalog.apldev.debug.core.model.remote.RequestWsTree;
import com.dyalog.apldev.debug.core.model.remote.StackData;
import com.dyalog.apldev.debug.core.model.remote.WorkspaceEditorInput;
import com.dyalog.apldev.debug.core.protocol.APLEvent;
import com.dyalog.apldev.debug.core.protocol.CIPEvent;
import com.dyalog.apldev.debug.core.protocol.FocusEvent;

/**
 * APL Debug Target
 */
@SuppressWarnings("restriction")
public class APLDebugTarget extends APLDebugElement implements IDebugTarget,
		IBreakpointManagerListener, ILaunchListener, IAPLEventListener {

	// associated system process (VM)
	public volatile IProcess fProcess;
	
	// containing launch object
	private ILaunch fLaunch;
	private String mainModule;
	private RemoteDebugger fDebugger;
	public final IProject fProject;
	
	// sockets to communicate with VM
	private Socket fRequestSocket;
//	private PrintWriter fRequestWriter;
	private OutputStream fRequestWriter;
//	private BufferedReader fRequestReader;
	private InputStream fRequestReader;
//	private Socket fEventSocket;
//	private BufferedReader fEventReader;
	
	// suspended state
	private boolean fVMSuspended = false;
	// terminated state
	private boolean fTerminated = false;
	
	// threads
//	private IThread[] fThreads;
	private Map<Integer, APLThread> fThreads = Collections.synchronizedMap(
			new LinkedHashMap<Integer, APLThread>());
//	private APLThread fThread;
	
	// event dispatch job
	private EventDispatchJob fEventDispatch;
	// event listeners;
	private Vector<IAPLEventListener> fEventListeners = new Vector<>();
//	private List<IPDAEventListener> fEventListeners = Collections.synchronizedList(
//			new ArrayList<IPDAEventListener>());
	// give delay for creating frames at first suspended
	
	private String fName = "APL";
	/**
	 * Type of receiving message sequence form Interpreter
	 */
	enum Part {lookForMagicNum, readMsg}
	


    private boolean fConnected = false;

	private boolean fStreamsClosed = false;

	private SessionConsole fConsoles;

	public volatile boolean finishedInit = false;

	private Socket fSocket;

	private DebuggerReader fReader;

	private DebuggerWriter fWriter;

	private IOConsoleOutputStream consoleStdOut;

	private IOConsoleOutputStream consoleErrOut;

	private boolean fDisconnected = false;

	private CommandProcessing fCommandProc;

	private AplDevConsoleInterpreter consoleInterpreter;

	/**
	 * Displayed nodeId in WS Explorer;
	 */
	private int currentNodeId = 0;
	
//	private IVariable[] fGlobalVariables = new IVariable[0];
	private Map<APLVariable, APLValue> fGlobalVariables = Collections.synchronizedMap(
			new LinkedHashMap<APLVariable, APLValue>());

	private IFile fProgram;

	/**
	 * Error code
	 */
	private int err;
	private int dmx;

	/**
	 * Stores token id and EntityWindow, name and token id pairs
	 */
	private EntityWindowsStack entityWindowsStack;

	/**
	 * Current thread set by FocusThread
	 */
	private int tid;

	/**
	 * Thread which will receive suspend terminate when DebugTarget selected
	 */
	private APLThread currentThread;

	/**
	 * Set if need update list of global variables
	 */
	private boolean updateGlobalVariables;

	/**
	 * Disconnect message from interpreter
	 */
	private String disconnectMessage;

	private boolean fLaunchInterpreter;

	private boolean fStarted;


	public IProject getProject() {
		return fProject;
	}

	public void setCurrentThread (int tid) {
		this.tid = tid;
	}
	
	public SessionConsole getConsoles() {
		return fConsoles; 
	}
	
	/**
	 * Interpreter commands writer
	 */
	public DebuggerWriter getInterpreterWriter() {
		return fWriter;
	}
	
	/**
	 * Interpreter commands reader
	 */
	public DebuggerReader getInterpreterReader() {
		return fReader;
	}
	
	public Socket getSocket() {
		return fSocket;
	}
	
	/**
	 * Listens to events form APL VM and fires corresponding
	 * debug events.
	 */
	class EventDispatchJob extends Job {
		
		public EventDispatchJob() {
			super("APL Inerpreter Messages Dispatch");
			setSystem(true);
		}
		
		protected IStatus run(IProgressMonitor monitor) {
			String message = "";
			Part part = Part.lookForMagicNum;
			MagicNum magicNum= new MagicNum();
			byte[] utf8Bytes={};
			int index = 0;
			boolean newMessage = false;
			while (!isTerminated() && message != null) {
				try {
//					message = fEventReader.readLine();
					int n=fRequestReader.available();
					if(n>0) {
						switch (part) {
						case lookForMagicNum:
							int len = magicNum.push((byte)fRequestReader.read());
							if (len != -1 && len > 8) {
								part=Part.readMsg;
								utf8Bytes=new byte[len - 8];
								index = 0;
							}
							break;
						case readMsg:
							int inVal = fRequestReader.read();
							if (inVal != -1) {
								utf8Bytes[index]=(byte) inVal;
								index++;
								if (index >= utf8Bytes.length) {
									message = new String(utf8Bytes, "UTF8");
//									System.out.println(magicNum.Len + " < " + message);
									Date date = new Date();
//									Calendar. 
									
									fConsoles.WriteRIDE(date.getHours() + ":"
											+ date.getMinutes() + ":"
											+ date.getSeconds() + " < "
											+ message);

									part= Part.lookForMagicNum;
									newMessage = true;
								}
							}
							break;
						}
					}

//						FileOutputStream fos = new FileOutputStream("c:\\Dyalog\\testResive.txt", true);
//						fos.write(utf8Bytes);
//						fos.flush();
//
//						int readLen=0;
//						do {
//							int len = (int) getMsgLen(utf8Bytes, readLen);
//							if (len == -1)
//								break;
//							byte[] msgBytes = new byte[len-8];
//							for (int i = 0; i < msgBytes.length; i++) {
//								if (utf8Bytes.length <= i + readLen + 8) {
//									System.out.println("n: "+n+"; len: "+len+"; readLen:"+readLen);
//								}
//								msgBytes[i] = utf8Bytes[i + readLen + 8];
//							}
//							message = new String(msgBytes, "UTF8");
//							System.out.println(len + " < " + message);
//							readLen += len;
//							if (readLen < n)
//								System.out.print("+ "); 
//						} while (readLen < n);
//					} else
//						message = "";
					
					if (newMessage) {
						newMessage = false;
//						event = fEventReader.readLine();
//						if (event != null)

						Object[] listeners = fEventListeners.toArray();
						String event = message;
//						APLEvent event = null;
//						try {
//							event = APLEvent.parseEvent(message);
//						} catch (IllegalArgumentException e) {
//							DebugCorePlugin.getDefault().getLog().log(
//									new Status (IStatus.ERROR, "example.debug.core",
//											"Error parsing APL event", e));
//							continue;
//						}
						for (int i = 0; i < listeners.length; i++) {
							((IAPLEventListener) listeners[i]).handleEvent(event);
						}
					}
				} catch (IOException e) {
//					terminated();
					vmTerminated();
				}
			}
			return Status.OK_STATUS;
		}
	}

	/**
	 * Notify event listeners (DebugTarget, Threads, LineBreakPoints)
	 */
	public void EventListeners(String cmdName, JSONObject cmdVal) {
		Object[] listeners = fEventListeners.toArray();
		for (int i = 0; i < listeners.length; i++) {
			((IAPLEventListener) listeners[i]).handleEvent(cmdName);
		}
	}
	/**
	 * Search for magic number in message from Interpreter
	 */
	class MagicNum {
		byte[] magicNum = {0, 0, 0, 0, 0, 0, 0, 0};
		int Len=-1;
		MagicNum() {
			
		}
		
		MagicNum(int len) {
			setLen(len);
			magicNum[4] = 'R'; 
			magicNum[5] = 'I';
			magicNum[6] = 'D';
			magicNum[7] = 'E';
		}
		byte[] getMagicNum() {
			return magicNum;
		}
		/**
		 * Push numbers to 8-byte array 
		 * @param Num
		 * @return message <code>length</code> if 8-bytes contain RIDE magic number
		 * else <code>-1<code>
		 */
		int push(byte Num) {
			for (int i=0; i<7; i++) {
				magicNum[i] = magicNum[i+1];
			}
			magicNum[7] = Num;
			if (checkRide())
				return retriveLen();
			else
				return -1;
			}
			boolean checkRide() {
				if(magicNum[4] == 'R' 
						&& magicNum[5] == 'I'
						&& magicNum[6] == 'D'
						&& magicNum[7] == 'E')
					return true;
				else
					return false;
			}
			int retriveLen() {
				int len = 0;
				for (int i=0; i<4; i++) {
					len += ((int) magicNum[i] & 0xFF) << 8*(3-i);
				}
				Len = len;
				return len;
			}
			void clear() {
				for(int i=0; i<8; i++) {
					magicNum[i] = 0;
				}
			}
			/**
			 * Convert int to 4 byte array
			 */
			void setLen(int len) {
				for (int i=3; len != 0 && i >= 0; i--) {
					magicNum[i] = (byte) (len & 0xFF);
					len = len >> 8;
				}
			}
		}
	
	public synchronized void setLastError(int err, int dmx) {
		this.err = err;
		this.dmx = dmx;
	}
	
	/**
	 * Registers the given event listener. The listener will be notified of
	 * events in the program being interpreted. Has no effect if the listener
	 * is already registered.
	 * 
	 * @param listener event listener
	 */
	public void addEventListener(IAPLEventListener listener) {
		if (!fEventListeners.contains(listener)) {
			fEventListeners.add(listener);
		}
	}
	
	/**
	 * Deregisters the given event listener. Has no effect if the listener is
	 * not currently registered.
	 * 
	 * @param listener event listener
	 */
	public void removeEventListener(IAPLEventListener listener) {
		fEventListeners.remove(listener);
	}
	

	/**
	 * Constructs a new debug target
	 * 
	 * @param launch
	 * @param process
	 * @param file
	 * @param debugger
	 * @param project
	 */
	public APLDebugTarget(ILaunch launch, IProcess process,
			String mainModule, RemoteDebugger debugger, IProject project) {

		super(null);
		this.fLaunch = launch;
		this.fProcess = process;
		this.mainModule = mainModule;
		this.fDebugger = debugger;
		this.fStarted = false;
//		this.fThreads = new APLThread[0];
		this.fProject = project;
		try {
			this.fLaunchInterpreter = launch.getLaunchConfiguration()
					.getAttribute(APLDebugCorePlugin.ATTR_LAUNCH_INTERPRETER, false);
		} catch (CoreException e) {
			this.fLaunchInterpreter = false;
		}

		this.fName = fProject.getName();

		fLaunch.addDebugTarget(this);
		fDebugger.addTarget(this);

		IBreakpointManager breakpointManager = getBreakpointManager();
		breakpointManager.addBreakpointListener(this);
		breakpointManager.addBreakpointManagerListener(this);

		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);

		this.entityWindowsStack = new EntityWindowsStack(this);
	}

	/**
	 * Creates session interactive console
	 */
	public void addSessionConsole() {
		if (fConsoles == null) {
			try {
				fConsoles = new SessionConsole(this);
			} catch (DebugException e) {
				APLDebugCorePlugin.log(IStatus.ERROR, "Error when creating console", e);
			}
		}
		// Currently interpreter don't work properly with eclipse process console
//		if (getProcess() != null)
//			fConsoles.addProcessConsoleInputListener();
//		IConsole console = DebugUITools.getConsole(getProcess());
//		if (console instanceof ProcessConsole) {
//			String key = "org.eclipse.debug.ui.ATTR_CONSOLE_PROCESS";
//			Object val = ((ProcessConsole) console).getAttribute(key);
//			System.out.println(val);
//		}
	}

	/**
	 * Remove session interactive console
	 */
	public void removeSessionConsole() {
		if ( fConsoles != null && fConsoles.isOpened() ) {
			fConsoles.close();
		}
	}

@SuppressWarnings("restriction")
// Write to Process console
	void writeProcessConsole() {
		if (consoleStdOut != null) {
	        try {
	        	consoleStdOut.write("\nIt work std\n");
	        	consoleStdOut.flush();

	        	consoleErrOut.write("\nIt work err\n");
	        	consoleErrOut.flush();
	        	
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	}
	/**
     * This function adds the input listener extension point, so that plugins that only care about
     * the input in the console can know about it.
     */
//	@SuppressWarnings({"restriction"})
	public void addConsoleInputListener() {
		IConsole console = DebugUITools.getConsole(this.getProcess());
		if (console instanceof ProcessConsole) {
			final ProcessConsole c = (ProcessConsole) console;
	        consoleStdOut = c.getStream(IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM);
	        consoleErrOut = c.getStream(IDebugUIConstants.ID_STANDARD_ERROR_STREAM);

//			final List<IConsoleInputListener> participants = ExtensionHelper
//					.getParticipants(ExtensionHelper.APLDEV_DEBUG_CONSOLE_INPUT_LISTENER);
			final APLDebugTarget target = this;
			c.getDocument().addDocumentListener(new IDocumentListener() {
				
				@Override
				public void documentAboutToBeChanged(DocumentEvent event) {
					// check if present a new line symbol
					if (event.fText.indexOf('\r') != -1 || event.fText.indexOf('\n') != -1) {
						try {
							ITypedRegion partition = event.fDocument.getPartition(event.fOffset);
							if (partition instanceof IOConsolePartition) {
								IOConsolePartition p = (IOConsolePartition) partition;
								
								// check if that is user input
								if (p.getType().equals(IOConsolePartition.INPUT_PARTITION_TYPE)) {
									if (event.fText.length() <= 2) {
										final String inputFound = p.getString();
										fConsoles.WriteRIDE("TYPE: " + inputFound);
//										for (IConsoleInputListener listener : partcipants) {
//											listener.newLineReceived(inputFound, target);
//										}
									}
								}
							}
						} catch (Exception e) {
							APLDebugCorePlugin.getDefault().getLog().log(
						            new Status (IStatus.ERROR, APLDebugCorePlugin.PLUGIN_ID,
						            		"Error listen input for process console", e));
						}
					}
				}
				
				@Override
				public void documentChanged(DocumentEvent event) {
					// only report when have a new line
					if (event.fText.indexOf('\r') != -1 || event.fText.indexOf('\n') != -1) {
						try {
							ITypedRegion partition = event.fDocument.getPartition(event.fOffset);
							if (partition instanceof IOConsolePartition) {
								IOConsolePartition p = (IOConsolePartition) partition;
								
								if (p.getType().equals(IOConsolePartition.INPUT_PARTITION_TYPE)) {
									if (event.fText.length() > 2) {
										// user pasted something
										fConsoles.WriteRIDE("PASTE: " + event.fText);
//										for (IOConsoleInputListener listener : participants) {
//											listener.pasteReceived(event.fText, target);
//										}
									}
								}
							}
						} catch (Exception e) {
							APLDebugCorePlugin.getDefault().getLog().log(
						            new Status (IStatus.ERROR, APLDebugCorePlugin.PLUGIN_ID,
						            		"Error listen input for process console", e));
						}
					}
				}
			});
		}
	}
	
	public boolean canTerminate() {
//		boolean debug = true;
//		if (debug) return true;
//		if (getProcess() != null)
//			return getProcess().canTerminate();
		return !isTerminated();
	}

	public boolean isTerminated() {
//		if (getProcess() != null)
//			return fTerminated || getProcess().isTerminated();
		return fTerminated;
	}

	public void terminate() throws DebugException {
		if (!isDisconnected() && fLaunchInterpreter) {
			// terminate interpreter through RIDE command
			DebuggerWriter writer = getInterpreterWriter();
			if (writer != null)
				writer.postTerminate();
			else
				disconnect();
		}
		Set<Integer> setThread= fThreads.keySet();
		for (Integer k : setThread) {
			exited(k);
		}

		exited(0);
		if (getThread(0) != null)
			getThread(0).terminate();
		vmTerminated();

		if (fProcess != null) {
			fProcess.terminate();
			fProcess = null;
		}

		if (fConsoles != null) {
			fConsoles.terminate();
		}
		fireTerminateEvent();
//		fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
	}

	public boolean canResume() {
		return !isTerminated() && isSuspended() && isDisconnected();
	}

	public boolean canSuspend() {
		return true;
	}

	public boolean isSuspended() {
		return false;
	}

	public void resume() throws DebugException {
		IThread[] threads = getThreads();
		for (int i = 0; i < threads.length; i++) {
				threads[i].resume();
		}
//		if (entity != null && entity.getDebugger())
//			getInterpreterWriter()
//				.postCommand("[\"RestartThreads\",{\"win\":" + entity.token +"}]");
	}

	public void suspend() throws DebugException {
		getInterpreterWriter().postCommand("[\"StrongInterrupt\",{}]");
	}

	public void breakpointAdded(IBreakpoint breakpoint) {
		if (supportsBreakpoint(breakpoint)) {
			try {
				if ((breakpoint.isEnabled() && getBreakpointManager().isEnabled())
						|| !breakpoint.isRegistered()) {
					APLLineBreakpoint aplBreakpoint = (APLLineBreakpoint) breakpoint;
					aplBreakpoint.install(this);
				}
			} catch (CoreException e) {
				
			}
		}
	}

	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (supportsBreakpoint (breakpoint)) {
			try {
				APLLineBreakpoint aplBreakpoint = (APLLineBreakpoint) breakpoint;
				// check if run_to_line breakpoint at same line as enabled line_breakpoint
				boolean skipRemove = false;
				if (breakpoint instanceof APLRunToLineBreakpoint) {
					int lineNumber = ((APLRunToLineBreakpoint) breakpoint).getLineNumber(); 
					IBreakpoint[] breakpoints = getBreakpointManager().getBreakpoints(getModelIdentifier());
					for (int i = 0; i < breakpoints.length; i++) {
						if (breakpoints[i] instanceof APLLineBreakpoint &&
								((LineBreakpoint) breakpoints[i]).getLineNumber() == lineNumber) {
							skipRemove = ((LineBreakpoint) breakpoints[i]).isEnabled() ? true : false;
							break;
						}
					}
				}
				if (!skipRemove) aplBreakpoint.remove(this);
			} catch (CoreException e) {
				
			}
		}
	}

	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (supportsBreakpoint(breakpoint)) {
			try {
				if (breakpoint.isEnabled() && getBreakpointManager().isEnabled()) {
					breakpointAdded(breakpoint);
				} else {
					breakpointRemoved(breakpoint, null);
				}
			} catch (CoreException e) {
				
			}
		}
	}

	public boolean canDisconnect() {
		return !fDisconnected;
	}

	public void disconnect(String message) throws DebugException {
		this.disconnectMessage = message;
		if (!isDisconnected()) {
			disconnect();
		}
		else {
			fireChangeEvent(DebugEvent.STATE);
		}

	}
	
	/**
	 * Disconnection reason
	 */
	public String getDisconnectMessage() {
		return disconnectMessage;
	}
	
	/**
	 * Disconnect action from toolbar or menu
	 */
	public void disconnect() throws DebugException {
		if (!isDisconnected()) {
			fDisconnected  = true;
			fConnected = false;
			disconnectSocket();
			if (fConsoles != null)
				fConsoles.terminate();
			fireChangeEvent(DebugEvent.STATE);
	//		fireEvent(new DebugEvent(this, DebugEvent.CHANGE));

			if (fCommandProc != null) {
				fCommandProc.done();
				fCommandProc = null;
			}
		}
	}
	
	private void disconnectSocket() {
		if (fSocket != null) {
			try {
				fSocket.shutdownInput();
			} catch (Exception e) {
				
			}
			try {
				fSocket.shutdownOutput();
			} catch (Exception e) {
				
			}
			try {
				fSocket.close();
			} catch (Exception e) {
				
			}
		}
		fSocket = null;
		if (fWriter != null) {
			fWriter.done();
			fWriter = null;
		}
		if (fReader != null) {
			fReader.done();
			fReader = null;
		}

	}
	

	public boolean isDisconnected() {
		return fDisconnected;
	}
	public void setConnected() {
		fConnected = true;
	}

	public boolean supportsStorageRetrieval() {
		return false;
	}

	public IMemoryBlock getMemoryBlock(long startAddress, long length)
			throws DebugException {
		return null;
	}

	/**
	 * Notification that we have connected to the VM and it has started.
	 * Resume the VM.
	 * @param event
	 */
	private void vmStarted() {
		fireCreationEvent();
//		try {
//			resume();
//		} catch (DebugException e) {
//			
//		}
	}
	
	public boolean getStarted() {
		return fStarted;
	}
	
	private void started() {
		if (fStarted)
			return;
		fStarted = true;
//		createThread
		CreateThread(0, "Tid:0", false);

		// Load source files into workspace
		LoadProject(fProject);

//		installDeferredBreakpoints();

		startModule(mainModule);
	}
	
	private void startModule(String module) {
		if (module == null || module.length() == 0)
			return;

		// launch main module
		
		if (module != null && module.length() > 0) {
			JSONArray cmd = new JSONArray();
			cmd.put(0,"Execute");
			JSONObject val = new JSONObject();
			val.put("text", module + "\n");
			val.put("trace", 0);
			cmd.put(1, val);
			this.getInterpreterWriter().postCommand(cmd.toString());
		}
	}

	private void exited(int id) {
		APLThread thread;
		synchronized (fThreads) {
			thread = fThreads.remove(id);
		}
		if (thread != null)
			thread.exit();
	}

	public void LoadProject(IProject project) {
		List <IFile> contents = new ArrayList <IFile> ();
		if (fProject != null) {
			try {
				IResource[] members = fProject.members();
				
				for (int i = 0; i < members.length; i++) {
					if (members[i] instanceof IFile) {
						IFile file = (IFile) members[i];
						if (file.getFileExtension().equals(APLDebugCorePlugin.MODULES_EXTENSION)) {
							contents.add(file);
						}
					}
				}
			} catch (CoreException e) {
				
			}
		}
		Iterator <IFile> itrContents = contents.iterator();
		while (itrContents.hasNext()) {
			IFile file = itrContents.next();
			try {
				InputStream in;
				in = file.getContents();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
				List<String> text = new ArrayList<String>();
				String line;
				while ((line = reader.readLine()) != null) {
					text.add(line);
				}
				String name = file.getName();
				// remove file extension
				name = name.substring(0, name.length() - file.getFileExtension().length() - 1);
				boolean result = LoadFile(text, name);
				if (!result) {
					// wait and try again
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						
					}
					result = LoadFile(text, name);
					if (!result) {
						// TODO if can't load file into WS
					}
				}
			} catch (IOException e) {
	
			} catch (CoreException e) {
				APLDebugCorePlugin.log(e);
			}
		}
	}

	/**
	 * Load text into interpreter workspace
	 */
	public boolean LoadFile(List<String> text, String name) {
		String[] textStr = text.toArray(new String[text.size()]);
		return loadToWorkspace(textStr, name);
	}
	
	/**
	 * Load text into interpreter workspace
	 */
	public boolean loadToWorkspace(final String[] text, final String name) {
		if (name == null || name.length() == 0)
			return false;
		// get breakpoints for current function from breakpoints manager
		List <Integer> stopList = new ArrayList <> ();
		IBreakpoint[] breakpoints = getBreakpointManager().getBreakpoints(APLDebugCorePlugin.ID_APL_DEBUG_MODEL);
		for (int i = 0; i < breakpoints.length; i++) {
			if (breakpoints[i] instanceof APLLineBreakpoint) {
				APLLineBreakpoint breakpoint = (APLLineBreakpoint) breakpoints[i];
				IResource resource = breakpoint.getMarker().getResource();
				if (fProject.equals(breakpoint.getMarker().getResource().getProject())) {
					String resName = resource.getName();
					int extLen = resource.getFileExtension().length();
					if (extLen > 0) {
						resName = resName.substring(0, resName.length() - 1 - extLen);
					}
					if (name.equals(resName)) {
						try {
							stopList.add(breakpoint.getLineNumber() - 1);
						} catch (CoreException e) {
						}
					}
				}
			}
		}
		Integer[] stops = new Integer[stopList.size()];
		stopList.toArray(stops);
		// Check if function window already opened
		EntityWindowsStack entityWins = getEntityWindows();
		EntityWindow entityWin = entityWins.getEntity(name);
		if (entityWin != null && ! entityWin.isClosed()) {
			getInterpreterWriter().postSave(entityWin.token, text, stops);
			if ( ! entityWin.isTracer()) {
				getInterpreterWriter().postCloseWindow(entityWin.token);
			}
		} else {
			Runnable saveOnOpen = new Runnable() {

				@Override
				public void run() {
					EntityWindow entity = entityWins.getEntity(name);
					getInterpreterWriter().postSave(entity.token, text, stops);
				}
			};
			entityWins.addOnOpenActionWithClose(name, saveOnOpen);
			getInterpreterWriter().postEdit(0, 0, name);
		}
//		// open window
//		JSONArray cmd = new JSONArray();
//		cmd.put(0, "Edit");
//		JSONObject val = new JSONObject();
//		int tokenId = 0;
//		val.put("win", tokenId);
//		val.put("pos", 0);
//		val.put("text", name);
//		val.put("unsaved", new JSONObject());
//		cmd.put(1, val);
//		JSONArray ans = new ReplyRequest(getDebugTarget()).get(cmd);
//		JSONObject ansVal;
//		String tname = null;
//		if (ans != null) {
//			int win = 0;
//			try {
//				String ansCmd = ans.getString(0);
//				ansVal = ans.getJSONObject(1);
//				if (ansCmd.equals("GotoWindow")) {
//					win = ansVal.getInt("win");
//				} else if (ansCmd.equals("OpenWindow")) {
//					win = ansVal.getInt("token");
//				} else {
//					// incorrect answer
//					return false;
//				}
//			} catch (JSONException e) {
//				
//			}
//			if (win != 0) {
//				cmd = new JSONArray();
//				cmd.put(0, "SaveChanges");
//				val = new JSONObject();
//				val.put("win", win);
//				val.put("text", text);
//				val.put("stop", new int[0]);
//				cmd.put(1, val);
//				ans = new ReplyRequest(getDebugTarget()).get(cmd);
//				if (ans != null) {
//					try {
//						ansVal = ans.getJSONObject(1);
//						int err = ansVal.getInt("err");
//						int savedWin = ansVal.getInt("win");
//						if (err != 0) {
//							// TODO When err not empty
//						}
//						if (savedWin != win) {
//							// TODO When reply save changes to another win
//						}
//					} catch (JSONException e) {
//						
//					}
//				}
//				cmd = new JSONArray();
//				cmd.put(0, "CloseWindow");
//				val = new JSONObject();
//				val.put("win", win);
//				cmd.put(1,val);
//				ans = new ReplyRequest(getDebugTarget()).get(cmd);
//				if (ans != null && (!"CloseWindow".equals(ans.getString(0))
//						|| ans.getJSONObject(1).getInt("win") != win)) {
//					// TODO Wrong reply CloseWindow
//				}
//			} else
//				return false;
//		}
		return true;
	}

	/**
	 * Install breakpoints that are already registered with the breakpoint manager.
	 */
	private void installDeferredBreakpoints() {
		IBreakpoint[] breakpoints = getBreakpointManager().getBreakpoints(getModelIdentifier());
		for (int i = 0; i < breakpoints.length; i++) {
			breakpointAdded(breakpoints[i]);
		}
	}
	
	/** Called when this debug target terminates.
	 * 
	 */
//	private synchronized void terminated() {
	private void vmTerminated() {
		setTerminated(true);
		synchronized (fThreads) {
			fThreads.clear();
		}
		disconnectSocket();
		
		IBreakpointManager breakpointManager = getBreakpointManager();
		breakpointManager.removeBreakpointListener(this);
		breakpointManager.removeBreakpointManagerListener(this);
		fireTerminateEvent();
		removeEventListener(this);

		// close listening connection
		fDebugger.disposeConnector();

	}
	
	
	
	private int calcDetail(String reason) {
		if (reason.startsWith("suspended breakpoint")
				|| reason.startsWith("suspended watch")) {
			return DebugEvent.BREAKPOINT;
		}
		else if (reason.endsWith("step")) {
			return DebugEvent.STEP_INTO;
		}
		else if (reason.endsWith("client")) {
			return DebugEvent.CLIENT_REQUEST;
		}
		else if (reason.endsWith("drop")) {
//			return DebugEvent.STEP_RETURN;
			return DebugEvent.STEP_END;
		}
		else if (reason.startsWith("event")) {
			return DebugEvent.BREAKPOINT;
		}
		else {
			return DebugEvent.UNSPECIFIED;
		}
	}
	
	private void vmResumed(String event) {
		setVMSuspended(false);
		fireResumeEvent(calcDetail(event));
	}
	
	private synchronized void setVMSuspended(boolean suspended) {
		fVMSuspended = suspended;
	}
	private synchronized void setTerminated(boolean terminated) {
		fTerminated = terminated;
	}
	
	/**
	 * Returns the values on the data stack (top down)
	 * 
	 * @return the values on the data stack (top down)
	 */
	public IValue[] getDataStack(int node) throws DebugException {
		
		if(fWriter == null)
			return new IValue[0];
		JSONObject data = new RequestWsTree(fWriter).get(node);
		if (data == null)
			return new IValue[0];
		JSONArray classes;
		JSONArray nodeIds;
		JSONArray names;
		String err;
		int nodeId;
		try {
			classes = data.getJSONArray("classes");
			nodeIds = data.getJSONArray("nodeIds");
			names = data.getJSONArray("names");
			err = data.getString("err");
			nodeId = data.getInt("nodeId");
			currentNodeId = nodeId;
			if (err.length() > 0) {
				// TODO ReplyTreeList check if err obtained
			}

			if (nodeIds != null && nodeIds.length() > 0) {
				int len = nodeIds.length();
				IValue[] theValues = new IValue[len];
				for (int i = 0; i < len; i++) {
	//				String value = values[len - i - 1];
					theValues[i] = new APLDataStackValue(this, names.getString(i), i,
							(int) (classes.getDouble(i)), nodeIds.getInt(i), nodeId);
				}
				return theValues;
			}
		} catch (JSONException e) {
			APLDebugCorePlugin.log(IStatus.ERROR, "Field not present " + data.toString(), e);
		}
		return new IValue[0];
	}
	
	public int getCurrentNodeId() {
		return currentNodeId;
	}
	
	public IVariable[] getGlobalVariables() {
		UpdateGlobalVariables();
		IVariable[] var;
		synchronized (fGlobalVariables) {
			int len = fGlobalVariables.size();
			var = new IVariable[len];
			Set<APLVariable> vars = fGlobalVariables.keySet();
			Iterator<APLVariable> itr = vars.iterator();
			int i = 0;
			while(itr.hasNext()) {
				var[i] = (IVariable) itr.next();
				i++;
			}
			return var;
		}
	}

	public IValue getGlobalVariableValue(IVariable var) {
		IValue val;
		synchronized (fGlobalVariables) {
			val = fGlobalVariables.get(var);
		}
		if (val == null)
			val = new APLValue(this, "error obtaining value", null);
		return val;
	}
	
	public void setNeedUpdateGlobalVariables() {
		this.updateGlobalVariables = true;
	}
	
	public void promptChanged() {
		updateGlobalVariables = true;
	}
	
	public void UpdateGlobalVariables() {
		if ( ! updateGlobalVariables)
			return;
		updateGlobalVariables = false;
		JSONObject data = new RequestWsTree(fWriter).get(0);
		if (data == null) {
//			fGlobalVariables = new IVariable[0];
			return;
		}
		JSONArray classes;
		JSONArray nodeIds;
		JSONArray names;
		String err;
		try {
			nodeIds = data.getJSONArray("nodeIds");
			names = data.getJSONArray("names");
			err = data.getString("err");
			if (err.length() > 0) {

			}
			int len = 0;
			int nameSpaceNode = 0;
			if (nodeIds != null) len = nodeIds.length();
			for (int i = 0; i < len; i++) {
				if (names.getString(i).startsWith("#")) {
					nameSpaceNode = nodeIds.getInt(i);
					break;
				}
			}
			if (nameSpaceNode == 0) return;
			data = new RequestWsTree(fWriter).get(nameSpaceNode);
			if (data == null)
				return;
			classes = data.getJSONArray("classes");
			names = data.getJSONArray("names");
			err = data.getString("err");
			if (err.length() > 0) {
				
			}
			if (names != null)
				len = names.length();
			List <String> varNames = new ArrayList<String>();
			for (int i = 0; i < len; i++) {
				if ((int) classes.getDouble(i) == 2)
					varNames.add(names.getString(i));
			}
			len = varNames.size();
			IVariable[] newGlobalVariables = new IVariable[len];
			synchronized (fGlobalVariables) {
				fGlobalVariables.clear();
				for (int i = 0; i < len; i++) {
//					JSONObject cmdVal = new RequestValueTip(fWriter).get(0, varNames.get(i), 0, 0, 64, 32);
					JSONObject cmdVal = new RequestValueTip(fWriter).get(0, varNames.get(i), 0, 0, 128, 256);
					JSONArray tip = cmdVal.getJSONArray("tip");
					int tipLen = tip.length();
					String val = "";
					for (int k = 0; k < tipLen; k++) {
						if (k > 0)
							val += "\n";
						val += tip.getString(k);
					}
//					String val = tip.toString();
					APLVariable variable = new APLVariable(this, varNames.get(i));
					fGlobalVariables.put(variable, new APLValue(this, val, variable));
				}
			}
		} catch (JSONException e) {
			APLDebugCorePlugin.log(IStatus.ERROR, "Field not present " + data.toString(), e);
//			fGlobalVariables = new IVariable[0];
			return;
		}

	}
	
	public String getValueTip(final String functionName, String line, int pos, int maxWidth, int maxHeight) {
		EntityWindow entityWin = entityWindowsStack.getDebugEntity(functionName, -1);
		if (entityWin == null) {
			entityWin = entityWindowsStack.getEntity(functionName);
		}
		if (entityWin == null) {
			final ReplyEditRequest request = new ReplyEditRequest(this);
			Runnable valueOnOpen = new Runnable() {
				@Override
				public void run() {
					request.put(entityWindowsStack.getEntity(functionName));
				}
			};
			entityWindowsStack.addOnOpenAction(functionName, valueOnOpen);
			entityWin = request.get(0, 0, functionName);
			if (entityWin == null) {
				return null;
			}
		}
		int token = entityWin.token;
		JSONArray cmd = new JSONArray();
		cmd.put(0, "GetValueTip");
		JSONObject val = new JSONObject();
		val.put("win", entityWin.token);
		val.put("line", line);
		val.put("pos", pos);
		val.put("token", token);
		val.put("maxWidth", maxWidth);
		val.put("maxHeight", maxHeight);
		cmd.put(1, val);
		JSONArray ans = new ReplyRequest(this).get(cmd);
		if (ans != null) {
			try {
				JSONObject ansVal = ans.getJSONObject(1);
				int classType = ansVal.getInt("class");
				int endCol = ansVal.getInt("endCol");
				int startCol = ansVal.getInt("startCol");
				JSONArray tip = ansVal.getJSONArray("tip");
				int ansToken = ansVal.getInt("token");
				if (ansToken == token) {
					int tipLen = tip.length();
					String value = "";
					for (int i = 0; i < tipLen; i++) {
						if (i > 0)
							value += "\n";
						value += tip.getString(i);
					}
					return value;
				}
			} catch (JSONException e) {
				
			}
		}
		return null;
	}
	
	/**
	 * Process debug target event
	 */
	public synchronized void handleEvent(APLEvent _event) {
		if (_event instanceof CIPEvent) {
			CIPEvent event = (CIPEvent) _event;
			EntityWindow entityWin = entityWindowsStack.getEntity(event.win);
			if (entityWin == null) {
				return;
			}
//			APLThread thread = fThreads.get(entityWin.getThreadId());
			APLThread thread = getThread(entityWin.getThreadId());
//			thread.name = entityWin.tname;
			this.currentThread = thread;
			entityWin.setLineNumer(event.line);
			thread.setTopStackWindowId(entityWin.token);
			ReplyStackRequest request = new ReplyStackRequest(this, entityWin.getThreadId());
			Runnable onReceiveStack = new Runnable() {

				@Override
				public void run() {
//					StackData stackData = debugTarget.getCommandProc().getStackData(threadId)
					request.put(true);
				}
				
			};
			getCommandProc().addOnReceiveStackAction(entityWin.getThreadId(), onReceiveStack);

			Boolean stackDataReceived = request.get();
			if (stackDataReceived == null) {
				System.out.println("Can't recieve stack frame");
			}

			// Ask for stack frame
//			try {
//				JSONArray cmd = new JSONArray();
//				cmd.put(0, "GetSIStack");
//				JSONObject val = new JSONObject();
//				cmd.put(1, val);
//				JSONArray ans = new ReplyRequest(getDebugTarget()).get(cmd);
//				if (ans != null) {
//					JSONObject ansVal = ans.getJSONObject(1);
//					JSONArray stack = ansVal.getJSONArray("stack");
//					int tid = ansVal.getInt("tid");
//					if (tid == thread.getIdentifier()) {
//						// already stack updated
////						thread.setStackFrame(stack);
//					}
//				}
				if (err == 1001) {
					thread.handleEvent("suspended breakpoint");
					err = 0;
					dmx = 0;
				} else {
					thread.handleEvent("suspended client");
				}
//			} catch (JSONException e) {
//				
//			}
			setNeedUpdateGlobalVariables();
		}
		else if (_event instanceof FocusEvent) {
			FocusEvent event = (FocusEvent) _event;
		}
//		else if (_event )
	}
	
	/**
	 * Update threads stack frame, if needed create thread
	 */
	public void setStackFrame(JSONArray cmd) {
		try {
			JSONObject val = cmd.getJSONObject(1);
			JSONArray stack = val.getJSONArray("stack");
			int tid = val.getInt("tid");
			APLThread thread = getThread(tid);
			if (thread == null) {
				thread = CreateThread(tid,"Tid:"+tid, true);
			}
			thread.setStackFrame(stack);
			if (!thread.isSuspended())
				thread.handleEvent("suspended client");
		} catch (JSONException e) {
			
		}
	}
	
	public void handleEvent(String event) {
		if (event.equals("started")) {
			vmStarted();
			started(); // when new thead started
		} else if (event.equals("terminated")) {
			try {
				terminate();
			} catch (DebugException e) {
			}
			vmTerminated();
		} else if (event.startsWith("VMresumed")) {
			vmResumed(event);
		}
	}

	/**
	 * When the breakpoint manager disables, remove all registered breakpoints
	 * request from the VM/ When it enables, reinstall them.
	 */
	public void breakpointManagerEnablementChanged(boolean enabled) {
		IBreakpoint[] breakpoints = getBreakpointManager().getBreakpoints(getModelIdentifier());
		for (int i = 0; i < breakpoints.length; i++) {
			if (breakpoints[i] instanceof APLLineBreakpoint) {
//				APLLineBreakpoint breakpoint =  (APLLineBreakpoint) breakpoints[i];
				if (enabled) {
					breakpointAdded(breakpoints[i]);
				} else {
					breakpointRemoved(breakpoints[i], null);
				}
			}
		}
	}
	
//	/**
//	 * Returns whether poping the data stack is currently permitted
//	 * 
//	 * @return whether poping the data stack is currently permitted
//	 */
//	public boolean canPop() {
//		try {
//			return !isTerminated() && isSuspended() && getDataStack().length > 0;
//		} catch (DebugException e) {
//			
//		}
//		return false;
//	}

//	/**
//	 * Pops and returns the top of the data stack
//	 * 
//	 * @return the top value on the stack
//	 * @throws DebugException if the stack is empty or request fails
//	 */
//	public IValue pop() throws DebugException {
//		IValue[] dataStack = getDataStack();
//		if (dataStack.length > 0) {
//			sendRequest("popdata");
//			return dataStack[0];
//		}
//		requestFailed("Empty stack", null);
//		return null;
//	}
	
	public IProcess getProcess() {
		return fProcess;
	}

	public IThread[] getThreads() throws DebugException {
		synchronized (fThreads) {
			return fThreads.values().toArray(new IThread[fThreads.size()]);
		}
//				return fThreads;
	}

	public boolean hasThreads() throws DebugException {
		return fThreads.size() > 0;
//		return false;
	}

	public String getName() throws DebugException {
		return fName;
	}

	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		if (!isTerminated() && breakpoint.getModelIdentifier().equals(getModelIdentifier())) {
//				String program = getLaunch().getLaunchConfiguration()
//						.getAttribute(DebugCorePlugin.ATTR_PROJECT, (String) null);
			if (fProject != null) {
				IResource resource = null;
				if (breakpoint instanceof APLRunToLineBreakpoint) {
					APLRunToLineBreakpoint rtl = (APLRunToLineBreakpoint) breakpoint;
					resource = rtl.getSourceFile();
				} else {
					IMarker marker = breakpoint.getMarker();
					if (marker != null) {
						resource = marker.getResource();
					}
				}
				if (resource != null) {
					if (fProject.equals(resource.getProject())) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public IDebugTarget getDebugTarget() {
		return this;
	}
	
	public ILaunch getLaunch() {
		return fLaunch;
	}

	/**
	 * Returns this debug target's single thread, or <code>null</code>
	 * if terminated.
	 * 
	 * @param threadID ID of the thread to return, or <code>0</code>
	 * to return the first available thread
	 * @return this debug target's single thread, or <code>null</code>
	 * if terminated
	 */
//	protected synchronized APLThread getThread() {
//		return fThread;
//	}
	public APLThread getThread(int threadId) {
		if (threadId >= 0) {
			return fThreads.get(new Integer(threadId));
		} else {
			synchronized(fThreads) {
				if (fThreads.size() > 0) {
					return fThreads.values().iterator().next();
				}
			}
		}
		return null;
	}

	public void updateThread(int tid, String tname) {
		APLThread thread = getThread(tid);
		if (thread == null) {
			CreateThread(tid, tname, true);
		}
		else
			thread.setName(tname);
	}
	
	private APLThread CreateThread(int tid, String tname, boolean suspended) {
		APLThread thread = new APLThread(this, tid, tname, suspended);
		fThreads.put(tid, thread);
		thread.start();
		return thread;
	}
	
	public void RemoveThread(int tid) {
		APLThread thread = getThread(tid);
		if (thread != null) {
			fThreads.remove(tid);
			thread.exit();
		}
	}
	

	
	@Override
	public void launchRemoved(ILaunch launch) {
		IDebugTarget target = launch.getDebugTarget();
		if (target != null && target instanceof APLDebugTarget
				&& target.equals(this)) {
			APLDebugTarget debugTarget = (APLDebugTarget) target;
			if (!debugTarget.isTerminated()) {
				try {
					debugTarget.terminate();
				} catch (DebugException e) {
				}
			}
			debugTarget.removeSessionConsole();
		}
	}

	@Override
	public void launchAdded(ILaunch launch) {
		
	}

	@Override
	public void launchChanged(ILaunch launch) {
		
	}

	public void startTransmission(Socket socket) throws IOException {
		this.fSocket = socket;
		this.fWriter = new DebuggerWriter(socket, this);
		this.fCommandProc = new CommandProcessing(fWriter, this);
		this.fReader = new DebuggerReader(socket, this);
		this.consoleInterpreter.setDebuggerWriter(fWriter);
		this.consoleInterpreter.setCommandsProcessing(fCommandProc);
		Thread t; 
		t = new Thread(fReader, "apl.reader");
		t.start();
		t = new Thread(fWriter, "apl.writer");
		t.start();
		t = new Thread(fCommandProc, "apl.commands");
		t.start();
		
	}
	
	public CommandProcessing getCommandProc() {
		return fCommandProc;
	}

	/**
	 * Send message to Interpreter
	 */
	public void postCommand(String cmd) {
		fWriter.postCommand(cmd);
	}
	/**
	 * Send message to Interpreter from RIDE console
	 */
	public void postRIDECommand(String cmd) {
		if (!isDisconnected())
			fWriter.postRIDECommand(cmd);
	}
	/**
	 * Send message to Interpreter from Session console
	 */
	public void postSessionPrompt(String prompt) {
		if (!isDisconnected())
			fWriter.postSessionPrompt(prompt);
	}
	
//	public void initialize() {
//		fWriter.postCommand("SupportedProtocols=2");
//		fWriter.postCommand("UsingProtocol=2");
//		fWriter.postCommand("[\"Identify\",{\"identity\":1}]");
//		fWriter.postCommand("[\"Connect\",{\"remoteId\":2}]");
//		fWriter.postCommand("[\"GetWindowLayout\",{}]");
//		fWriter.postCommand("[\"SetPW\",{\"pw\":80}]");
//
//		
//	}

	public void setConsoleInterpter(AplDevConsoleInterpreter consoleInterpreter) {
		this.consoleInterpreter = consoleInterpreter;
	}
	
	public EntityWindowsStack getEntityWindows() {
		return entityWindowsStack;
	}

	/**
	 * Open source editor. Usually called by Shift-Enter from console or editor window
	 * Must called in UI thread
	 * 
	 * @param win token id opened by interpreter 
	 * @param pos cursor position
	 * @param text line which contain entity name
	 */
	public void EditEntity(int win, int pos, String text) {
		
//		getInterpreterWriter().postEdit(0, pos, text);
		JSONArray cmd = new JSONArray();
		cmd.put(0, "Edit");
		JSONObject val = new JSONObject();
		int tokenId = 0;
		val.put("win", tokenId);
		val.put("pos", pos);
		val.put("text", text);
		val.put("unsaved", new JSONObject());
		cmd.put(1, val);
		JSONArray ans = new ReplyRequest(getDebugTarget()).get(cmd);
		JSONObject ansVal;
		if (ans != null) {
			int token = -1;
			try {
				String cmdName = ans.getString(0);
				ansVal = ans.getJSONObject(1);
				if (cmdName.equals("OpenWindow") || cmdName.equals("UpdateWindow")) {
					token = ansVal.getInt("token");
				} else if (cmdName.equals("GotoWindow")) {
					token = ansVal.getInt("win");
				}
			} catch (JSONException e) {
				
			}
			if (token != -1) {
				// open interpreter window in editor
				EntityWindow entity = getEntityWindows().getEntity(token);
				if (entity == null)
					return;
				// get the page
				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
				IWorkbenchPage page;
				if (window != null) {
					page = window.getActivePage();
				} else
					return;

				// Check if entity source equal to file form Project
				IResource resource = fProject.findMember(entity.name + "." + APLDebugCorePlugin.MODULES_EXTENSION);
				if (resource != null && resource instanceof IFile) {
					
//					IFileStore fileToOpen = EFS.getLocalFileSystem()
//							.getStore(((File) resource).getLocationURI());
//					IEditorInput input = new FileStoreEditorInput(fileToOpen);
					try {
//						IDE.openEditorOnFileStore(page, fileToOpen);
						IDE.openEditor(page, (IFile) resource);
//						page.openEditor(input, DebugCorePlugin.FUNCTION_EDITOR_ID, true);
					} catch (PartInitException e) {
						
					}
				} else {
					// entity havn't file source
					WorkspaceEditorInput input = new WorkspaceEditorInput(entity, this);
					try {
//						IDE.openEditorOnFileStore(page, fileToOpen);
//						IDE.openEditor(page, (IFile) resource);
						page.openEditor(input, APLDebugCorePlugin.FUNCTION_EDITOR_ID, true);
					} catch (PartInitException e) {
						
					}
				}
			}
		}
	}

	public void setStackFrame(StackData stackData) {
		APLThread thread = getThread(stackData.getThreadId());
		if (thread == null) {
			// create new thread
			if (thread == null) {
				EntityWindow entityWin = getEntityWindows()
						.getThreadEntity(stackData.getThreadId());
				thread = CreateThread(tid,entityWin.getThreadName(), true);
			}
		}
		thread.setFramesData(stackData.getData());
		if (thread.isSuspended()) {
			try {
				thread.computeStackFrames(true);
				thread.fireSuspendEvent(DebugEvent.CONTENT);
			} catch (DebugException e) {
			}
		} else {
			thread.fireSuspendEvent(DebugEvent.CLIENT_REQUEST);
//			thread.handleEvent("suspended client");
		}
	}
	
}
