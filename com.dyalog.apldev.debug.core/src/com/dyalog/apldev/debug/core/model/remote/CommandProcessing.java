package com.dyalog.apldev.debug.core.model.remote;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.model.APLDebugTarget;
import com.dyalog.apldev.debug.core.protocol.APLEvent;
import com.dyalog.apldev.debug.core.protocol.CIPEvent;
import com.dyalog.apldev.interactive_console.console.ICallback;
import com.dyalog.apldev.interactive_console.console.InterpreterResponse;
import com.dyalog.apldev.interactive_console.console.SessionConsole;
import com.dyalog.apldev.interactive_console.content.Tuple;
import com.dyalog.apldev.log.Log;

public class CommandProcessing implements Runnable {
	
	private List<String> cmdQueue = new ArrayList<>();
	private boolean done;
	Calendar now;
	private SessionConsole fConsoles;
	private boolean fInit = true;
	private DebuggerWriter fWriter;
	private APLDebugTarget fDebugTarget;
	ICallback <Object, Tuple<String, String>> onContentsReceived;
	ICallback <Object, InterpreterResponse> onResponseReceived;
	ICallback <Object, String> onEchoInput;
	private String fInfo;
	public static final String EMPTY = "";
	/**
	 * List with action after receiving stack frame data
	 */
	private Map <Integer, Runnable[]> fOnReceiveStackAction = Collections.synchronizedMap(
			new LinkedHashMap <Integer, Runnable[]> ());

	private Map <Integer, RequestWsTree> fRequestTreeList = Collections.synchronizedMap(
			new LinkedHashMap <Integer, RequestWsTree>());
	private Map <Integer, RequestValueTip> fRequestValueTipList = Collections.synchronizedMap(
			new LinkedHashMap <Integer, RequestValueTip>());
	private Map <Integer, RequestEdit> fRequestOpenWindow = Collections.synchronizedMap(
			new LinkedHashMap <Integer, RequestEdit>());
	private Map <Integer, RequestSave> fRequestReplySaveChanges = Collections.synchronizedMap(
			new LinkedHashMap <Integer, RequestSave>());
	private Map <Integer, RequestClose> fRequestCloseWindow = Collections.synchronizedMap(
			new LinkedHashMap <Integer, RequestClose>());

	private List <ReplyRequest> fReplyRequests = 
			Collections.synchronizedList(new ArrayList <ReplyRequest>());

	public CommandProcessing (DebuggerWriter writer, APLDebugTarget target) {
		fDebugTarget = target;
		fConsoles = fDebugTarget.getConsoles();
		fWriter = writer;

	}
	
	public void setOnContentsReceived (ICallback <Object, Tuple<String, String>> onContentsReceived) {
		this.onContentsReceived = onContentsReceived;
		if (fInfo != null)
			onContentsReceived.call(new Tuple<String, String> (fInfo, EMPTY));
	}

	public void setOnResponseReceived(ICallback<Object, InterpreterResponse> onResponseReceived) {
		this.onResponseReceived = onResponseReceived;
	}
	
	public void setOnEchoInput(ICallback< Object, String> onEchoInput) {
		this.onEchoInput = onEchoInput;
	}
	
	/**
	 * Add commands from Interpreter for processing
	 */
	public void postCommand (String cmd) {
		synchronized (cmdQueue) {
			cmdQueue.add(cmd);
		}
	}

