package com.dyalog.apldev.debug.core.console;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

import com.dyalog.apldev.debug.core.console.history.HistoryFilteredList;

/**
 * Select elements out of a list of elements
 */
public abstract class AbstractHistoryElementSListSelectionDialog extends SelectionStatusDialog {

	private ILabelProvider fRenderer;
	private boolean fIgnoreCase = true;
	private boolean fIsMultipleSelection = false;
	private boolean fMatchEmptyString = true;
	private boolean fAllowDuplicates = true;
	private Label fMessage;
	protected HistoryFilteredList fFilteredList;
	private Text fFilterText;
	private ISelectionStatusValidator fValidator;
	private String fFilter = null;
	private String fEmptyListMessage = "";
	private String fEmptySelectionMessage = "";
	private int fWidth = 60;
	private int fHeight = 18;
	private Object[] fSelection = new Object[0];
	
	/**
	 * Constructs a list selection dialog
	 * @param parent
	 * @param renderer ILabelProvider for the list
	 */
	protected AbstractHistoryElementSListSelectionDialog(Shell parent, ILabelProvider renderer) {
		super(parent);
		fRenderer = renderer;
		
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.MAX | SWT.RESIZE);
	}

	/**
	 * Handles default selection (double click).
	 * By default, the OK button is pressed.
	 */
	protected void handleDefaultSelected() {
		if (validateCurrentSelection()) {
			buttonPressed(IDialogConstants.OK_ID);
		}
	}
	
	/**
	 * Specifies if sorting, filtering and folding is case sensitive
	 */
	public void setIgnoreCase(boolean ignoreCase) {
		fIgnoreCase = ignoreCase;
	}
	
	/**
	 * Returns if sorting, filtering and folding is case sensitive.
	 */
	public boolean isCaseIgnored() {
		return fIgnoreCase;
	}
	
	/**
	 * Specifies whether everything or nothing should be filtered on
	 * empty filter string
	 */
	public void setMatchEmptyString(boolean matchEmptyString) {
		fMatchEmptyString = matchEmptyString;
	}
	
	/**
	 * Specifies if multiple selection is allowed
	 */
	public void setMultipleSelection(boolean multipleSelection) {
		fIsMultipleSelection = multipleSelection;
	}
	
	/**
	 * Specifies whether duplicate entries are displayed or not
	 */
	public void setAllowDuplicates(boolean allowDuplicates) {
		fAllowDuplicates = allowDuplicates;
	}
	
	/**
	 * Sets the list size in characters
	 */
	public void setSize(int width, int height) {
		fWidth = width;
		fHeight = height;
	}
	
	/**
	 * Sets the message to be displayed if the list is empty
	 */
	public void setEmptyListMessage(String message) {
		fEmptyListMessage = message;
	}
	
	/**
	 * Sets the message to be displayed if the selection is empty
	 */
	public void setEmptySelectionMessage(String message) {
		fEmptySelectionMessage = message;
	}
	
	/**
	 * Sets an optional validator to check if the selection is valid
	 */
	public void setValidator(ISelectionStatusValidator validator) {
		fValidator = validator;
	}
	
	/**
	 * Sets the elements of the list (widget). To be called within open().
	 */
	protected void setListElements(Object[] elements) {
		Assert.isNotNull(fFilteredList);
		fFilteredList.setElements(elements);
	}
	
	/**
	 * Sets the filter pattern
	 */
	public void setFilter(String filter) {
		if (fFilterText == null) {
			fFilter = filter;
		} else {
			fFilterText.setText(filter);
		}
	}
	
	/**
	 * Return the current filter pattern
	 */
	public String getFilter() {
		if (fFilteredList == null) {
			return fFilter;
		} else {
			return fFilteredList.getFilter();
		}
	}
	
	/**
	 * Returns the indices referring the current selection
	 */
	public int[] getSelectionIndices() {
		Assert.isNotNull(fFilteredList);
		return fFilteredList.getSelectionIndices();
	}
	
	/**
	 * Sets the indices to be selected. E.g.: if the element should be selected,
	 * it would be an array with a single indices (for the last element).
	 */
	public void setSelectionIndices(int[] indices) {
		Assert.isNotNull(fFilteredList);
		fFilteredList.setSelection(indices);
	}
	
	/**
	 * Returns an index referring the first current selection
	 */
	protected int getSelectionIndex() {
		Assert.isNotNull(fFilteredList);
		return fFilteredList.getSelectionIndex();
	}
	
	/**
	 * Sets the selection referenced by an array of elements. Empty or null
	 * array removes selection.
	 */
	protected void setSelection(Object[] selection) {
		Assert.isNotNull(fFilteredList);
	}
	
	/**
	 * Returns an array of the currently selected elements
	 */
	protected Object[] getSelectedElements() {
		Assert.isNotNull(fFilteredList);
		return fFilteredList.getSelection();
	}
	
	/**
	 * Returns all elements which are folded together to one entry in the list
	 */
	public Object[] getFoldedElements(int index) {
		Assert.isNotNull(fFilteredList);
		return fFilteredList.getFoldedElements(index);
	}
	
	/**
	 * Creates the message text widget and sets layout data
	 */
	@Override
	protected Label createMessageArea(Composite composite) {
		Label label = super.createMessageArea(composite);
		
		GridData data = new GridData();
		data.grabExcessVerticalSpace = false;
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = GridData.BEGINNING;
		label.setLayoutData(data);
		
		fMessage = label;
		return label;
	}
	
	/**
	 * Handles a selection changed event. By default, the current selection
	 * is validated.
	 */
	protected void handleSelectionChanged() {
		validateCurrentSelection();
	}
	
	/**
	 * Validates the current selection and updates the status line accordingly
	 */
	protected boolean validateCurrentSelection() {
		Assert.isNotNull(fFilteredList);
		
		IStatus status;
		Object[] elements = getSelectedElements();
		
		if (elements.length > 0) {
			if (fValidator != null) {
				status = fValidator.validate(elements);
			} else {
				status = new Status(IStatus.OK, PlatformUI.PLUGIN_ID,
						IStatus.OK, "", null);
			}
		} else {
			if (fFilteredList.isEmpty()) {
				status = new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID,
						IStatus.ERROR, fEmptyListMessage, null);
			} else {
				status = new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID,
						IStatus.ERROR, fEmptySelectionMessage, null);
			}
		}
		updateStatus(status);
		return status.isOK();
	}
	
	@Override
	protected void cancelPressed() {
		setResult(null);
		super.cancelPressed();
	}
	
	/**
	 * Creates a filtered list
	 */
	protected HistoryFilteredList createFilteredList(Composite parent) {
		int flags = SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL
				| (fIsMultipleSelection ? SWT.MULTI : SWT.SINGLE);
		HistoryFilteredList list = new HistoryFilteredList(parent, flags,
				fRenderer, fIgnoreCase, fAllowDuplicates, fMatchEmptyString);
		
		GridData data = new GridData();
		data.widthHint = convertWidthInCharsToPixels(fWidth);
		data.heightHint = convertHeightInCharsToPixels(fHeight);
		data.grabExcessVerticalSpace = true;
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		list.setLayoutData(data);
		list.setFont(parent.getFont());
		list.setFilter((fFilter == null ? "" : fFilter));
		
		list.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				handleDefaultSelected();
			}
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleWidgetSelected();
			}
		});
		
		fFilteredList = list;
		return list;
	}
	
	private void handleWidgetSelected() {
		Object[] newSelection = fFilteredList.getSelection();
		
		if (newSelection.length != fSelection.length){
			fSelection = newSelection;
			handleSelectionChanged();
		} else {
			for (int i = 0; i != newSelection.length; i++) {
				if (!newSelection[i].equals(fSelection[i])) {
					fSelection = newSelection;
					handleSelectionChanged();
					break;
				}
			}
		}
	}
	
	protected Text createFilterText(Composite parent) {
		Text text = new Text(parent, SWT.BORDER);
		GridData data = new GridData();
		data.grabExcessVerticalSpace = false;
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.BEGINNING;
		text.setLayoutData(data);
		text.setFont(parent.getFont());
		text.setText(fFilter == null ? "" : fFilter);
		Listener listener = new Listener() {
			
			@Override
			public void handleEvent(Event e) {
				fFilteredList.setFilter(fFilterText.getText());
			}
		};
		text.addListener(SWT.Modify, listener);
		text.addKeyListener(new KeyListener() {
		
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_UP) {
					fFilteredList.setFocus();
				}
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				
			}
		});
		
		fFilterText = text;
		return text;
	}
	
	@Override
	public int open() {
		super.open();
		return getReturnCode();
	}
	
	private void access$superCreate() {
		super.create();
	}
	
	@Override
	public void create() {
		
		BusyIndicator.showWhile(null, new Runnable() {
			@Override
			public void run() {
				access$superCreate();
				Assert.isNotNull(fFilteredList);
				if (fFilteredList.isEmpty()) {
					handleEmptyList();
				} else {
					validateCurrentSelection();
					fFilterText.selectAll();
					fFilterText.setFocus();
				}
			}
		});
	}
	
	/**
	 * Handles empty list by disabling widgets
	 */
	protected void handleEmptyList() {
		fMessage.setEnabled(false);
		fFilterText.setEnabled(false);
		fFilteredList.setEnabled(false);
		updateOkState();
	}
	
	/**
	 * Update the enablement of the OK button based on whether or not there
	 * is a selection 
	 */
	protected void updateOkState() {
		Button okButton = getOkButton();
		if (okButton != null) {
			okButton.setEnabled(getSelectedElements().length != 0);
		}
	}

}
