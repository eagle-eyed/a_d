package com.dyalog.apldev.interactive_console.console;

import org.eclipse.swt.widgets.Display;

public class RunInUiThread {

	public static void sync(Runnable r) {
		if(Display.getCurrent() == null) {
			Display.getDefault().syncExec(r);
		} else {
			// We already have a hold to it
			r.run();
		}
	}
	
	public static void async(Runnable r) {
		async(r, false);
	}
	
	public static void async(Runnable r, boolean runNowIfInUiThread) {
		Display current = Display.getCurrent();
		if (current == null) {
			Display.getDefault().asyncExec(r);
		} else {
			if (runNowIfInUiThread) {
				r.run();
			} else {
				current.asyncExec(r);
			}
		}
	}
}
