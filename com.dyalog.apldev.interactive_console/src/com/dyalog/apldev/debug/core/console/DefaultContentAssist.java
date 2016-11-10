package com.dyalog.apldev.debug.core.console;

import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.text.contentassist.ContentAssistant;

import com.dyalog.apldev.debug.core.content.KeyBindingHelper;
import com.dyalog.apldev.debug.core.content.StringUtils;

public class DefaultContentAssist extends ContentAssistant {
	/**
	 * Keeps a boolean indicating if the last request was an auto-activation or not.
	 */
	private boolean lastAutoActivated;
	
	/**
	 * The number of times this content assistant has been activated.
	 */
	public int lastActivationCount;
	
	public DefaultContentAssist() {
		this.enableAutoInsert(true);
		this.lastAutoActivated = true;
		
		try {
			setRepeatedInvocationMode(true);
		} catch (Exception e) {
			
		}
		
		try {
			setRepeatedInvocationTrigger(KeyBindingHelper.getContentAssistProposalBinding());
		} catch (Exception e) {
			
		}
		
		try {
			setStatusLineVisible(true);
		} catch (Exception e) {
			
		}
	}
	
	/**
	 * Shows the completions available and sets the lastAutoActivated flag
	 * and updates the lastActivtionCount.
	 */
	@Override
	public String showPossibleCompletions() {
		lastActivationCount += 1;
		lastAutoActivated = false;
		return super.showPossibleCompletions();
	}

	/**
	 * @return true if the last time was an auto activation (and updates
	 * the internal flag regarding it).
	 */
	public boolean getLastcompletionAutoActivated() {
		boolean r = lastAutoActivated;
		lastAutoActivated = true;
		return r;
	}
	
	public void setIterationStatusMessage(String string) {
		setStatusMessage(StringUtils.format(string, getIterationGesture()));
	}
	
	private String getIterationGesture() {
		TriggerSequence binding = KeyBindingHelper.getContentAssistProposalBinding();
		return binding != null ? binding.format() : "completion key";
	}
	
	/**
	 * Available for stopping the completion
	 */
	@Override
	public void hide() {
		super.hide();
	}
}
