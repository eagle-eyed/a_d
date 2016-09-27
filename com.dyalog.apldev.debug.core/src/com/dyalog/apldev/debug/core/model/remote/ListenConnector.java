package com.dyalog.apldev.debug.core.model.remote;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;

public class ListenConnector implements Runnable {

	protected volatile int timeout;
	protected ServerSocket serverSocket = null;
	protected Socket socket;
	protected Exception e;
	// Interpreter connects as Server and APLDev plugin as client
	private boolean fClient = true;
	private String fHost = "localhost";
	private int fPort = -1;

	public ListenConnector(int timeout) throws IOException {
		this.timeout = timeout;
		try {
//			serverSocket = SocketUtil.createLocalServerSocket();
			serverSocket = new ServerSocket(0);
			int localPort = serverSocket.getLocalPort();
			SocketUtil.checkValidPort(localPort);
			fPort = localPort;
			serverSocket.close();
		} catch (IOException e) {
			APLDebugCorePlugin.log(IStatus.ERROR, "Error when search free server socket.", e);
			throw e;
		}
	}

	public ListenConnector(int timeout, int localPort) throws IOException {
		this.timeout = timeout;
		this.fPort = localPort;
		if (fClient)
			return;
		try {
			serverSocket = SocketUtil.createLocalServerSocket(fPort);
		} catch (BindException e) {
			// try connect as client
			try {
				socket = new Socket("localhost", localPort);
				fClient  = true;
			} catch (UnknownHostException e1) {
//				requestFailed("Unable to connect to APL VM", e);
				APLDebugCorePlugin.log(IStatus.ERROR, "Unable to connect to APL VM", e1);
				throw e1;
			}
		} catch (IOException e) {
			APLDebugCorePlugin.log(IStatus.ERROR, "Error when creating server socket.", e);
			throw e;
		}
	}
	
	/**
	 * If socket already connected as client
	 */
	public boolean isClient() {
		return fClient;
	}
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	Exception getException() {
		return e;
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public void stopListening() {
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				APLDebugCorePlugin.log(IStatus.WARNING,
						"Can't close server socket.", e);
			} finally {
				serverSocket = null;
			}
		}
	}
	
	public boolean isDisposed() {
		return serverSocket == null;
	}
	
	@Override
	public void run() {
		try {
			if (isClient())
				socket = waitForConnectionAsClient();
			else
				socket = waitForConnection();
		} catch (IOException e) {
			this.e = e;
			stopListening();
		}
	}

	public Socket waitForConnectionAsClient() {
		long start = System.currentTimeMillis();
		do {
			try {
				socket = new Socket(fHost, fPort);
				this.e = null;
				return socket;
			} catch (UnknownHostException e1) {
//				System.err.println("Connection: UnknownHost");
				this.e = e1;
			} catch (IOException e) {
//				System.err.println("Connection: IOException");
				this.e = e;
			}
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {

			}
		} while (timeout > (System.currentTimeMillis() - start) );
		return null;
	}
	
	public Socket waitForConnection() 
			throws SocketException, IOException {
		serverSocket.setSoTimeout(timeout);
		return serverSocket.accept();
	}
	
	public int getLocalPort() throws IOException {
		return fPort;
//		int localPort;
//		if ( !fClient ) {
//			localPort = serverSocket.getLocalPort();
//		} else {
//			localPort = fPort;
//		}
//		
//		SocketUtil.checkValidPort(localPort);
//		return localPort;
	}
	
	@Override
	protected void finalize() throws Throwable {
		// Clear resources when garbage-collected.
		try {
			this.stopListening();
		} catch (Throwable e) {
			// Never fail
			APLDebugCorePlugin.log(IStatus.WARNING,
					"Error finalizing ListenConnector", e);
		}
	}
	
	public void close() {
		
	}

	public void setConnectionSettings(String host, int port) {
		this.fPort = port;
		this.fHost = host;
	}
}
