package com.dyalog.apldev.debug.core.console.history;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.Accessible;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.misc.StringMatcher;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * A composite widget which holds a list of elements for user selection. The
 * elements are sorted alphabetically. Optionally, the elements can be filtered
 * and duplicate entries can be hidden (folding)
 */
public class HistoryFilteredList extends Composite {

	/**
	 * The FilterMatcher is the interface used to check filtering crite
	 */
	public interface FilterMatcher {
		/**
		 * Sets the filter
		 * @param pattern the filter pattern
		 * @param ignoreCase a flag indicating whether pattern matching is case
		 * insensitive or not
		 * @param ignoreWildCards a flag indicating whether wildcard characters are
		 * interpreted or not
		 */
		void setFilter(String pattern, boolean ignoreCase, boolean ignoreWildCards);
		
		/**
		 * 
		 * @param element to test against
		 * @return <code>true</code> if the object matches the pattern,
		 * <code>false</code> otherwise. <code>setFilter</code> must have been
		 * called at least once prior to a call to this method
		 */
		boolean match(Object element);
	}
	
	private class DefaultFilterMatcher implements FilterMatcher {
		private StringMatcher fMatcher;

		@Override
		public void setFilter(String pattern, boolean ignoreCase, boolean ignoreWildCards) {
			fMatcher = new StringMatcher(pattern + '*', ignoreCase, ignoreWildCards);
		}

		@Override
		public boolean match(Object element) {
			return fMatcher.match(fLabelProvider.getText(element));
		}
	}
	
	private Table fList;
	ILabelProvider fLabelProvider;
	private boolean fMatchEmptyString = true;
	private boolean fIgnoreCase;
	private boolean fAllowDuplicates;
	private String fFilter = "";
	Object[] fElements = new Object[0];
	Label[] fLabels;
	Vector<Image> fImages = new Vector<Image>();
	int[] fFoldedIndices;
	int fFoldedCount;
	int[] fFilteredIndices;
	int fFilteredCount;
	private FilterMatcher fFilterMatcher = new DefaultFilterMatcher();
	TableUpdateJob fUpdateJob;
	
	/**
	 * Label is a private class used for comparing list objects
	 */
	private static class Label {
		/**
		 * The string portion of the label.
		 */
		public final String string;
		/**
		 * The image portion of the label
		 */
		public final Image image;
		/**
		 * Create a new instance of label
		 * 
		 * @param newString
		 * @param image
		 */
		public Label(String newString, Image image) {
			if (newString == null) {
				this.string = "";
			} else {
				this.string = newString;
			}
			this.image = image;
		}
		
		/**
		 * Return whether or not the receiver is the same as label
		 * 
		 * @param label
		 * @return boolean
		 */
		public boolean equals(Label label) {
			if (label == null) {
				return false;
			}
			// if the string portions match (whether null or not), fall through
			// and check the image portion
			if (string == null && label.string != null) {
				return false;
			}
			if ((string != null) && (!string.equals(label.string))) {
				return false;
			}
			if (image == null) {
				return label.image == null;
			}
			return image.equals(label.image);
		}
	}
	