	void parseCommand(String cmd) {
		try {
			JSONArray cmdJ = new JSONArray(cmd);
			String cmdName = (String) cmdJ.get(0);
			JSONObject cmdVal = (JSONObject) cmdJ.get(1);
			String res;
//			if (cmdName != null && cmdName.length() > 0) {
//				fDebugTarget.EventListeners(cmdName, cmdVal);
//			}
//				APLEvent event = null;
//				try {
//					event = APLEvent.parseEvent(cmdName, cmdVal);
//				} catch (IllegalArgumentException e) {
//					DebugCorePlugin.log(e);
//				}
//			}
//			Object[] listeners = fDebugTarget.getEventListeners().toArray();
//			for (int i = 0; i < listeners.length; i++) {
//				((IAPLEventListener) listener[i]).handleEvent(event);
//			}
			ReplyRequest request;
			switch (cmdName) {
			case "AppendSessionOutput":
				res = cmdVal.get("result").toString();
				if (onContentsReceived != null)
					onContentsReceived.call(new Tuple<String, String>(res, EMPTY));
//				fConsoles.WriteSession(res);
				break;
			case "SetPromptType":
//				InterpreterResponce type;
				int type = cmdVal.getInt("type");
				if (type == 1 && !fDebugTarget.getStarted()) {
					// Initialization debug elements without blocking CommandPoc 
					new DebugTargetEvent("started");
				}
				if (onResponseReceived != null)
					onResponseReceived.call(new InterpreterResponse(type));
				break;
			case "EchoInput":
				res = cmdVal.getString("input");
				if (onEchoInput != null)
					onEchoInput.call(res);
				break;
			case "Identify":
				this.fInfo = "version: " + cmdVal.get("version")+"\n";
				if (onContentsReceived != null)
					onContentsReceived.call(new Tuple<String, String>(fInfo, EMPTY));
				// Initialization debug elements without blocking CommandPoc 
//				new DebugTargetEvent("started");
				
				// get Information about opened windows
				JSONArray getLayout = new JSONArray();
				getLayout.put(0,"GetWindowLayout");
				getLayout.put(1, new JSONObject());
				fDebugTarget.getInterpreterWriter().postCommand(getLayout.toString());
				break;
			case "UpdateDisplayName":
				if (!fDebugTarget.getStarted()) {
					// some times after starting Interpreter don't send SetPromptType
					fDebugTarget.getInterpreterWriter().postCanAcceptInput();
				}
				break;
			case "CanAcceptInput":
				int canAcceptInput = cmdVal.getInt("canAcceptInput");
				if (canAcceptInput == 1 && ! fDebugTarget.getStarted()) {
					new DebugTargetEvent("started");
					break;
				}
			case "ReplyTreeList":
				processReplyTreeList(cmdVal);
				break;
			case "ValueTip":
				CheckIfReply(cmdJ, "GetValueTip");
				processReplyValueTip(cmdVal);
				break;
			case "OpenWindow":
				try {
					int currentRow = cmdVal.getInt("currentRow");
					boolean debugger = cmdVal.getInt("debugger") == 1 ? true : false;
					int entityType = cmdVal.getInt("entityType");
					String name = cmdVal.getString("name");
					int offset = cmdVal.getInt("offset");
					boolean readOnly = cmdVal.getInt("readOnly") == 1 ? true : false;
					int size = cmdVal.getInt("size");
					JSONArray stops = cmdVal.getJSONArray("stop");
					int[] stop = new int[stops.length()];
					Iterator <Object> itr = stops.iterator();
					for (int i = 0; itr.hasNext() && i < stop.length; i++) {
						Object ob = itr.next();
						if (ob instanceof Integer)
							stop[i] = (int) ob;
						else
							stop[i] = -1;
					}
					JSONArray texts = cmdVal.getJSONArray("text");
					String[] text = new String[texts.length()];
					itr = texts.iterator();
					for (int i = 0; itr.hasNext() && i < text.length; i++) {
						Object ob = itr.next();
						if (ob instanceof String)
							text[i] = (String) ob;
						else
							text[i] = null;
					}
					int tid = cmdVal.getInt("tid");
					String tname = cmdVal.getString("tname");
					int token = cmdVal.getInt("token");

					EntityWindow entityWin = new EntityWindow(currentRow, debugger, entityType, name, offset, readOnly, size,
							stop, text, tid, tname, token);
					fDebugTarget.getEntityWindows().addEntityWindow(token, entityWin, false);
				} catch (JSONException e) {
					
				}
				CheckIfReply(cmdJ, "Edit");
				processOpenWindow(cmdVal);
				break;
			case "UpdateWindow":
				try {
					int currentRow = cmdVal.getInt("currentRow");
					boolean debugger = cmdVal.getInt("debugger") == 1 ? true : false;
					int entityType = cmdVal.getInt("entityType");
					String name = cmdVal.getString("name");
					int offset = cmdVal.getInt("offset");
					boolean readOnly = cmdVal.getInt("readOnly") == 1 ? true : false;
					int size = cmdVal.getInt("size");
					JSONArray stops = cmdVal.getJSONArray("stop");
					int[] stop = new int[stops.length()];
					Iterator <Object> itr = stops.iterator();
					for (int i = 0; itr.hasNext() && i < stop.length; i++) {
						Object ob = itr.next();
						if (ob instanceof Integer)
							stop[i] = (int) ob;
						else
							stop[i] = -1;
					}
					JSONArray texts = cmdVal.getJSONArray("text");
					String[] text = new String[texts.length()];
					itr = texts.iterator();
					for (int i = 0; itr.hasNext() && i < text.length; i++) {
						Object ob = itr.next();
						if (ob instanceof String)
							text[i] = (String) ob;
						else
							text[i] = null;
					}
					int tid = cmdVal.getInt("tid");
					String tname = cmdVal.getString("tname");
					int token = cmdVal.getInt("token");

					EntityWindow entityWin = new EntityWindow(currentRow, debugger, entityType, name, offset, readOnly, size,
							stop, text, tid, tname, token);
					fDebugTarget.getEntityWindows().addEntityWindow(token, entityWin, true);
				} catch (JSONException e) {
					
				}

				break;
			case "GotoWindow":
				int win = cmdVal.getInt("win");
				fDebugTarget.getEntityWindows().GotoWindowAction(win);
				CheckIfReply(cmdJ, "Edit");
				break;
			case "ReplySaveChanges":
				if (fReplyRequests.size() > 0) {
					request = fReplyRequests.get(0);
					if (request != null && "SaveChanges".equals(request.getCmdName())) {
						fReplyRequests.remove(0);
						request.put(cmdJ);
					}
				}
				processSaveChanges(cmdVal);
				break;
			case "CloseWindow":
				try {
					int closeWin = cmdVal.getInt("win");
					fDebugTarget.getEntityWindows().remove(closeWin);
				} catch (JSONException e) {
					
				}
				CheckIfReply(cmdJ, "CloseWindow");
				processCloseWindow(cmdVal);
				break;
			case "WindowTypeChanged":
				WindowTypeChanged(cmdVal);
				break;
			case "SetHighlightLine":
				int sourceWin = cmdVal.getInt("win");
				int line = cmdVal.getInt("line");
				new DebugTargetEvent(new CIPEvent(cmdJ, sourceWin, line));
				break;
			case "HadError":
				int err = cmdVal.getInt("error");
				int dmx = cmdVal.getInt("dmx");
				fDebugTarget.setLastError(err, dmx);
				break;
			case "FocusThread":
				int tid = cmdVal.getInt("tid");
				fDebugTarget.setCurrentThread(tid);
//				new DebugTargetEvent(new FocusEvent(cmdJ, tid));
				break;
			case "ReplyGetSIStack":
				int tidStk = cmdVal.getInt("tid");
				JSONArray stkData = cmdVal.getJSONArray("stack");
				int len = stkData.length();
				String[] data = new String[len];
				for (int i = 0; i < len; i++) {
					data[i] = stkData.getString(i);
				}
				StackData stackData = new StackData(data, tidStk);
				Runnable[] actions = fOnReceiveStackAction.remove(tidStk);
				if (actions != null && actions.length > 0) {
					for (Runnable action : actions) {
						action.run();
					}
				}
				fDebugTarget.setStackFrame(stackData);
//				fDebugTarget.setStackFrame(cmdJ);
				CheckIfReply(cmdJ, "GetSIStack");
				break;
			case "Disconnect":
				String message = cmdVal.getString("message");
				try {
					fDebugTarget.disconnect(message);
				} catch (DebugException e) {
					Log.log(IStatus.ERROR, "Error disconnecting debug target after 'Disconnect' message", e);
				}
				break;
			case "InvalidSyntax":
				break;
			case "SysError":
				String text = cmdVal.getString("text");
				try {
					fDebugTarget.disconnect("SysError: " + text);
				} catch (DebugException e) {
					Log.log(IStatus.ERROR, "Error disconnecting after 'SysError' message", e);
				}
			}
		} catch (JSONException e) {
			
		}
	}

