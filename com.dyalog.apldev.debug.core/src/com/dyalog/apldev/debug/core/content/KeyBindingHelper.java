package com.dyalog.apldev.debug.core.content;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;

/**
 * Helper for knowing about keybingings and related actions
 */
public class KeyBindingHelper {

	// pre-defined helpers 
	/**
	 * @return true if the given event matches a content assistant keystroke
	 * and false otherwise).
	 */
	public static boolean matchesContentAssistKeybinding(KeyEvent event) {
		return matchesKeybinding(event, ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
	}
	
	/**
	 * @return the key sequence that is the best match for a content assist request.
	 */
	public static KeySequence getContentAssistProposalBinding() {
		return getCommandKeyBinding(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
	}

	/**
	 * @return true if the given event matches a quick assistant keystroke
	 */
	public static boolean matchesQuickAssistKeybinding(KeyEvent event) {
		return matchesKeybinding(event, ITextEditorActionDefinitionIds.QUICK_ASSIST);
	}
	
	/**
	 * @return the key sequence that is the best match for a quick assist request.
	 */
	public static KeySequence getQuickAssistProposalBinding() {
		return getCommandKeyBinding(ITextEditorActionDefinitionIds.QUICK_ASSIST);
	}
	
	/**
	 * @param event the key event to be checked
	 * @param commandId the command to be checked
	 * @return true if the given key event can trigger the passed command
	 */
	public static boolean matchesKeybinding(KeyEvent event, String commandId) {
		int keyCode = event.keyCode;
		int stateMask = event.stateMask;
		
		return matchesKeybinding(keyCode, stateMask, commandId);
	}
	
	public static KeySequence getKeySequence(String text) throws ParseException, IllegalArgumentException {
		KeySequence keySequence = KeySequence.getInstance(KeyStroke.getInstance(text));
		return keySequence;
	}
	
	public static boolean matchesKeybinding(int keyCode, int stateMask, String commandId) {
		final IBindingService bindingSvc = (IBindingService) PlatformUI.getWorkbench()
				.getAdapter(IBindingService.class);
		TriggerSequence[] activeBindingsFor = bindingSvc.getActiveBindingsFor(commandId);
		
		for (TriggerSequence seq : activeBindingsFor) {
			if (seq instanceof KeySequence) {
				KeySequence keySequence = (KeySequence) seq;
				if (matchesKeybinding(keyCode, stateMask, keySequence)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public static boolean matchesKeybinding(int keyCode, int stateMask, KeySequence keySequence) {
		KeyStroke[] keyStrokes = keySequence.getKeyStrokes();
		
		for (KeyStroke keyStroke : keyStrokes) {
			if (keyStroke.getNaturalKey() == keyCode
					&& ((keyStroke.getModifierKeys() & stateMask) != 0 
							|| keyStroke.getModifierKeys() == stateMask)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param commandId the command we want to know about
	 * @return the 'best' key sequence that will activate the given command
	 */
	public static KeySequence getCommandKeyBinding(String commandId) {
		Assert.isNotNull(commandId);
		final IBindingService bindingSvc = (IBindingService) PlatformUI.getWorkbench()
				.getAdapter(IBindingService.class);
		
		TriggerSequence keyBinding = bindingSvc.getBestActiveBindingFor(commandId);
		if (keyBinding instanceof KeySequence) {
			return (KeySequence) keyBinding;
		}
		
		List<Tuple<Binding, ParameterizedCommand>> matches = new ArrayList<Tuple<Binding, ParameterizedCommand>>();
		// give a spin on all the bindings
		Binding[] bindings = bindingSvc.getBindings();
		for (Binding binding : bindings) {
			ParameterizedCommand command = binding.getParameterizedCommand();
			if (command != null) {
				if (commandId.equals(command.getId())) {
					matches.add(new Tuple<Binding, ParameterizedCommand>(binding, command));
				}
			}
		}
		for (Tuple<Binding, ParameterizedCommand> tuple : matches) {
			if (tuple.o1.getTriggerSequence() instanceof KeySequence) {
				KeySequence keySequence = (KeySequence) tuple.o1.getTriggerSequence();
				return keySequence;
			}
		}
		
		return null;
	}
	
	/**
	 * @param fActivateEditorBinding
	 */
	public static void executeCommand(String commandId) {
		IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench()
				.getService(IHandlerService.class);
		try {
			handlerService.executeCommand(commandId, null);
		} catch (Exception e) {
			APLDebugCorePlugin.log(e);
		}
	}
}
