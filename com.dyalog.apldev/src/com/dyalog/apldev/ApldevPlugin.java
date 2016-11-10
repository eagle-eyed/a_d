package com.dyalog.apldev;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ApldevPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.dyalog.apldev"; //$NON-NLS-1$
	public static final String ATTR_SHOW_RIDE = PLUGIN_ID + ".ATTR_SHOW_RIDE";
	
	// The shared instance
	private static ApldevPlugin plugin;
	
	/**
	 * The constructor
	 */
	public ApldevPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ApldevPlugin getDefault() {
		return plugin;
	}

	public static String getUniqueIdentifier() {
		ApldevPlugin plugin = getDefault();
		return plugin != null ? plugin.getBundle().getSymbolicName()
				: "com.dyalog.apldev";
	}

	public static String getPluginID() {
		return getDefault().getBundle().getSymbolicName();
	}

}
