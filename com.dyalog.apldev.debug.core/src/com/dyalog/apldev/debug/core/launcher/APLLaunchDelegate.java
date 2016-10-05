package com.dyalog.apldev.debug.core.launcher;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.util.NLS;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.model.APLDebugTarget;
import com.dyalog.apldev.debug.core.model.remote.ListenConnector;
import com.dyalog.apldev.debug.core.model.remote.RemoteDebugger;

/**
 * Abstract base class for APL launch configuration delegates.
 * This class defines some helper methods and common behavior useful
 * regardless of the actual launch configuration type. Subclasses
 * must implement {@link #doLaunch}.
 */
public class APLLaunchDelegate extends LaunchConfigurationDelegate {

	private IProject[] fOrderedProjects;

	/**
	 * We need to reimplement this method (otherwise, all the projects in the workspace will be rebuilt, and not only
	 * the ones referenced in the configuration).
	 */
	@Override
	protected IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode) throws CoreException {
		return fOrderedProjects;
	}

	@Override
	public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor)
			throws CoreException {
		// build project list
		fOrderedProjects = null;
		String projName = configuration.getAttribute(APLDebugCorePlugin.ATTR_PROJECT_NAME, "");
		if (projName.length() > 0) {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);

			if (project != null) {
				fOrderedProjects = computeReferencedBuildOrder(new IProject[] { project });
			}
		}
		// do generic launch checks
		return super.preLaunchCheck(configuration, mode, monitor);
	}
	
	public final void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
