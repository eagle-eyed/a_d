package com.dyalog.apldev.log;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.dyalog.apldev.ApldevPlugin;

public class Log {
	public static void log(Throwable e) {
		ApldevPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, ApldevPlugin.getUniqueIdentifier(), 0,
				"Debug Error", e));
	}

	public static Status makeStatus(int errorLevel, String message, Throwable e) {
        return new Status(errorLevel, ApldevPlugin.getPluginID(), errorLevel, message, e);
    }
	
	/**
	 * 
	 * @param errorLevel IStatus.[OK|INFO|WARNING|ERROR]
	 * @param message
	 * @param e
	 */
	public static void log(int errorLevel, String message, Throwable e) {
		Status s = makeStatus(errorLevel, message, e);
		ApldevPlugin.getDefault().getLog().log(s);
	}
	public static void logInfo(String message) {
		log(IStatus.INFO, message, new RuntimeException(message));
	}
	
	public static void logInfo(String message, Throwable e) {
		log(IStatus.INFO, message, e);
	}

}