	private void WindowTypeChanged(JSONObject cmdVal) throws JSONException {
		int chgWin = cmdVal.getInt("win");
		int tracer = cmdVal.getInt("tracer");
		fDebugTarget.getEntityWindows().changeTracer(chgWin, tracer);
	}

	/**
	 * Add action after opening window
	 */
	public void addOnReceiveStackAction(int threadId, Runnable onOpenAction) {
		synchronized (fOnReceiveStackAction) {
			Runnable[] actions = fOnReceiveStackAction.get(threadId);
			if (actions != null) {
				Runnable[] moreActions = new Runnable[actions.length + 1];
				System.arraycopy(actions, 0, moreActions, 0, actions.length);
				actions = moreActions;
			} else {
				actions = new Runnable[1];
			}
			actions[actions.length - 1] = onOpenAction;
			fOnReceiveStackAction.put(threadId, actions);
		}
	}

	private void CheckIfReply(JSONArray cmdJ, String cmdName) {
		synchronized (fReplyRequests) {
			if (fReplyRequests.size() > 0) {
				ReplyRequest request = fReplyRequests.get(0);
				if (request != null && cmdName.equals(request.getCmdName())) {
					fReplyRequests.remove(0);
					request.put(cmdJ);
				}
			}
		}
	}

	/**
	 * Set Reply handler
	 */
	void setReplyHandler(ReplyRequest replyRequest) {
		synchronized (fReplyRequests) {
			fReplyRequests.add(replyRequest);
		}
	}
	
	/**
	 * Remove reply handler if it no longer needed
	 */
	void removeReplyHandler(ReplyRequest replyRequest) {
		synchronized (fReplyRequests) {
			fReplyRequests.remove(replyRequest);
		}
	}

	
	class DebugTargetEvent implements Runnable {
		Thread thread;
		String stringEvent;
		APLEvent aplEvent;
		
