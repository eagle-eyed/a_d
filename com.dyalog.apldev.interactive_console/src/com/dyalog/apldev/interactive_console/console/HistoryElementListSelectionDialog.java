package com.dyalog.apldev.interactive_console.console;

import java.util.Arrays;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class HistoryElementListSelectionDialog
	extends AbstractHistoryElementSListSelectionDialog {

	private Object[] fElements;
	
	/**
	 * Creates a list selection dialog
	 */
	public HistoryElementListSelectionDialog(Shell parent,
			ILabelProvider renderer) {
		super(parent, renderer);
	}
	
	/**
	 * Sets the elements of the list
	 */
	public void setElements(Object[] elements) {
		fElements = elements;
	}
	
	@Override
	protected void computeResult() {
		setResult(Arrays.asList(getSelectedElements()));
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite contents = (Composite) super.createDialogArea(parent);
		
		createMessageArea(contents);
		createFilteredList(contents);
		createFilterText(contents);
		
		setListElements(fElements);
		setSelectionIndices(new int[] { fElements.length - 1 });
		setSelection(getInitialElementSelections().toArray());
		
		return contents;
	}
}
