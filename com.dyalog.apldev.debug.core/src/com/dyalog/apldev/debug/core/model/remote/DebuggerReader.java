package com.dyalog.apldev.debug.core.model.remote;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.console.SessionConsole;
import com.dyalog.apldev.debug.core.model.APLDebugTarget;

public class DebuggerReader implements Runnable {
	private Socket fSocket;
	private InputStream fInput;
//	private InputStreamReader fRequestReader;
	private volatile boolean done;
	private APLDebugTarget fDebugTarget;
	private SessionConsole fConsoles;
	private Object lock = new Object();
	private int sequence = 0;
	
//	private MagicNum fMagicNum;

	/**
	 * commands waiting for responce. Their keys are the sequence
	 */
	private Dictionary<Integer, String> responseQueue = new Hashtable<>();
	private Command fCommand;
	private CommandProcessing fCommandProc;
	

	public DebuggerReader(Socket s, APLDebugTarget t) throws IOException {
		fSocket = s;
		fDebugTarget = t;
		fConsoles = fDebugTarget.getConsoles();
		fInput = fSocket.getInputStream();
		fCommand = new Command(fInput, fConsoles);
		fCommandProc = fDebugTarget.getCommandProc();
		
		
//		fRequestReader = new InputStreamReader(fInput);
//		fPart = Part.lookForMagicNum;
//		fMagicNum = new MagicNum();
	}

//	private void processCommand(String cmdLine) {
//
////		fConsoles.WriteRIDE("< " + cmdLine);
////		System.out.println(magicNum.Len + " < " + message);
////		Date date = new Date();
//////		Calendar. 
//////		fConsoles.WriteSession("--- hi ---");
////		fConsoles.WriteRIDE(date.getHours() + ":"
////				+ date.getMinutes() + ":"
////				+ date.getSeconds() + " < "
////				+ cmdLine);
//	}
	
	/**
	 * Commands which listen for response
	 * 
	 * @param cmd
	 */
	public void addToResponseQueue(String cmd) {
		sequence ++;
		synchronized (responseQueue) {
			responseQueue.put(sequence, cmd);
		}
	}
	
	/**
	 * Terminate socket reader Thread
	 */
	public void done() {
		done = true;
	}


	@Override
	public void run() {
		while (!done) {
			try {
//				String cmdLine = readLine();
//				if(fInput.available() > 0) {
//					int in = fInput.read();
					if (fCommand.readStream()) {
						fCommandProc.postCommand(fCommand.getCommand());
//						fCommandProc.parseCommand(fCommand.getCommand());
					}
//				}
//				processCommand(cmdLine);
//				synchronized (lock) {
//					Thread.sleep(50);
//				}
			} catch (SocketException e1) {
//				DebugCorePlugin.log(e1);
				done = true;
			} catch (IOException e2) {
				if (!e2.getMessage().equals("Done") 
						& !e2.getMessage().equals("Can't read socket"))
					APLDebugCorePlugin.log(e2);
				done = true;
			} catch (Exception e) {
				System.err.println(e);
//				done = true;
			}
		}
		
		if (done || fSocket == null || !fSocket.isConnected()) {
			if (fDebugTarget != null && !fDebugTarget.isDisconnected()) {
				try {
					fDebugTarget.disconnect("Connection lost");
				} catch (DebugException ed) {
					
				}
			}
			done = true;
		}
	}

}
/**
 * Constructs from byte sequence commands from Interpreted
 */
class Command {
	private String fCmd;
	private byte[] prefix = {0, 0, 0, 0, 0, 0, 0, 0};
	private byte[] payload = null;
	private boolean searchPrefix = true;
	// indicate first 8 byte for prefix
	private int first = 0;
	// length payload from prefix;
	private int fLen;
	// byte number in payload
	private int fIndex = 0;
	private boolean fLookforBrackets;
	private int fUnpairedBrackets;
	private SessionConsole fConsoles;
	private InputStream fInput;
	private boolean fStart = true;

	
	public Command (InputStream input, SessionConsole consoles) {
		fConsoles = consoles;
		fInput = input;
	}