		DebugTargetEvent(APLEvent event) {
			this.aplEvent = event;
			thread = new Thread(this, "APLEvent DebugTargetEvent from CommandProc");
			thread.start();
		}

		DebugTargetEvent(String event) {
			this.stringEvent = event;
			thread = new Thread(this, "String DebugTargetEvent from CommandProc");
			thread.start();
		}
		
		@Override
		public void run() {
			if (stringEvent != null)
				fDebugTarget.handleEvent(stringEvent);
			else
				fDebugTarget.handleEvent(aplEvent);
		}
	}
	
	private void processReplyTreeList(JSONObject cmdVal) {
		int replyNodeId;
		try {
			replyNodeId = cmdVal.getInt("nodeId");
		} catch (JSONException e) {
			return;
		}
		RequestWsTree requestWsTree;
		synchronized(fRequestTreeList) {
			requestWsTree = fRequestTreeList.remove(replyNodeId);
		}
		if (requestWsTree != null)
			requestWsTree.put(cmdVal);
	}

	private void processReplyValueTip(JSONObject cmdVal) {
		int replyStartCol;
		try {
			replyStartCol = cmdVal.getInt("startCol");
		} catch (JSONException e) {
			return;
		}
		RequestValueTip requestValueTip;
		synchronized(fRequestTreeList) {
			requestValueTip = fRequestValueTipList.remove(replyStartCol);
		}
		if (requestValueTip != null)
			requestValueTip.put(cmdVal);
	}

	private void processOpenWindow(JSONObject cmdVal) {
		int replyFun;
		try {
			replyFun = cmdVal.getInt("name");
		} catch (JSONException e) {
			return;
		}
		RequestEdit requestEdit;
		synchronized(fRequestOpenWindow) {
			requestEdit = fRequestOpenWindow.remove(replyFun);
		}
		if (requestEdit != null)
			requestEdit.put(cmdVal);
	}

	private void processSaveChanges(JSONObject cmdVal) {
		int replyWin;
		try {
			replyWin = cmdVal.getInt("win");
		} catch (JSONException e) {
			return;
		}
		RequestSave requestSave;
		synchronized(fRequestReplySaveChanges) {
			requestSave = fRequestReplySaveChanges.remove(replyWin);
		}
		if (requestSave != null)
			requestSave.put(cmdVal);
	}

	private void processCloseWindow(JSONObject cmdVal) {
		int replyWin;
		try {
			replyWin = cmdVal.getInt("win");
		} catch (JSONException e) {
			return;
		}
		RequestClose requestClose;
		synchronized(fRequestCloseWindow ) {
			requestClose = fRequestCloseWindow.remove(replyWin);
		}
		if (requestClose != null)
			requestClose.put();
	}

	/**
	 * Set ReplyTreeList handler
	 */
	void setTreeListRequestReceiver(RequestWsTree requestWsTree) {
		synchronized(fRequestTreeList) {
			fRequestTreeList.put(requestWsTree.getRequestNode(), requestWsTree);
		}
	}

	/**
	 * Set ReplyValueTip handler
	 */
	void setValueTipRequestReceiver(RequestValueTip requestValueTip) {
		synchronized(fRequestValueTipList) {
			fRequestValueTipList.put(requestValueTip.getRequestStartCol(), requestValueTip);
		}
	}
	
	/**
	 * Set OpenWindow handler
	 */
	public void setOpenReceiver(RequestEdit requestEdit) {
		synchronized(fRequestOpenWindow) {
			fRequestOpenWindow.put(requestEdit.getRequestText().hashCode(), requestEdit);
		}
	}

	/**
	 * Set ReplySaveChanges handler
	 */
	public void setSaveReceiver(RequestSave requestSave) {
		synchronized(fRequestReplySaveChanges) {
			fRequestReplySaveChanges.put(requestSave.getRequestWin(), requestSave);
		}
	}


	
	@Override
	public void run() {
		while (!done) {
			String cmd = null;
			synchronized (cmdQueue) {
				if (cmdQueue.size() > 0) {
					cmd = cmdQueue.remove(0);
				}
			}
				try {
					if (cmd != null) {
						fConsoles.appendReceiveRide(cmd, "");
						if (fInit) {
							if (cmd.equals("SupportedProtocols=2"))
								fWriter.postCommand("SupportedProtocols=2");
							if (cmd.equals("UsingProtocol=2")) {
								fWriter.postCommand("UsingProtocol=2");
								fWriter.postCommand("[\"Identify\",{\"identity\":1}]");
								fInit = false;
							}
						} else {
							parseCommand(cmd);
						}
					}
//					synchronized (lock) {
						Thread.sleep(50);
//					}
				} catch (Exception e) {
					Log.log(e);
				}
			}

	}


	/**
	 * Terminate command processing Thread
	 */
	public void done() {
		this.done = true;
	}

}
