package com.dyalog.apldev.debug.ui.presentation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.log.Log;

public class ImageCache {

	private volatile Image imMissing = null;
	private final Object lock = new Object();
	private final Object descriptorLock = new Object();
	private final Map <Object, Image> imageHash = new HashMap <Object, Image>(10);
	private final Map <Object, ImageDescriptor> descriptorHash = new HashMap <Object, ImageDescriptor>(10);
	private final URL baseURL;
	
	public ImageCache(URL baseURL) {
		this.baseURL = baseURL;
	}
	
	public Image get(String key) {
		Image image = getFromImageHash(key);
		
		if (image == null) {
			ImageDescriptor desc;
			try {
				desc = getDescriptor(key);
				image = desc.createImage();
				image = putOnImageHash(key, image);
			} catch (Exception e) {
				// if image is imMissing return default "imMissing" image
				Log.log(IStatus.ERROR, "Missing image: " + key, e);
				if (imMissing == null || imMissing.isDisposed()) {
					desc = ImageDescriptor.getMissingImageDescriptor();
					imMissing = desc.createImage();
				}
			}
		}
		return image;
	}
	
	private Image getFromImageHash(Object key) {
		synchronized (lock) {
			Image ret = imageHash.get(key);
			if (ret != null && ret.isDisposed()) {
				imageHash.remove(key);
				ret = null;
			}
			return ret;
		}
	}

	private Image putOnImageHash(Object key, Image image) {
		synchronized (lock) {
			Image createdInMeanwhile = imageHash.get(key);
			// if exist substitute with new one
			if (createdInMeanwhile != null && !createdInMeanwhile.isDisposed()) {
				image.dispose();
				image = createdInMeanwhile;
			} else {
				imageHash.put(key, image);
			}
			return image;
		}
	}
	
	public ImageDescriptor getDescriptor(String key) {
		synchronized (descriptorLock) {
			if (!descriptorHash.containsKey(key)) {
				URL url;
				ImageDescriptor desc;
				try {
					url = new URL(baseURL, key);
					desc = ImageDescriptor.createFromURL(url);
				} catch (MalformedURLException e) {
					Log.log(IStatus.ERROR, "Missing image: " + key, e);
					desc = ImageDescriptor.getMissingImageDescriptor();
				}
				descriptorHash.put(key, desc);
				return desc;
			}
			return descriptorHash.get(key);
		}
	}
	
	public void dispose() {
		synchronized (lock) {
			Iterator<Image> e = imageHash.values().iterator();
			while (e.hasNext()) {
				e.next().dispose();
			}
			imageHash.clear();
			if (imMissing != null)
				imMissing.dispose();
			imMissing = null;
		}
	}
}