	public boolean readStream() throws IOException {
		int num = 0;
		boolean ans = false;
		if (searchPrefix) {
			if (fStart) {
				num = fInput.read(prefix, 0, 8);
				if (num == -1)
					throw new IOException("Can't read socket");
				fStart = false;
				if (num == 8 && checkRide()) {
					retriveLen();
					if (fLen > 0) {
						payload = new byte[fLen];
						fIndex = 0;
						searchPrefix = false;
					}
				} 
			} else {
				// if some process send wrong package
				boolean search = true;
				while (search && fInput.available() > 0) {
					int in;
					if((in = fInput.read()) != -1)
						if (push((byte) in) > 0) {
							search = false;
							payload = new byte[fLen];
							fIndex = 0;
							searchPrefix = false;
						}
				}
			}
			if (!searchPrefix) {
				num = fInput.read(payload, 0, fLen - fIndex);
				fIndex += num;
				if (fIndex >= fLen) {
					fCmd = new String(payload, 0, fLen, StandardCharsets.UTF_8);
					fStart = true;
					searchPrefix = true;
					ans = true;
				}
			}
		}
		return ans;
	}
	
	/**
	 * @param in if equal -1 return <code>false</code>;
	 * 
	 * @return true if obtain new command
	 */
	public boolean pushStreamByte(int in) {
		if (in == -1)
			return false;
		boolean ans = false;
		if (searchPrefix) {
			int len = push((byte) in);
			if (len != -1 && len > 0) {
				searchPrefix = false;
				payload = new byte[len];
				fIndex = 0;
				ans = false;
			}
		} else {
			// parse payload in case if mistake in prefix length field
			if (fIndex == 0 && in == '[') {
				fLookforBrackets = true;
				fUnpairedBrackets = 1;
			}
			else if (fLookforBrackets && in == '[')
				fUnpairedBrackets++;
			else if (fLookforBrackets && in == ']')
				fUnpairedBrackets--;
			else if (fIndex > 0 && in == '"'
					&& payload[fIndex - 1] != '\\'
					&& payload[fIndex] == '[')
				fLookforBrackets ^= true;
			
			payload[fIndex++] = (byte) in;
			// stop reading if json array filled
			if (payload[0] == '[' && fUnpairedBrackets == 0) {
				if (fIndex != fLen) {
					fConsoles.WriteRIDE("Wrong length field. expected: "
							+ fLen + " achieved: " + fIndex);
				}
				fCmd = new String(payload, 0, fIndex, StandardCharsets.UTF_8);
				ans = true;
			}
					
			// stop reading if field length achieved or json array closed
			else if (fIndex >= payload.length
					&& payload[0] != '[') {
				fCmd = new String(payload, StandardCharsets.UTF_8);
				ans = true;
			}
			else if (fIndex >= payload.length
					&& payload[0] == '[' && fUnpairedBrackets > 0) {
				// increase buffer
				byte[] t = new byte[(int) (payload.length * 3 / 2)];
				for (int i = 0; i < payload.length; i++)
					t[i] = payload[i];
				payload = t;
			}
		}
		if (ans) {
			clearPrefix();
			fLookforBrackets = false;
			searchPrefix = true;
			fIndex = 0;
		}
		return ans;
	}
	/**
	 * Gets last parsed command string
	 * @return
	 */
	public String getCommand() {
		return fCmd;
	}
	/**
	 * Push numbers to 8-byte array 
	 * @param Num
	 * @return message <code>length</code> if 8-bytes contain RIDE magic number
	 * else <code>-1<code>
	 */
	private int push(byte Num) {
		if (first < 8) {
			prefix[first++] = Num;
		} else {
			for (int i=0; i<7; i++) {
				prefix[i] = prefix[i+1];
			}
			prefix[7] = Num;
		}
		if (first >= 7 && checkRide())
			return retriveLen()-8;
		else
			return -1;
		}
	
		boolean checkRide() {
			if(prefix[4] == 'R' 
					&& prefix[5] == 'I'
					&& prefix[6] == 'D'
					&& prefix[7] == 'E')
				return true;
			else
				return false;
		}

		int retriveLen() {
			int len = 0;
			for (int i=0; i<4; i++) {
				len += ((int) prefix[i] & 0xFF) << 8*(3-i);
			}
			fLen = len - 8;
			return len;
		}

		private void clearPrefix() {
			first = 0;
			for(int i=0; i<8; i++) {
				prefix[i] = 0;
			}
		}

}

