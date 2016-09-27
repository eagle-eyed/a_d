package com.dyalog.apldev.debug.core.model.remote;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.model.IProcess;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.launcher.APLRunnerConfig;
import com.dyalog.apldev.debug.core.model.APLDebugTarget;

/**
 * Network interface to the debugger
 */
public class RemoteDebugger {

	private ListenConnector connector; // Runnable that connects to the debugger
	private Thread connectThread;
	private List<APLDebugTarget> fTargets = new ArrayList<APLDebugTarget>();
	
	public ListenConnector startConnect(IProgressMonitor monitor,
			APLRunnerConfig config, int localPort)
			throws IOException, CoreException {

		ListenConnector connector = config.getDebuggerListenConnector(localPort);
		// start listen for connection to server socket during timeout
		startConnect(connector);
		return connector;
	}

	public ListenConnector startConnect(IProgressMonitor monitor, APLRunnerConfig config)
			throws IOException, CoreException {
		monitor.subTask("Finding free socket...");
		ListenConnector connector = config.getDebuggerListenConnector();
		// start listen for connection to server socket during timeout
		startConnect(connector);
		return connector;
	}
	
	public ListenConnector startConnect(ListenConnector connector)
			throws IOException, CoreException {
		this.connector = connector;
//		if (!connector.isClient()) {
			// open server socket and wait for connection
			connectThread = new Thread(connector, "APLInerpreter.connect");
			connectThread.start();
//		}
		return connector;
	}
	
	/**
	 * Wait for the connection to the debugger to complete
	 */
	public Socket waitForConnection(IProgressMonitor monitor, Process p, IProcess ip)
			throws Exception {
//		if (connector.isClient())
//			return connector.getSocket();
		// launch the debug listener on a thread, and wait until it completes
		while (connectThread.isAlive()) {
			if (monitor.isCanceled()) {
				connector.stopListening();
				p.destroy();
				return null;
			}
			if (p != null) {
				try {
					p.exitValue(); // throws exception if process has terminated
					// process has terminated - stop waiting for a connection
					connector.stopListening();
					String errorMessage = ip.getStreamsProxy().getErrorStreamMonitor()
							.getContents();
					if (errorMessage.length() != 0) {
	                    throw new CoreException(APLDebugCorePlugin.makeStatus(IStatus.ERROR,
	                            "Something got printed in the error stream", null));
					}
				} catch (IllegalThreadStateException e) {
					// expected while process is alive
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				
			}
		}
		Exception connectorException = connector.getException();
		if (connectorException != null) {
			throw connectorException;
		}
		return connector.getSocket();
	}
	
	public void disposeConnector() {
		if (connector != null) {
			connector.stopListening();
			connector = null;
		}
	}

	public void addTarget(APLDebugTarget aplDebugTarget) {
		this.fTargets.add(aplDebugTarget);
		
	}
}

