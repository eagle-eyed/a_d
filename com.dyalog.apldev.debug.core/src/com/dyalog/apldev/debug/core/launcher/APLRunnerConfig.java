package com.dyalog.apldev.debug.core.launcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.model.remote.ListenConnector;

/**
 * Holds configuration for launching APL Interpreter
 */

public class APLRunnerConfig {

	public static final String RUN_REGULAR = "APL regular run";

//	public final IProject project;
	public IProject project;
//	public final IPath[] resource;
//	public final IPath interpreter;
	private ListenConnector ListenConnector;
	public int acceptTimeout = 5000; // milliseconds
	public String[] envp = null;

	private boolean isDebug;
	private ILaunchConfiguration configuration;
	private String run;

	private Path interpreter;


	public APLRunnerConfig (ILaunchConfiguration configuration, String mode, String run)
			throws CoreException {
		this(configuration, mode, run, true);
	}
	
	public APLRunnerConfig(ILaunchConfiguration conf, String mode, String run,
			boolean makeArgumentsVariableSubstitution) throws CoreException {

		try {
			project = getProjectFromConfiguration(conf);
		} catch (CoreException e) {

		}
		this.configuration = conf;
		this.run = run;
		isDebug = mode.equals(ILaunchManager.DEBUG_MODE);
		// make the environment
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		envp = launchManager.getEnvironment(conf);
//		interpreter = new Path("c:\\Dyalog\\Dyalog APL-64 15.0 Unicode\\dyalog.exe");
	}
	
	/**
	 * Gets the project that should be used for a launch configuration
	 * @return the related IProject
	 * @throws CoreException
	 */
	public static IProject getProjectFromConfiguration(ILaunchConfiguration conf)
			throws CoreException {
		String projName = conf.getAttribute(APLDebugCorePlugin.ATTR_PROJECT_NAME, "");
		if (projName == null || projName.length() == 0) {
			throw new CoreException(APLDebugCorePlugin.makeStatus(IStatus.ERROR,
					"Unable to get project for the run", null));
		}
		IWorkspace w = ResourcesPlugin.getWorkspace();
		IProject p = w.getRoot().getProject(projName);
		if (p == null || !p.exists()) {
			throw new CoreException(APLDebugCorePlugin.makeStatus(IStatus.ERROR,
					"Could not get project: " + projName, null));
		}
		return p;
	}
	
	public synchronized ListenConnector getDebuggerListenConnector()
			throws IOException {
			if (this.ListenConnector == null) {
				this.ListenConnector = new ListenConnector(this.acceptTimeout);
			}
			return this.ListenConnector;
		}

	public synchronized ListenConnector getDebuggerListenConnector(int localPort)
			throws IOException {
			if (this.ListenConnector == null) {
				this.ListenConnector = new ListenConnector(this.acceptTimeout, localPort);
			}
			return this.ListenConnector;
		}

	/**
	 * Create a command line for launching.
	 * @return String[] 
	 * @throws CoreException
	 */
	public String[] getCommandLine() throws CoreException {
		List<String> commandList = new ArrayList<String>();
		commandList.add(interpreter.toOSString());
		// communication port
		String portNum;
		return commandList.toArray(new String[commandList.size()]);
	}

	public String getPort() throws CoreException {
		String portNum;
		try {
			portNum = Integer.toString(getDebuggerListenConnector().getLocalPort());
		} catch (IOException e) {
			throw new CoreException(APLDebugCorePlugin.makeStatus(IStatus.ERROR,
					"Unable to get port", e));
		}
		return portNum;
	}

	/**
	 * 
	 * @param env
	 * @return an array with the formatted map
	 */
	public static String[] getMapEnvAsArray(Map<String, String> env) {
		List<String> strings = new ArrayList<String> (env.size());
//		StringBuffer buffer = new StringBuffer();
//		for (Iterator<Map.Entry<String, String>> iter = env.entrySet().iterator();
//				iter.hasNext();) {
//			Map.Entry<String, String> entry = iter.next();
//			buffer.clear().append(entry.getKey());
//			buffer.append('=').append(entry.getValue());
//			strings.add(buffer.toString());
//		}
		
		return strings.toArray(new String[strings.size()]);
	}
}
