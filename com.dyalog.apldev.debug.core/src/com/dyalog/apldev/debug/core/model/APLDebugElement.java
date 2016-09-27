package com.dyalog.apldev.debug.core.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;

/**
 * Common function for PDA debug elements.
 */
public class APLDebugElement extends DebugElement {

	/**
	 * Constructs a new debug element in the given target.
	 * 
	 * @param target debug target
	 */
	public APLDebugElement(IDebugTarget target) {
		super(target);
	}

	@Override
	public String getModelIdentifier() {
		return APLDebugCorePlugin.ID_APL_DEBUG_MODEL;
	}

	/**
	 * Sends a request to the APL interpreter, and waits for and returns the reply.
	 * 
	 * @param request command
	 * @return reply
	 * @throws DebugException if the request fails
	 */
	public String sendRequest(String request) throws DebugException {
		return getAPLDebugTarget().sendRequest(request);
	}
	
	/**
	 * Send Request without waiting reply
	 */
	public void sendRequestNoReply(String request) throws DebugException {
		getAPLDebugTarget().sendRequestNoReply(request);
	}
	
	/**
	 * Returns the debug target
	 */
	protected APLDebugTarget getAPLDebugTarget() {
		return (APLDebugTarget) getDebugTarget();
	}
	
	/**
	 * Return the breakpoint manager
	 */
	protected IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();
	}
}
