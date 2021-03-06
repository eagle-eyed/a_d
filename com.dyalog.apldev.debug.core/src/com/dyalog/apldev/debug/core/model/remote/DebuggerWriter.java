package com.dyalog.apldev.debug.core.model.remote;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.model.APLDebugTarget;
import com.dyalog.apldev.interactive_console.console.SessionConsole;
import com.dyalog.apldev.log.Log;

public class DebuggerWriter implements Runnable {

	private Socket fSocket;
	private volatile boolean done;
	private APLDebugTarget fDebugTarget;
	private SessionConsole fConsoles;
	// lock object for sleeping
//	private Object lock  = new Object();
	private OutputStream fRequestWriter;
	private List<String> cmdQueue = new ArrayList<String>();

	public DebuggerWriter (Socket s, APLDebugTarget t) throws IOException {
		fSocket = s;
		fDebugTarget = t;
		fRequestWriter = fSocket.getOutputStream();
		fConsoles = t.getConsoles();
	}

	/**
	 * Send session prompt
	 */
	public void postSessionPrompt(String prompt) {
		JSONArray cmd = new JSONArray();
		cmd.put(0,"Execute");
		JSONObject val = new JSONObject();
		val.put("text", (String) prompt + "\n");
		val.put("trace", (int) 0);
		cmd.put(1, val);
		postCommand(cmd.toString());
		
		fDebugTarget.promptChanged();
}
	
	/**
	 * Read node for Workspace Explorer
	 * 
	 * @param node
	 * @param requestWsTree
	 * @return
	 */
	public boolean postGetTree (int node, RequestWsTree requestWsTree) {
		try {
			JSONArray cmd = new JSONArray();
			cmd.put(0, "TreeList");
			JSONObject val = new JSONObject();
			val.put("nodeId", node);
			cmd.put(1, val);
			fDebugTarget.getCommandProc().setTreeListRequestReceiver(requestWsTree);
			postCommand(cmd.toString());
		} catch (JSONException e) {
			Log.log(IStatus.ERROR, "Error parsing to JSON: postGetTree (" + node +")", e);
			return false;
		}
		return true;
	}

	/**
	 * Read ValueTip for hover window or variable view
	 * 
	 * @param win
	 * @param line
	 * @param pos
	 * @param token
	 * @param maxWidth
	 * @param maxHeight
	 * @return
	 */
	public boolean postGetValueTip(int win, String line,
			int pos, int token, int maxWidth, int maxHeight,
			RequestValueTip requestValueTip) {
		try {
			JSONArray cmd = new JSONArray();
			cmd.put(0, "GetValueTip");
			JSONObject val = new JSONObject();
			val.put("win", win);
			val.put("line", line);
			val.put("pos", pos);
			val.put("token", token);
			val.put("maxWidth", maxWidth);
			val.put("maxHeight", maxHeight);
			cmd.put(1, val);
			fDebugTarget.getCommandProc().setValueTipRequestReceiver(requestValueTip);
			postCommand(cmd.toString());
		} catch (JSONException e) {
			Log.log(IStatus.ERROR, "Error parsing to JSON: postGetValueTip (" + line +")", e);
			return false;
		}
		return true;
	}
	
	public boolean postEdit(int win, int pos, String text, RequestEdit requestEdit) {
		try {
			JSONArray cmd = new JSONArray();
			cmd.put(0, "Edit");
			JSONObject val = new JSONObject();
			val.put("win", win);
			val.put("pos", 0);
			val.put("text", text);
//			val.put("unsaved", new JSONArray());
			val.put("unsaved", new JSONObject());
			cmd.put(1, val);
			fDebugTarget.getCommandProc().setOpenReceiver(requestEdit);
			postCommand(cmd.toString());
		} catch (JSONException e) {
			Log.log(IStatus.ERROR, "Error parsing to JSON: postEdit (.., " + text +")", e);
			return false;
		}
		return true;
	}

	public boolean postSave(int win, List<String> text, int[] lineBreakpoint, RequestSave requestSave) {
		try {
			JSONArray cmd = new JSONArray();
			cmd.put(0, "SaveChanges");
			JSONObject val = new JSONObject();
			val.put("win", win);
			val.put("text", text);
			val.put("stop", lineBreakpoint);
			cmd.put(1, val);
			fDebugTarget.getCommandProc().setSaveReceiver(requestSave);
			postCommand(cmd.toString());
		} catch (JSONException e) {
			Log.log(IStatus.ERROR, "Error parsing to JSON: postSave (.., " + text + ",..)", e);
			return false;
		}
	return true;
	}

