package com.dyalog.apldev.debug.ui.presentation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.breakpoints.APLLineBreakpoint;
import com.dyalog.apldev.debug.core.model.APLDataStackValue;
import com.dyalog.apldev.debug.core.model.APLDebugTarget;
import com.dyalog.apldev.debug.core.model.APLStackFrame;
import com.dyalog.apldev.debug.core.model.APLThread;
import com.dyalog.apldev.debug.core.model.remote.WorkspaceEditorInput;
import com.dyalog.apldev.debug.ui.APLDebugUIPlugin;

/**
 * Renders APL debug elements
 */
public class APLModelPresentation extends LabelProvider implements
		IDebugModelPresentation {

	@Override
	public Image getImage(Object element) {
		ImageRegistry imageRegistry = APLDebugUIPlugin.getDefault().getImageRegistry();
		ImageCache imageCache = APLDebugUIPlugin.getImageCache();
		if (element instanceof APLDataStackValue) {
			
			switch (((APLDataStackValue) element).getType()) {
			case -1:
				// invalid name
				return imageCache.get("greendot.gif");
			case 0:
				// unused name
				return imageCache.get("greendot_big.gif");
			case 1:
				// label
				return imageCache.get("label_1.gif");
			case 2:
				// variable
//				return imageCache.get("WSE/variable_tab.gif");
				return imageCache.get("greendot.gif");
			case 3:
				// function
//				return imageCache.get("WSE/func.gif");
				return imageCache.get("greendot_big.gif");
			case 4:
				// operator
				return imageCache.get("op1.gif");
			case 9:
				// object
				return imageCache.get("WSE/class_obj.gif");
			}
			return imageRegistry.getDescriptor(APLDebugUIPlugin.IMG_VAR).createImage();
		}
		return null;
	}

	public IEditorInput getEditorInput(Object element) {
		if (element instanceof IFile) {
			return new FileEditorInput((IFile) element);
		} else if (element instanceof WorkspaceEditorInput) {
			return (WorkspaceEditorInput) element;
		} else if (element instanceof APLLineBreakpoint) {
//			APLLineBreakpoint breakpoint = (APLLineBreakpoint) element;
			// check if that is interpreter workspace resource
			// TODO if APLLineBreakpoint haven't file resource
			return new FileEditorInput((IFile) ((ILineBreakpoint) element)
					.getMarker().getResource());
		}
		return null;
	}

	public String getEditorId(IEditorInput input, Object element) {
		if (element instanceof IFile || element instanceof ILineBreakpoint) {
			return APLDebugCorePlugin.FUNCTION_EDITOR_ID;
		} else if (element instanceof WorkspaceEditorInput) {
			return APLDebugCorePlugin.FUNCTION_EDITOR_ID;
		}
		else {
			System.out.println("Model for non file source?");
		}
		return null;
	}

	public void setAttribute(String attribute, Object value) {

	}
	
	public String getText(Object element) {
		if (element instanceof APLDebugTarget) {
			return getTargetText((APLDebugTarget) element);
		} else if (element instanceof APLThread) {
			return getThreadText((APLThread) element);
		} else if (element instanceof APLStackFrame) {
			return getStackFrameText((APLStackFrame) element);
		} else if (element instanceof APLLineBreakpoint) {
			// default value
			return null;
		}
		return null;
	}
	
	/**
	 * Returns a label for the given debug target
	 * 
	 * @param target debug target
	 * @return a label for the given debug target
	 */
	private String getTargetText(APLDebugTarget target) {
		try {
//			String projectName = target.getLaunch().getLaunchConfiguration()
//					.getAttribute(DebugCorePlugin.ATTR_PROJECT, (String) null);
			if (target != null) {
				String label = "";
				if (target.isTerminated()) {
					label = "<terminated>";
				} else if (target.isDisconnected()) {
					label = "<disconnected>";
					String message = target.getDisconnectMessage();
					if (message != null && message.length() > 0) {
						return label + " " + target.getDisconnectMessage();
					}
				}
				return label + "APL [" + target.getName() + "]";
			}
		} catch (CoreException e) {
			
		}
		return "APL";
	}
	
	/**
	 * Returns a label for the given stack frame
	 * 
	 * @param frame a stack frame
	 * @return a label for the given stack frame
	 */
	private String getStackFrameText(APLStackFrame frame) {
		try {
			return frame.getName() + " (line: " + frame.getLineNumber() + ")";
		} catch (DebugException e) {
			
		}
		return null;
	}
	
	/**
	 * Returns a label for the given thread
	 * 
	 * @param thread a thread
	 * @return a label for the given thread
	 * @throws DebugException 
	 */
	private String getThreadText(APLThread thread) {
		String label = thread.getName();
		if (thread.isStepping()) {
			label += " (stepping)";
		} else if (thread.isSuspended()) {
			// describe suspending reason line breakpoint, run to line
			IBreakpoint[] breakpoints = thread.getBreakpoints();
			if (breakpoints.length == 0) {
				if (thread.getError() == null) {
					label += " (suspended)";
				} else {
					label += " (" + thread.getError() + ")";
				}
			} else {
				IBreakpoint breakpoint = breakpoints[0]; // there can only be one in PDA
				if (breakpoint instanceof APLLineBreakpoint) {
					APLLineBreakpoint aplBreakpoint = (APLLineBreakpoint) breakpoint;
					if (aplBreakpoint.isRunToLineBreakpoint()) {
						label += " (run to line)";
					} else {
						label += " (suspended at line breakpoint)";
					}
				}
			} 
		} else if (thread.isTerminated()) {
			label = "<terminated> " + label;
		}
		return label;
	}
	
	public void computeDetail(IValue value, IValueDetailListener listener) {
		String detail = "";
		try {
			detail = value.getValueString();
		} catch (DebugException e) {
			
		}
		listener.detailComputed(value, detail);
	}

}