	/**
	 * Constructs a new filtered list
	 * 
	 * @param parent the parent composite
	 * @param style the widget style
	 * @param labelProvider the label renderer
	 * @param ignoreCase specifies whether sorting and folding is case sensitive
	 * @param allowDuplicates specifies whether folding of duplicates is desired
	 * @param matchEmptyString specifies whether empty filter strings should 
	 * filter everything or nothing
	 */
	public HistoryFilteredList(Composite parent, int style, ILabelProvider labelProvider,
			boolean ignoreCase, boolean allowDuplicates, boolean matchEmptyString) {
		super(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);
		fList = new Table(this, style);
		fList.setLayoutData(new GridData(GridData.FILL_BOTH));
		fList.setFont(parent.getFont());
		fList.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				fLabelProvider.dispose();
				if (fUpdateJob != null) {
					fUpdateJob.cancel();
				}
			}
		});
		fLabelProvider = labelProvider;
		fIgnoreCase = ignoreCase;
		fAllowDuplicates = allowDuplicates;
		fMatchEmptyString = matchEmptyString;
	}

	/**
	 * Sets the list of elements
	 * 
	 * @param elements the elements to be shown in the list
	 */
	public void setElements(Object[] elements) {
		if (elements == null) {
			fElements = new Object[0];
		} else {
			// copy list for sorting
			fElements = new Object[elements.length];
			System.arraycopy(elements, 0, fElements, 0, elements.length);
		}
		int length = fElements.length;
		// fill labels
		fLabels = new Label[length];
		Set<Image> imageSet = new HashSet<Image>();
		for (int i = 0; i != length; i++) {
			String text = fLabelProvider.getText(fElements[i]);
			Image image = fLabelProvider.getImage(fElements[i]);
			fLabels[i] = new Label(text, image);
			imageSet.add(image);
		}
		fImages.clear();
		fImages.addAll(imageSet);
		fFilteredIndices = new int[length];
		fFoldedIndices = new int[length];
		updateList();
	}
	
	/**
	 * Tests list (before folding and filtering)
	 */
	public boolean isEmpty() {
		return (fElements == null) || (fElements.length == 0);
	}
	
	/**
	 * Sets the filter matcher
	 */
	public void setFilterMatcher(FilterMatcher filterMatcher) {
		Assert.isNotNull(filterMatcher);
		fFilterMatcher = filterMatcher;
	}
	
	/**
	 * Adds a selection listener
	 */
	public void addSelectionListener(SelectionListener listener) {
		fList.addSelectionListener(listener);
	}
	
	/**
	 * Removes a selection listener
	 */
	public void removeSelectionListener(SelectionListener listener) {
		fList.removeSelectionListener(listener);
	}
	
	/**
	 * Sets the selection of the list
	 */
	public void setSelection(int[] selection) {
		if (selection == null || selection.length == 0) {
			fList.deselectAll();
		} else {
			if (fUpdateJob == null) {
				fList.setSelection(selection);
				fList.notifyListeners(SWT.Selection, new Event());
			} else {
				fUpdateJob.updateSelection(selection);
			}
		}
	}
	
	/**
	 * @return an array of indices specifying the current selection
	 */
	public int[] getSelectionIndices() {
		return fList.getSelectionIndices();
	}
	
	/**
	 * @return the index of the selection
	 */
	public int getSelectionIndex() {
		return fList.getSelectionIndex();
	}
	
	/**
	 * Sets the selection of the list
	 */
	public void setSelection(Object[] elements) {
		if (elements == null || elements.length == 0) {
			fList.deselectAll();
			return;
		}
		if (fElements == null) {
			return;
		}
		// fill indices
		int[] indices = new int[elements.length];
		for (int i = 0; i != elements.length; i++) {
			int j;
			for (j = 0; j != fFoldedCount; j++) {
				int max = (j == fFoldedCount - 1) ? fFilteredCount : fFoldedIndices[j + 1];
				int l;
				for (l = fFoldedIndices[j]; l != max; l++) {
					// found matching element?
					if (fElements[fFilteredIndices[l]].equals(elements[i])) {
						indices[i] = j;
						break;
					}
				}
				if (l != max) {
					break;
				}
			}
			// not found
			if (j == fFoldedCount) {
				indices[i] = 0;
			}
		}
		setSelection(indices);
	}
	
	/**
	 * Returns an array of the selected elements
	 */
	public Object[] getSelection() {
		if (fList.isDisposed() || (fList.getSelectionCount() == 0)) {
			return new Object[0];
		}
		int[] indices = fList.getSelectionIndices();
		Object[] elements = new Object[indices.length];
		for (int i = 0; i != indices.length; i++) {
			elements[i] = fElements[fFilteredIndices[fFoldedIndices[indices[i]]]];
		}
		return elements;
	}
	
	/**
	 * Sets the filter pattern
	 */
	public void setFilter(String filter) {
		fFilter = (filter == null) ? "" : filter;
		updateList();
	}
	
	private void updateList() {
		fFilteredCount = filter();
		fFoldedCount = fold();
		if (fUpdateJob != null) {
			fUpdateJob.cancel();
		}
		fUpdateJob = new TableUpdateJob(fList, fFoldedCount);
		fUpdateJob.schedule();
	}
	
	/**
	 * Returns the filter pattern
	 */
	public String getFilter() {
		return fFilter;
	}
	
	/**
	 * Returns all elements which are folded together
	 */
	public Object[] getFoldedElements(int index) {
		if ((index < 0) || (index >= fFoldedCount)) {
			return null;
		}
		int start = fFoldedIndices[index];
		int count = (index == fFoldedCount - 1) ? fFilteredCount - start
				: fFoldedIndices[index + 1] - start;
		Object[] elements = new Object[count];
		for (int i = 0; i != count; i++) {
			elements[i] = fElements[fFilteredIndices[start + i]];
		}
		return elements;
	}
	
	/**
	 * Folds duplicate entries
	 * 
	 * @return the number of elements after folding 
	 */
	private int fold() {
		if (fAllowDuplicates) {
			for (int i = 0; i != fFilteredCount; i++) {
				fFoldedIndices[i] = i;
			}
			return fFilteredCount;
		}
		int k = 0;
		Label last = null;
		for (int i = 0; i != fFilteredCount; i++) {
			int j = fFilteredIndices[i];
			Label current = fLabels[j];
			if (!current.equals(last)) {
				fFoldedIndices[k] = i;
				k++;
				last = current;
			}
		}
		return k;
	}
	
	/**
	 * Filters the list with the filter pattern
	 * 
	 * @return int the number of elements after filtering 
	 */
	private int filter() {
		if (((fFilter == null) || (fFilter.length() == 0)) && !fMatchEmptyString) {
			return 0;
		}
		fFilterMatcher.setFilter(fFilter.trim(), fIgnoreCase, false);
		int k = 0;
		for (int i = 0; i != fElements.length; i++) {
			if (fFilterMatcher.match(fElements[i])) {
				fFilteredIndices[k++] = i;
			}
		}
		return k;
	}
	
	private class TableUpdateJob extends WorkbenchJob {
		final Table fTable;
		final int fCount;
		private int currentIndex = 0;
		int[] indicesToSelect;
		private boolean readyForSelection = false;
		
		/**
		 * Create a new instance of a job used to update the table
		 * 
		 * @param table
		 * @param count the number of items to update per running
		 */
		public TableUpdateJob(Table table, int count) {
			super(WorkbenchMessages.FilteredList_UpdateJobName);
			setSystem(true);
			fTable = table;
			fCount = count;
		}
		
		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (fTable.isDisposed()) {
				return Status.CANCEL_STATUS;
			}
			int itemCount = fTable.getItemCount();
			// Remove excess items
			if (fCount < itemCount) {
				fTable.setRedraw(false);
				fTable.remove(fCount, itemCount - 1);
				fTable.setRedraw(true);
				itemCount = fTable.getItemCount();
			}
			// table empty -> no selection
			if (fCount == 0) {
				fTable.notifyListeners(SWT.Selection,  new Event());
				return Status.OK_STATUS;
			}
			// How many we are going to do this time
			int iterations = Math.min(250,  fCount - currentIndex);
			for (int i = 0; i < iterations; i++) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				final TableItem item = (currentIndex < itemCount)
						? fTable.getItem(currentIndex)
							: new TableItem(fTable, SWT.NONE);
				final Label label = fLabels[fFilteredIndices[fFoldedIndices[currentIndex]]];
				item.setText(label.string);
				item.setImage(label.image);
				currentIndex++;
			}
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			if (currentIndex < fCount) {
				schedule(0);
			} else {
				if (indicesToSelect == null) {
					// Make a default selection in the table if there is none.
					// If a selection has already been made, honor it.
					// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=112146
					if (fCount > 0) {
						if (fTable.getSelectionIndices().length == 0) {
							defaultSelect();
						} else {
							// There is a selection, but it likely hasn't changed since the
							// job started. Force a selection notification, since the
							// items represented by the selection have changed.
							// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=119456
							fTable.notifyListeners(SWT.Selection, new Event());
						}
					}
				} else {
					// Set the selection as indicated
					selectAndNotify(indicesToSelect);
				}
				// This flag signifies that the selection can now be directly
				// updated in the widget
				readyForSelection = true;
			}
			return Status.OK_STATUS;
		}
		
		/**
		 * Update the selection for the supplied indices
		 */
		void updateSelection(final int[] indices) {
			indicesToSelect = indices;
			if (readyForSelection) {
				selectAndNotify(indices);
			}
		}
		
		/**
		 * Select the first element if there is no selection
		 */
		private void defaultSelect() {
			/**
			 * Reset to the first selection if no index has been queued
			 */
			selectAndNotify(new int[] { 0 });
		}
		
		/**
		 * Select the supplied indices and notify any listeners
		 */
		private void selectAndNotify(final int[] indices) {
			// It is possible that the table was disposed
			// before the update finished. If so then leave
			if (fTable.isDisposed()) {
				return;
			}
			fTable.setSelection(indices);
			fTable.notifyListeners(SWT.Selection, new Event());
		}
	}
	
	/**
	 * Returns whether or not duplicates are allowed
	 */
	public boolean getAllowDuplicates() {
		return fAllowDuplicates;
	}
	
	/**
	 * Sets whether or not duplicates are allowed. If this value is set the
	 * items should be set again for this value to take effect.
	 */
	public void setAllowDuplicates(boolean allowDuplicates) {
		this.fAllowDuplicates = allowDuplicates;
	}
	
	/**
	 * Returns whether or not case should be ignored
	 */
	public boolean getIgnoreCase() {
		return fIgnoreCase;
	}
	
	/**
	 * Sets whether or not case should be ignored. If this value is set the
	 * items should be set again for this value to take effect
	 */
	public void setIgnoreCase(boolean ignoreCase) {
		this.fIgnoreCase = ignoreCase;
	}
	
	/**
	 * Returns whether empty filter strings should filter everything or nothing
	 */
	public boolean getMatchEmptyString() {
		return fMatchEmptyString;
	}
	
	/**
	 * Sets whether empty filter strings should filter everything or nothing.
	 * If this value is set the items should be set again for this value to
	 * take effect.
	 */
	public void setMatchEmptyString(boolean matchEmptyString) {
		this.fMatchEmptyString = matchEmptyString;
	}
	
	/**
	 * Returns the label provider for the items
	 */
	public ILabelProvider getLabelProvider() {
		return fLabelProvider;
	}
	
	/**
	 * Sets the label provider. If this value is set the items should be set
	 * again for this value to take effect.
	 */
	public void setLabelProvider(ILabelProvider labelProvider) {
		this.fLabelProvider = labelProvider;
	}
	
	/**
	 * Returns the accessible object for the receiver. If this is the first
	 * time this object is request, then the object is created and returned.
	 * 
	 * @return accessible object
	 * @exception SWTException <ul>
	 * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
	 * created the receiver</li>
	 * </ul>
	 * @see Accessible#addAccessibleListener
	 * @see Accessible#addAccessibleControlListener
	 */
	@Override
	public Accessible getAccessible() {
		return fList.getAccessible();
	}
}
