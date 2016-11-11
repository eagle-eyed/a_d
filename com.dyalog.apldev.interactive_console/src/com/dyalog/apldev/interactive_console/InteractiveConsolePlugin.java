package com.dyalog.apldev.interactive_console;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.dyalog.apldev.interactive_console.console.prefs.ColorManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class InteractiveConsolePlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.dyalog.apldev.interactive_console"; //$NON-NLS-1$

	// The shared instance
	private static InteractiveConsolePlugin plugin;

	public static final String CONSOLE_ICON = "session.gif";
	public static final String INTERRUPT_ICON = "interrupt.gif";
	public static final String RIDE_CONSOLE_ICON = "ride_console.gif";
	public static final String TERMINATE = "terminate.gif";
	
	/**
	 * The constructor
	 */
	public InteractiveConsolePlugin() {
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
		ColorManager.getDefault().dispose();
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static InteractiveConsolePlugin getDefault() {
		return plugin;
	}

	/* Images for the console */
	private static final String[][] IMAGES = new String[][] {
		{ "icons/cubeR.ico", InteractiveConsolePlugin.RIDE_CONSOLE_ICON },
		{ "icons/session.gif", InteractiveConsolePlugin.CONSOLE_ICON },
		{ "icons/interrupt.gif", InteractiveConsolePlugin.INTERRUPT_ICON },
		{ "icons/terminate.gif", InteractiveConsolePlugin.TERMINATE },
	};
	
	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		for (int i = 0; i < IMAGES.length; i++) {
			URL url = getDefault().getBundle().getEntry(IMAGES[i][0]);
			registry.put(IMAGES[i][1], ImageDescriptor.createFromURL(url));
		}
	}
	
	public ImageDescriptor getImageDescriptor(String key) {
		return getImageRegistry().getDescriptor(key);
	}
}