//private void readByte(int in) {
//		if (in == -1)
//			return;
//		fPayload=null;
//		int index = 0;
//		fPart = Part.lookForMagicNum;
//		fMagicNum.clear();
//		int in;
//		int unpairedBrackets = 0;
//		boolean lookforBrackets = false;
////		while ((in = fRequestReader.read()) != -1) {
//		while (!done) {
//			
//			switch (fPart) {
//			case lookForMagicNum:
//				int len = fMagicNum.push((byte) in);
//				if (len != -1 && len > 8) {
//					fPart = Part.readMsg;
//					payload = new byte[len - 8];
////					payload.ensureCapacity(len-8);
//					index = 0;
//				}
//				break;
//			case readMsg:
//				if (index == 0 && in == '[') {
//					lookforBrackets = true;
//					unpairedBrackets = 1;
//				}
//				else if (lookforBrackets && in == '[')
//					unpairedBrackets++;
//				else if (lookforBrackets && in == ']')
//					unpairedBrackets--;
//				else if (index > 0 && in == '"'
//						&& payload[index - 1] != '\\'
//						&& payload[index] == '[')
//					lookforBrackets ^= true;
//				
//				payload[index++] = (byte) in;
//				// stop reading if json array filled
//				if (payload[0] == '[' && unpairedBrackets == 0) {
//					if (index != fMagicNum.len() - 8) {
//						fConsoles.WriteRIDE("Wrong length field. expected: "
//								+ (fMagicNum.len() - 8) + " achieved: " + index);
//					}
//					return new String(payload, 0, index, StandardCharsets.UTF_8);
//				}
//						
//				// stop reading if field length achieved or json array closed
//				else if (index >= payload.length
//						&& payload[0] != '[') {
//					return new String(payload, StandardCharsets.UTF_8);
//				}
//				else if (index >= payload.length
//						&& payload[0] == '[' && unpairedBrackets > 0) {
//					// increase buffer
//					byte[] t = new byte[(int) (payload.length * 3 / 2)];
//					for (int i = 0; i < payload.length; i++)
//						t[i] = payload[i];
//					payload = t;
//				}
//				break;
//			}
//		}
//
//		throw new IOException("Done");
//	}
//
//}

/**
 * Search for magic number in message from Interpreter
 */
//class MagicNum {
//	byte[] magicNum = {0, 0, 0, 0, 0, 0, 0, 0};
//	private int fLen=-1;
//	private int first = 0;
//
//	MagicNum() {
//		
//	}
//	int len() {
//		return fLen;
//	}
//	
//	MagicNum(int len) {
//		setLen(len);
//		magicNum[4] = 'R'; 
//		magicNum[5] = 'I';
//		magicNum[6] = 'D';
//		magicNum[7] = 'E';
//	}
//	byte[] getMagicNum() {
//		return magicNum;
//	}
//	/**
//	 * Push numbers to 8-byte array 
//	 * @param Num
//	 * @return message <code>length</code> if 8-bytes contain RIDE magic number
//	 * else <code>-1<code>
//	 */
//	int push(byte Num) {
//		if (first < 8) {
//			magicNum[first++] = Num;
//		} else {
//			for (int i=0; i<7; i++) {
//				magicNum[i] = magicNum[i+1];
//			}
//			magicNum[7] = Num;
//		}
//		if (first >= 7 && checkRide())
//			return retriveLen();
//		else
//			return -1;
//		}
//		boolean checkRide() {
//			if(magicNum[4] == 'R' 
//					&& magicNum[5] == 'I'
//					&& magicNum[6] == 'D'
//					&& magicNum[7] == 'E')
//				return true;
//			else
//				return false;
//		}
//
//		int retriveLen() {
//			int len = 0;
//			for (int i=0; i<4; i++) {
//				len += ((int) magicNum[i] & 0xFF) << 8*(3-i);
//			}
//			fLen = len;
//			return len;
//		}
//
//		void clear() {
//			first = 0;
//			for(int i=0; i<8; i++) {
//				magicNum[i] = 0;
//			}
//		}
//	/**
//	 * Convert int to 4 byte array
//	 */
//	void setLen(int len) {
//		for (int i=3; len != 0 && i >= 0; i--) {
//			magicNum[i] = (byte) (len & 0xFF);
//			len = len >> 8;
//		}
//		fLen = len;
//	}
//}
