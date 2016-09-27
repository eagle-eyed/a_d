package com.dyalog.apldev.debug.core.model.remote;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketImpl;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;

/**
 * Class to find a port to debug
 */

public class SocketUtil {

	public static ServerSocket createLocalServerSocket(int port)
			throws IOException {

		ServerSocket serverSocket = new ServerSocket(port);
		int localPort = serverSocket.getLocalPort();
		try {
			SocketUtil.checkValidPort(localPort);
		} catch (Exception e) {
			throw e;
		}
		return serverSocket;
	}
	
	public static ServerSocket createLocalServerSocket() throws IOException {
		ServerSocket serverSocket = new ServerSocket(0);
		int localPort = serverSocket.getLocalPort();
		try {
			SocketUtil.checkValidPort(localPort);
		} catch (Exception e) {
			// close this one and try a different approach.
			try {
				serverSocket.close();
			} catch (Exception e1) {

			}
			
			serverSocket = new ServerSocket(findUnusedLocalPorts(1) [0]);
			localPort = serverSocket.getLocalPort();
			try {
				SocketUtil.checkValidPort(localPort);
			} catch (IOException invalidPortException) {
				// close the socket and throw error!
				try {
					serverSocket.close();
				} catch (Exception e1) {
					
				}
				throw invalidPortException;
			}
		}
		return serverSocket;
	}

	/**
	 * Returns free ports on the local host.
	 * @param ports: number of ports to return 
	 */
	private static Integer[] findUnusedLocalPorts(final int ports) {
		Throwable firstFoundExc = null;
		final List<ServerSocket> socket = new ArrayList<ServerSocket>();
		final List<Integer> portsFound = new ArrayList<Integer>();
		try {
			try {
				for (int i = 0; i < ports; i++) {
					ServerSocket s = new ServerSocket(0);
					socket.add(s);
					int localPort = s.getLocalPort();
					checkValidPort(localPort);
					portsFound.add(localPort);
				}
			} catch (Throwable e) {
				firstFoundExc = e;
				// Try a different approach
				final Set<Integer> searched = new HashSet<Integer>();
				try {
					for (int i = 0; i < ports && portsFound.size() < ports; i++) {
						int localPort = findUnusedLocalPort(20000, 65535, searched);
						checkValidPort(localPort);
						portsFound.add(localPort);
					}
				} catch (Exception e1) {
					APLDebugCorePlugin.getDefault().getLog().log(
				            new Status (IStatus.ERROR, APLDebugCorePlugin.PLUGIN_ID,
				            		"Error creating Socket", e));

				}
			} finally {
				for (ServerSocket s : socket) {
					if (s != null) {
						try {
							s.close();
						} catch (Exception e) {
							
						}
					}
				}
			}
			
			if (portsFound.size() != ports) {
				throw firstFoundExc;
			}
		} catch (Throwable e) {
			String message = "Unable to find unused local port.";
			throw new RuntimeException(message, e);
		}
		
		return portsFound.toArray(new Integer[portsFound.size()]);
	}

	/**
	 * Returns a free port number on the specified host within
	 * the given range, or -1 if none found.
	 */
	private static int findUnusedLocalPort(int searchFrom, int searchTo, Set<Integer> searched) {
		for (int i = 0; i < 15; i++) {
			int port = getRandomPort(searchFrom, searchTo);
			if (searched.contains(i)) {
				continue;
			}
			searched.add(i);
			ServerSocket s = null;
			try {
				s = new ServerSocket();
				SocketAddress sa = new InetSocketAddress(InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }), port);
				s.bind(sa);
				return s.getLocalPort();
				
			} catch (IOException e) {
				
			} finally {
				if (s != null) {
					try {
						s.close();
					} catch (IOException ioe) {
						
					}
				}
			}
		}
		return -1;
	}

	private static final Random fgRandom = new Random(System.currentTimeMillis());
	
	private static int getRandomPort(int searchFrom, int searchTo) {
		return (int) (fgRandom.nextFloat() * (searchTo - searchFrom)) + searchFrom;
	}

	/**
	 * If wrong localPort value raise IOException
	 */
	public static void checkValidPort(int localPort) throws IOException {

		if (localPort == -1) {
			throw new IOException("Port not bound (found port -1).");
		}
		if (localPort == 0) {
			throw new IOException("Port not bound (found port 0)");
		}
	}
	
}