	public boolean postClose(int win, RequestClose requestClose) {
		try {
			JSONArray cmd = new JSONArray();
			cmd.put(0, "SaveChanges");
			JSONObject val = new JSONObject();
			cmd.put(1, val);
		} catch (JSONException e) {
			Log.log(IStatus.ERROR, "Error parsing to JSON: postClose (" + win + ")", e);
			return false;
		}
		return true;
	}



	/**
	 * Add command from RIDE console for processing
	 */
	public void postRIDECommand(String cmd) {
		if (cmd.equals("SupportedProtocols=2") ||
				cmd.equals("UsingProtocol=2"))
			postCommand(cmd);
		else {
			// check if correct JSON array
			try {
				JSONArray payload = new JSONArray(cmd);
				if (payload.length() == 2 
						& payload.get(0) instanceof String
						& payload.get(1) instanceof JSONObject)
					postCommand(payload.toString());
				else
					fConsoles.WriteRIDE("Must be [\"CommandName\",{JSON Object}]");
			} catch (JSONException e) {
				fConsoles.WriteRIDE(e.getMessage());
			}
		}
	}
	
	/**
	 * Add command for processing
	 */
	public void postCommand(String cmd) {
		postCommand(cmd, null);
	}
	/**
	 * Send command to Interpreter and set reply handler
	 * @param cmd
	 */
	public synchronized void postCommand(String cmd, ReplyRequest replyRequest) {
//		boolean sendInCmdQueueThred = false;
//		if (sendInCmdQueueThred) {
//			synchronized (cmdQueue) {
//				cmdQueue.add(cmd);
//	//			fConsoles.appendSendRide(cmd," queue");
//			}
//		}

		try {
			byte[] payload;
			// check if it's init commands
			if (cmd.equals("SupportedProtocols=2") ||
					cmd.equals("UsingProtocol=2"))
				payload = cmd.getBytes(StandardCharsets.UTF_8);
			else {
				JSONArray payloadJ = new JSONArray(cmd);
				String payloadStr = payloadJ.toString();
				payload = payloadStr.getBytes(StandardCharsets.UTF_8);
			}
			byte[] prefix = {0, 0, 0, 0, 'R', 'I', 'D', 'E'};
			setLengthField(prefix, payload.length + 8);
			try {
				if (replyRequest != null)
					fDebugTarget.getCommandProc().setReplyHandler(replyRequest);

				fRequestWriter.write(prefix);
				fRequestWriter.write(payload);
				fRequestWriter.flush();

				if (fConsoles != null && !fConsoles.isTerminated())
					fConsoles.appendSendRide(cmd,"");

			} catch (IOException e) {
				Log.log(e);
			}
		} catch (JSONException e) {
			Log.log(IStatus.ERROR, "Can't parse to JSON array: " + cmd, e);
		}
		
	}
	
