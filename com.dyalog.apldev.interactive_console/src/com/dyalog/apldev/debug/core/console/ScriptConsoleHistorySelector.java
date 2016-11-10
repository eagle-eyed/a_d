package com.dyalog.apldev.debug.core.console;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * Select one element from the history
 */
public class ScriptConsoleHistorySelector {

	public static List<String> select(final ScriptConsoleHistory history) {
		HistoryElementListSelectionDialog dialog
			= new HistoryElementListSelectionDialog(Display.getDefault()
					.getActiveShell(), getLabelProvider()) {
			private static final int CLEAR_HISTORY_ID = IDialogConstants.CLIENT_ID + 1;
			
			@Override
			protected void createButtonsForButtonBar(Composite parent) {
				super.createButtonsForButtonBar(parent);
				createButton(parent, CLEAR_HISTORY_ID, "Clear History", false);
			}
			
			@Override
			protected void buttonPressed(int buttonId) {
				if (buttonId == CLEAR_HISTORY_ID) {
					history.clear();
					// After deleting the history, close the dialog
					cancelPressed();
				}
				super.buttonPressed(buttonId);
			}
		};
		
		dialog.setTitle("Command history");
		final List<String> selectFrom = history.getAsList();
		dialog.setElements(selectFrom.toArray(new String[0]));
		dialog.setEmptySelectionMessage("No command selected");
		dialog.setAllowDuplicates(true);
		dialog.setBlockOnOpen(true);
		dialog.setSize(100, 25); // size in number of chars
		dialog.setMessage("Select command(s) to be executed");
		dialog.setMultipleSelection(true);
		
		if (dialog.open() == SelectionDialog.OK){
			Object[] result = dialog.getResult();
			if (result != null) {
				ArrayList<String> list = new ArrayList<String>();
				for (Object o : result) {
					list.add(o.toString());
				}
				return list;
			}
		}
		return null;
	}
	
	/**
	 * @return a label provider that'll show the history command as a string
	 */
	private static ILabelProvider getLabelProvider() {
		return new ILabelProvider() {
			
			@Override
			public Image getImage(Object element) {
				return null;
			}
			
			@Override
			public String getText(Object element) {
				return element.toString();
			}
			
			@Override
			public void addListener(ILabelProviderListener listener) {
				
			}
			
			@Override
			public void dispose() {
				
			}
			
			@Override
			public boolean isLabelProperty(Object element, String property) {
				return true;
			}
			
			@Override
			public void removeListener(ILabelProviderListener listener) {
				
			}
		};
	}
}
