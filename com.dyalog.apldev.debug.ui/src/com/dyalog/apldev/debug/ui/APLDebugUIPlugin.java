package com.dyalog.apldev.debug.ui;

import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.dyalog.apldev.debug.ui.presentation.ImageCache;

/**
 * The activator class controls the plug-in life cycle
 */
public class APLDebugUIPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.dyalog.apldev.debug.ui"; //$NON-NLS-1$

	// The shared instance
	/**
	 *  The id of the plugin
	 */
	private static APLDebugUIPlugin plugin;
	
	// Resource bundle.
	private ResourceBundle resourceBundle;
	
	private final static String ICONS_PATH = "icons/";
	private final static String PATH_OBJECT = ICONS_PATH + "obj16/"; // Model object icons
	
	/**
	 * Variable decoration
	 */
	public final static String IMG_VAR = "IMG_VAR";
	
	
	/**
	 * APL application images
	 */
	public final static String IMG_OBJ_APL = "IMB_OBJ_PDA";

//	public static final String ATTR_PROJECT = "com.dyalog.apldev.debug.ATTR_PROJECT";

	public static final String ATTR_LOCATION = "org.eclipse.ui.externaltools" + ".ATTR_LOCATION";

	public static final String MAIN_ICON = "cube.png";
	public static final String MAIN_TAB_ICON = "A16.gif";
	
	public ImageCache imageCache;
	
	/**
	 * The constructor
	 */
	public APLDebugUIPlugin() {
		super();
//		plugin = this;
	}

	/**
	 * This method is called upon plug in activation
	 */
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
//	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		imageCache =new ImageCache(APLDebugUIPlugin.getDefault().getBundle().getEntry("/icons/"));
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
//	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
		resourceBundle = null;
		imageCache.dispose();
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static APLDebugUIPlugin getDefault() {
		return plugin;
	}
	
//	public static IBundleInfo info;
//	
//	public static IBundleInfo getBundleInfo() {
//		if (info == null) {
//			info = new BundleInfo(getDefault().getBundle());
//		}
//	}
	
	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = APLDebugUIPlugin.getDefault().getResourceBundle();
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
				resourceBundle = ResourceBundle.getBundle("example.debug.ui.DebugUIPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
		return resourceBundle;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#initializeImageRegistry(org.eclipse.jface.resource.ImageRegistry)
	 */
	protected void initializeImageRegistry(ImageRegistry reg) {
		declareImage(IMG_VAR, "icons/cube.gif");
		declareImage(IMG_OBJ_APL, PATH_OBJECT + "A16.png");
	}
	
	/**
	 * Declares a workbench image given the path of the image file (raltive to
	 * the workbench plug-in). This is a helper method that creates the image
	 * descriptor and passes it to the main <code>declareImage</code> method.
	 * 
	 * @param key symbolicName the symbolic name of the image
	 * @param path the path of the image file relative to the base of the workbench
	 * plug-ins install directory
	 * <code>false</code> if this is not a shared image
	 */
	private void declareImage(String key, String path) {
		ImageDescriptor desc = ImageDescriptor.getMissingImageDescriptor();
		Bundle bundle = Platform.getBundle(PLUGIN_ID);
		URL url = null;
		if (bundle != null) {
			url = FileLocator.find(bundle, new Path(path), null);
			if (url != null) {
				desc = ImageDescriptor.createFromURL(url);
			}
		}
		getImageRegistry().put(key, desc);
		
	}
	
	/**
	 * Returns the workspace instance
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * Instance of custom image manager
	 */
	public static ImageCache getImageCache() {
		return plugin.imageCache;
	}

	public static String getPluginID() {
//		return getBundleInfo().getPluginID();
		return PLUGIN_ID;
	}
}