	/**
	 * Terminate socket writer Thread
	 */
	public void done() {
		this.done = true;
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
//					String outgoing;
//					try {
//						outgoing = cmd.getOutgoing();
//						if (outgoing == null) {
//							continue;
//						}
//					} catch (Throwable e) {
//						continue;
//					}

					byte[] payload = cmd.getBytes(StandardCharsets.UTF_8);
					byte[] prefix = {0, 0, 0, 0, 'R', 'I', 'D', 'E'};
					setLengthField(prefix, payload.length + 8);
					
					fRequestWriter.write(prefix);
					fRequestWriter.write(payload);
					
					fRequestWriter.flush();
					
					fConsoles.appendSendRide(cmd, " II");
				}
//				synchronized (lock) {
					Thread.sleep(10);
//				}
			} catch (SocketException e0) {
				System.out.println("SocketException" + e0);
				try {
					fDebugTarget.terminate();
					done = true;
				} catch (DebugException e) {

				}
			} catch (InterruptedException | IOException e) {
				done = true;
			} catch (Throwable e1) {
				Log.log(IStatus.ERROR, "Exception in DebugWriter", e1);
			}
			if ((fSocket == null) || !fSocket.isConnected()) {
				done = true;
			}
		}
	}


	/**
	 * Convert int to 4 byte array
	 */
	void setLengthField(byte[] magicNum, int len) {
		for (int i=3; len != 0 && i >= 0; i--) {
			magicNum[i] = (byte) (len & 0xFF);
			len = len >> 8;
		}
	}

	/**
	 * Edit entity
	 * 
	 * @param win token id opened by interpreter which contain text line
	 * @param pos entity offset in line
	 * @param text line with entity
	 */
	public void postEdit(int win, int pos, String text) {
		if (win == 0 && pos == 0
				&& fDebugTarget.getEntityWindows().isOpening(text)) {
			// Request for opening already send
			return;
		}
		JSONArray cmd = new JSONArray();
		cmd.put(0, "Edit");
		JSONObject val = new JSONObject();
		val.put("win", win);
		val.put("text", text);
		val.put("pos", pos);
		val.put("unsaved", new JSONObject());
		cmd.put(1, val);
		postCommand(cmd.toString());
	}

	/**
	 * Close entity window
	 */
	public void postCloseWindow(int win) {
		EntityWindow entityWin = fDebugTarget.getEntityWindows().getEntity(win);
		if (entityWin != null && 
				! (entityWin.isDebug() && ! entityWin.isTracer())) {
			entityWin.setClosed();
		}
		JSONArray cmd = new JSONArray();
		cmd.put(0, "CloseWindow");
		JSONObject val = new JSONObject();
		val.put("win", win);
		cmd.put(1, val);
		postCommand(cmd.toString());
	}
	
	public void postTerminate() {
		JSONArray cmd = new JSONArray();
		cmd.put(0, "Execute");
		JSONObject val = new JSONObject();
		val.put("text", ")OFF\n");
		val.put("trace", 0);
		cmd.put(1, val);
		postCommand(cmd.toString());
	}

	public void postCanAcceptInput() {
		JSONArray cmd = new JSONArray();
		cmd.put(0, "CanSessionAcceptInput");
		cmd.put(1, new JSONObject());
		postCommand(cmd.toString());
	}
	
	public void postGetSIStack() {
		JSONArray cmd = new JSONArray();
		cmd.put(0, "GetSIStack");
		cmd.put(1, new JSONObject());
		postCommand(cmd.toString());
	}

	public void postSave(int token, String[] text, int[] stop) {
		JSONArray cmd = new JSONArray();
		cmd.put(0, "SaveChanges");
		JSONObject val = new JSONObject();
		val.put("win", token);
		val.put("text", text);
		val.put("stop", stop);
		cmd.put(1, val);
		postCommand(cmd.toString());
	}

	public void postSave(int token, String[] text, Integer[] stop) {
		JSONArray cmd = new JSONArray();
		cmd.put(0, "SaveChanges");
		JSONObject val = new JSONObject();
		val.put("win", token);
		val.put("text", text);
		val.put("stop", stop);
		cmd.put(1, val);
		postCommand(cmd.toString());
	}
	
	public void postCutback(int token) {
		JSONArray cmd = new JSONArray();
		cmd.put(0, "Cutback");
		JSONObject val = new JSONObject();
		val.put("win", token);
		cmd.put(1, val);
		postCommand(cmd.toString());
	}

	public void postStepReturn(int token) {
		JSONArray cmd = new JSONArray();
		cmd.put(0, "ContinueTrace");
		JSONObject val = new JSONObject();
		val.put("win", token);
		cmd.put(1, val);
		postCommand(cmd.toString());
	}

	public void postStepOver(int token) {
		JSONArray cmd = new JSONArray();
		cmd.put(0, "RunCurrentLine");
		JSONObject val = new JSONObject();
		val.put("win", token);
		cmd.put(1, val);
		postCommand(cmd.toString());
	}

	public void postStepInto(int token) {
		JSONArray cmd = new JSONArray();
		cmd.put(0, "StepInto");
		JSONObject val = new JSONObject();
		val.put("win", token);
		cmd.put(1, val);
		postCommand(cmd.toString());
	}

	public void postEdit(int win, int pos, String name, String[] strings) {
		JSONArray cmd = new JSONArray();
		cmd.put(0, "Edit");
		JSONObject val = new JSONObject();
		val.put("win", 0);
		val.put("pos", 0);
		val.put("text", name);
		val.put("unsaved", new JSONObject());
		cmd.put(1, val);
		postCommand(cmd.toString());
	}

	public void postResume(int token) {
		JSONArray cmd = new JSONArray();
		cmd.put(0, "Continue");
		JSONObject val = new JSONObject();
		val.put("win", token);
		cmd.put(1, val);
		postCommand(cmd.toString());
	}

	public void postLineAttributes(int token, int nLines, int[] stop, int[] trace, int[] monitor) {
		JSONArray cmdSet = new JSONArray();
		JSONObject valSet = new JSONObject();
		cmdSet.put(0, "SetLineAttributes");
		valSet.put("win", token);
		valSet.put("nLines", nLines);
		valSet.put("stop", stop);
		valSet.put("trace", trace);
		valSet.put("monitor", monitor);
		cmdSet.put(1, valSet);
		postCommand(cmdSet.toString());
	}
}
