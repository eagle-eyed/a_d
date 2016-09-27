package com.dyalog.apldev.debug.ui.editor;

import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.dyalog.apldev.debug.core.model.APLStackFrame;
import com.dyalog.apldev.debug.core.model.APLThread;

public class PopFrameActionDelegate implements IObjectActionDelegate,
		IActionDelegate2 {
	
	private APLThread fThread = null;

//	public PopFrameActionDelegate() {
//	}

	public void run(IAction action) {
		try {
			fThread.pop();
		} catch (DebugException e) {
			
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			Object element = ss.getFirstElement();
			if (element instanceof APLStackFrame) {
				APLStackFrame frame = (APLStackFrame) element;
				fThread = (APLThread) frame.getThread();
				try {
					action.setEnabled(fThread.canPop() && fThread.getTopStackFrame().equals(frame));
				} catch (DebugException e) {
					
				}
				return;
			}
		}
		action.setEnabled(false);
	}

	public void init(IAction action) {

	}

	public void dispose() {
		fThread = null;
	}

	public void runWithEvent(IAction action, Event event) {
		run(action);
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {

	}

}