//		launch.setSourceLocator(new SourceLocator());
		
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		String projectName = configuration.getAttribute(APLDebugCorePlugin.ATTR_PROJECT_NAME, (String) null);
		IProject project = null;
		if (projectName != null && projectName.length() > 0) {
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		}
		
		if (projectName == null) {
			abort("Can't read project name", null);
		} else if (project == null) {
			abort(MessageFormat.format("Can't open {0} project", new Object[] {projectName}), null);
		} else if ( ! project.exists()) {
			abort(MessageFormat.format("Project {0} not exist", new Object[] {projectName}), null);
		} else if ( ! project.isOpen()) {
			abort(MessageFormat.format("Project {0} not opened", new Object[] {projectName}), null);
		}
		
		monitor.setTaskName("Preparing Configuration");
		SubMonitor subMonitor = SubMonitor.convert(monitor, "Preparing configuration", 3);
		APLRunnerConfig config = new APLRunnerConfig(configuration, mode,
				APLRunnerConfig.RUN_REGULAR);
		try {
			final RemoteDebugger debugger = new RemoteDebugger();
			boolean launchInterpreter = configuration.getAttribute(
					APLDebugCorePlugin.ATTR_LAUNCH_INTERPRETER, false);
			String[] commandLine = {""};
			File workingDirectory = null;
			String[] envp = null;
			Map <String,String> envMap = new HashMap <String, String> ();
			
			boolean connectSettings = configuration.getAttribute(
					APLDebugCorePlugin.ATTR_INTERPRETER_CONNECT, false);
			int port = -1;
			String host= "";
			
			if (launchInterpreter) {
				// launch process
				String pathInterpreter = configuration.getAttribute(
						APLDebugCorePlugin.ATTR_INTERPRETER_PATH, "");
				List <String> commandList = new ArrayList <String> ();
				commandList.add(pathInterpreter);
				String prgmArg = configuration.getAttribute(
						APLDebugCorePlugin.ATTR_PROGRAM_ARGUMENTS, (String) null);
				if (prgmArg != null) {
					String[] args = DebugPlugin.parseArguments(prgmArg);
					if (args != null) {
						for (int i = 0; i < args.length; i++) {
							commandList.add(args[i]);
						}
					}
//					commandLine = quoteWindowsArgs(commandLine);
				}
				String workDir = configuration.getAttribute(
						APLDebugCorePlugin.ATTR_WORKING_DIRECTORY, (String) null);
				
				envMap = configuration.getAttribute(
						ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, envMap);
				ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
				envp = launchManager.getEnvironment(configuration);
				if (envp == null) {
					// set system environment
					envMap = launchManager.getNativeEnvironment();
					Set<String> set = envMap.keySet();
					envp = new String[envMap.size()];
					int i = 0;
					for (String key : set) {
						if (i < envp.length)
							envp[i++] = key + "=" + envMap.get(key);
					}
				}

				if (workDir == null) {
					// use default working directory
					if (config.project != null) {
						workingDirectory = config.project.getLocation().toFile();
					}
				} else {
					workingDirectory = new Path(workDir).toFile();
				}
				monitor.setTaskName("Launching Interpreter");
				commandLine = new String[commandList.size()];
				commandList.toArray(commandLine);
			}
			subMonitor.worked(1);

			final ListenConnector listenConnector = config.getDebuggerListenConnector();
			if (connectSettings) {
				// use specified connection settings
				host = configuration.getAttribute(APLDebugCorePlugin.ATTR_INTERPRETER_HOST, "");
				
				String portVal = configuration.getAttribute(APLDebugCorePlugin.ATTR_INTERPRETER_PORT, "4502");
				try {
					port = Integer.parseInt(portVal);
					listenConnector.setConnectionSettings(host, port);
				} catch (NumberFormatException e) {
					
				}
			}
			
			Process process = null;
			if (launchInterpreter & !connectSettings) {
				port = listenConnector.getLocalPort();
				// Add environment variable if needed
				if (envp == null) {
					envp = new String[] {"RIDE_INIT=SERVE::" + port};
				} else {
					// check if communication port already specified in environment variable
					if( !envMap.containsKey("RIDE_INIT")) {
						String[] envpAdd = new String[envp.length];
						System.arraycopy(envp, 0, envpAdd, 0, envp.length);
						envpAdd[envpAdd.length - 1] = "RIDE_INIT=SERVE::" + port;
						envp = envpAdd;
					}
				}
				process = launchProcess(commandLine, workingDirectory, envp);
				if (process == null) {
					throw new CoreException(APLDebugCorePlugin.makeStatus(IStatus.ERROR,
						"Could not execute APL interpreter process.", null));
				}
			} else if (launchInterpreter & connectSettings) {
				// just launch process without checking environment variable
				process = launchProcess(commandLine, workingDirectory, envp);
				
			} else if (!launchInterpreter & connectSettings) {
				
			} else {
				// Terminate wrong configuration
				return;
			}
			final IProcess ip;
			if (process != null) {
				// specify process properties visible from property window
				HashMap<String, String> processAttributes = new HashMap<>();
				String timestamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
						.format(new Date(System.currentTimeMillis()));
				String cmdLine = getCmdLineAsString(commandLine);

				processAttributes.put(IProcess.ATTR_PROCESS_TYPE, "apl");
				ip = DebugPlugin.newProcess(launch, process,
						NLS.bind("{0} ({1})", new String[] {commandLine[0], timestamp}),
						processAttributes);
				ip.setAttribute(DebugPlugin.ATTR_PATH, commandLine[0]);
				ip.setAttribute(IProcess.ATTR_CMDLINE,
						DebugPlugin.renderArguments(commandLine, null));
				String ltime = launch.getAttribute(DebugPlugin.ATTR_LAUNCH_TIMESTAMP);
				ip.setAttribute(DebugPlugin.ATTR_LAUNCH_TIMESTAMP, ltime != null ? ltime : timestamp);
				if (workingDirectory != null) {
					ip.setAttribute(DebugPlugin.ATTR_WORKING_DIRECTORY, workingDirectory.getAbsolutePath());
					if (envp != null) {
						Arrays.sort(envp);
						StringBuffer buff = new StringBuffer();
						for (int i = 0; i < envp.length; i++) {
							buff.append(envp[i]);
							if (i < envp.length-1) {
								buff.append('\n');
							}
						}
						ip.setAttribute(DebugPlugin.ATTR_ENVIRONMENT, buff.toString());
					}
				}
			} else {
				ip = null;
			}
			
			// command for executing after launching
			String mainModule = configuration.getAttribute(APLDebugCorePlugin.ATTR_MAINMODULE, (String) null);

			APLDebugTarget target = new APLDebugTarget(launch, ip, mainModule, debugger, config.project);
			target.finishedInit = true;

			subMonitor.worked(1);
			monitor.setTaskName("Waiting for connection...");
	
			debugger.startConnect(subMonitor, config, port);

			Socket socket = null;
			try {
				socket = debugger.waitForConnection(subMonitor, process, ip);
				if (socket == null) {
					target.terminate();
					debugger.disposeConnector();
					return;
				}
			} catch (Exception e) {
				if (ip != null)
					ip.terminate();
				if (process != null)
					process.destroy();
				target.terminate();
				String message = "Unexpected error setting up the debugger";
				if (e instanceof SocketTimeoutException) {
					message = "Timed out after " + Float.toString(config.acceptTimeout / 1000)
							+ " seconds while waiting for interpreter to connect.";
				}
				throw new CoreException (APLDebugCorePlugin.makeStatus(IStatus.ERROR, message, e));
			}
//			launch.setSourceLocator();
			
			target.addSessionConsole();
//			target.addConsoleInputListener();
			target.startTransmission(socket);
//			target.initialize();
			monitor.setTaskName("Done");
			subMonitor.done();

		} catch (IOException e) {
			throw new CoreException(APLDebugCorePlugin.makeStatus(IStatus.ERROR,
					"Unexpected IO Exception during launching APL application", null));
		}
	}

	private Process launchProcess(String[] commandLine, File workingDirectory, String[] envp) {
		Process process = null;
		try {
		Runtime r = Runtime.getRuntime();
		/* 
		 * on Windows interpreter launch command must looks like:
		 * process = r.exec("cmd /c start dyalog", envp);
		 * on linux
		 * process = r.exec("xterm -e dyalog");
		 * or
		 * process = r.exec("xterm -e /opt/mdyalog/15.0/64/unicode/maple");
		 */
		// Runtime exec better work if passed single line string
		String cmdLine = getCmdLineAsString(commandLine);
		if (workingDirectory != null)
			process = r.exec(cmdLine, envp, workingDirectory);
		else
			process = r.exec(cmdLine, envp);

//		if (commandLine.length == 1) {
//			if (workingDirectory != null)
//				process = r.exec(commandLine[0], envp, workingDirectory);
//			else
//				process = r.exec(commandLine[0], envp);
//		} else {
//			if (workingDirectory != null && workingDirectory.isDirectory()) {
//				process = r.exec(commandLine, envp, workingDirectory);
//			} else {
//				process = r.exec(commandLine, envp, null);
//			}
//		}
		/* below standard instruction for launching interpreter but on Windows don't
		 * work properly with dyalog interpreter, so commented and substituted by typed above
		 */
//		process = DebugPlugin.exec(commandLine, workingDirectory, envp);
		} catch (IOException e) {
			APLDebugCorePlugin.log(IStatus.ERROR, "Can't launch interpreter process", e);
		}
		return process;
	}

	/**
	 * Throws an exception with a new status containing the given
	 * message and optional exception.
	 * 
	 * @param message error message
	 * @param e underlying exception
	 * @throws CoreException
	 */
	private void abort(String message, Throwable e) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR,
				APLDebugCorePlugin.PLUGIN_ID, 0, message, e));
	}
	
	/**
	 * Returns a free port number on localhost, or -1 if unable to find a free port.
	 * 
	 * @return a free port number on localhost, or -1 if unable to find a free port
	 */
	public static int findFreePort() {
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(0);
			return socket.getLocalPort();
		} catch (IOException e) {

		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					
				}
			}
		}
		return -1;
	}
	
	private static String[] quoteWindowsArgs(String[] cmdLine) {
		if (Platform.getOS().equals(Constants.OS_WIN32)) {
			String[] winCmdLine = new String[cmdLine.length];
			if (cmdLine.length > 0) {
				winCmdLine[0] = cmdLine[0];
			}
			for (int i = 1; i < cmdLine.length; i++) {
				winCmdLine[i] = winQuote(cmdLine[i]);
			}
			cmdLine = winCmdLine;
		}
		return cmdLine;
	}
	
	private static boolean needsQuoting(String s) {
		int len = s.length();
		if (len == 0) // empty string has to be quoted
			return true;
		if ("\"\"".equals(s))
			return false;
		for (int i = 0; i < len; i++) {
			switch (s.charAt(i)) {
				case ' ':
				case '\t':
				case '\\':
				case '"':
					return true;
			}
		}
		return false;
	}
	
	private static String winQuote(String s) {
		if (! needsQuoting(s)) {
			return s;
		}
		s = s.replaceAll("([\\\\]*\"", "$1$1\\\\\"");
		s = s.replaceAll("([\\\\]*)\\z", "$1$1");
		return "\"" + s + "\"";
	}
	
	/**
	 * Returns the given array of strings as a single space-delimited string
	 */
	protected String getCmdLineAsString(String[] cmdLine) {
		StringBuffer buff = new StringBuffer();
		for (int i = 0, numStrings = cmdLine.length; i < numStrings; i++) {
			buff.append(cmdLine[i]);
			buff.append(' ');
		}
		return buff.toString().trim();
	}
}
