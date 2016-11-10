package com.dyalog.apldev.debug.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class APLDebugCorePlugin extends Plugin {
	// The shared instance.
	private static APLDebugCorePlugin plugin;
	// Resource bundle.
	private ResourceBundle resourceBundle;
	
	/**
	 * Unique identifier for the APL debug model
	 */
	public static final String ID_APL_DEBUG_MODEL = "apl.debugModel";

	/**
	 * Name of the string substitution variable that resolves to the
	 * location of a local Perl executable (value <code>perlExecutable</code>).
	 */
	public static final String VARIABLE_PERL_EXECUTABLE = "aplExecutable";
	/**
	 * Launch configuration attribute key. Value is a path to a perl
	 * program. The path is a string representing a full path
	 * to a perl program in the workspace.
	 */
	public static final String ATTR_PDA_PROGRAM = ID_APL_DEBUG_MODEL + ".ATTR_PDA_PROGRAM";
	
	/**
	 * Identifier for the APL launch configuration type
	 */
	public static final String ID_APL_LAUNCH_CONFIGURATION_TYPE = "apl.launchType";
	
	/**
	 * Plug-in identifier.
	 */
	public static final String PLUGIN_ID = "com.dyalog.apldev.debug.core";
	public static final String ATTR_PROJECT_NAME = PLUGIN_ID + ".ATTR_PROJECT";
	public static final String ATTR_MAINMODULE = PLUGIN_ID + ".ATTR_MAINMODULE";
	public static final String ATTR_INTERPRETER = PLUGIN_ID + ".INTERPRETER";
	public static final String MODULES_EXTENSION = "apl";
	
	public static final String ATTR_WORKING_DIRECTORY = PLUGIN_ID + ".ATTR_WORKING_DIRECTORY";
//	public static final String ATTR_DEFAULT_WORKING_DIRECTORY = PLUGIN_ID + ".ATTR_DEFAULT_WORKING_DERECTORY";
	public static final String ATTR_OTHER_WORKING_DIRECTORY = PLUGIN_ID + ".ATTR_OTHER_WORKING_DIRECTORY";
	public static final String ATTR_LAUNCH_INTERPRETER = PLUGIN_ID + ".ATTR_LAUNCH_INTERPRETER";
	public static final String ATTR_INTERPRETER_PORT = PLUGIN_ID + ".ATTR_INTERPRETER_PORT";
	public static final String ATTR_INTERPRETER_HOST = PLUGIN_ID + ".ATTR_INTERPRETER_HOST";
	public static final String ATTR_INTERPRETER_PATH = PLUGIN_ID + ".ATTR_INTERPRETER_PATH";
	public static final String ATTR_INTERPRETER_CONNECT = PLUGIN_ID + ".ATTR_INTERPRETER_CONNECT";
	public static final String FUNCTION_EDITOR_ID = "apl.editor";
	public static final String ATTR_PROGRAM_ARGUMENTS = PLUGIN_ID + ".PROGRAM_ARGUMENTS";
	public static String ATTR_DEFAULT_INTERPRETER_PATH_WIN = "cmd /c start dyalog";
	public static String ATTR_DEFAULT_INTERPRETER_PATH_LINUX = "xterm -e dyalog";
	public static String ATTR_DEFAULT_INTERPRETER_PATH_MACOSX = "dyalog";

	
	/**
	 * The constructor.
	 */
	public APLDebugCorePlugin() {
		super();
		plugin = this;
	}
	
	/**
	 * This method is called upon plug-in activation
	 */
//	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}
	
	/**
	 * This method is called when the plug-in is stoped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
		resourceBundle = null;
	}
	
	/**
	 * Returns the shared instance.
	 */
	public static APLDebugCorePlugin getDefault() {
		if (plugin == null) {
			throw new NullPointerException("Probably in test code relying on running outside of OSGi");
		}
		return plugin;
	}
	public static String getPluginID() {
		return getDefault().getBundle().getSymbolicName();
	}
	
	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = APLDebugCorePlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}
	
	/**
	 * Returns the plugin's resource bundle
	 */
	public ResourceBundle getResourceBundle() {
		try {
			if (resourceBundle == null)
				resourceBundle = ResourceBundle.getBundle(PLUGIN_ID + ".DebugCorePluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
		return resourceBundle;
	}
	
	/**
	 * Return a <code>java.io.File</code> object that corresponds to the specified
	 * <code>IPath</code> in the plugin directory, or <code>null</code> if none.
	 */
	public static File getFileInPlugin(IPath path) {
		try {
			URL installURL = getDefault().getBundle().getEntry(path.toString());
			URL localURL = FileLocator.toFileURL(installURL);
			return new File(localURL.getFile());
		} catch (IOException ioe) {
			return null;
		}
	}
	
	
	public static String getUniqueIdentifier() {
		APLDebugCorePlugin plugin = getDefault();
		return plugin != null ? plugin.getBundle().getSymbolicName()
				: "com.dyalog.apldev.debug.core";
	}
	
	/**
	* Returns an image descriptor for the image file at the given
	* plug-in relative path.
	*
	* @param path the path
	* @return the image descriptor
	*/
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(getUniqueIdentifier(), path);
	}


}
