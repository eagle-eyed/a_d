package com.dyalog.apldev.debug.ui.views;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.model.APLDataStackValue;
import com.dyalog.apldev.debug.core.model.APLDebugTarget;

/**
 * View of the APL VM data stack (Workspace Explorer)
 */
public class DataStackView extends AbstractDebugView implements
		ISelectionListener {
	
	private APLDebugTarget fTarget;
//	private UpdateAction fUpdateAction;

	class StackViewContentProvider implements ITreeContentProvider {
		
		public Object[] getChildren(Object parentElement) {
				try {
					if (parentElement instanceof APLDebugTarget) {
//						int currentNodeId = ((APLDebugTarget) parentElement).getCurrentNodeId();
//						IValue[] data = ((APLDebugTarget) parentElement).getDataStack(currentNodeId);
						
						return ((APLDebugTarget) parentElement).getDataStack(0);
					}	
					else if (parentElement instanceof APLDataStackValue) {
						APLDataStackValue data = (APLDataStackValue) parentElement;
						int nodeId = data.getNodeId();
						if (nodeId != 0)
							return ((APLDebugTarget) data.getDebugTarget()).getDataStack(nodeId);
						return new IValue[0];
					}
				} catch (DebugException e) {
					
				}
			return new Object[0];
		}
		
		public Object getParent(Object element) {
			if (element instanceof IDebugTarget) {
				return null;
			} else {
				return ((IDebugElement) element).getDebugTarget();
			}
		}
		
		public boolean hasChildren(Object element) {
			if (element instanceof IDebugElement) {
				return getChildren(element).length > 0;
			}
			return false;
		}
		
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}
		
		public void dispose() {
			
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			
		}
	}
	
//	public DataStackView() {
//
//	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		update();
	}
	
	/**
	 * Updates the view for the selected target (if suspended)
	 */
	private synchronized void update() {
		IAdaptable adaptable = DebugUITools.getDebugContext();
		fTarget = null;
		if (adaptable != null) {
			IDebugElement element = (IDebugElement) adaptable.getAdapter(IDebugElement.class);
			if (element != null) {
				if (element != null) {
					if (element.getModelIdentifier().equals(APLDebugCorePlugin.ID_APL_DEBUG_MODEL)) {
						fTarget = (APLDebugTarget) element.getDebugTarget();
					}
				}
			}
			Object input = null;
//			if (fTarget != null && fTarget.isSuspended()) {
//				input = fTarget;
//			}
			if (fTarget != null
					&& !fTarget.isTerminated() && !fTarget.isDisconnected())
				input = fTarget;
			
			getViewer().setInput(input);
//			fUpdateAction.setDebugTarget(fTarget);
			getViewer().refresh();
		}
	}

	protected Viewer createViewer(Composite parent) {
		TreeViewer viewer = new TreeViewer(parent);
//		TreeViewer viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setLabelProvider(DebugUITools.newDebugModelPresentation());
		viewer.setContentProvider(new StackViewContentProvider());
		getSite().getWorkbenchWindow().getSelectionService()
			.addSelectionListener(IDebugUIConstants.ID_DEBUG_VIEW, this);
		getSite().setSelectionProvider(viewer);
		return viewer;
	}

	protected void createActions() {
//		fUpdateAction = new UpdateAction(this);
	}

	protected String getHelpContextId() {
		return null;
	}

	protected void fillContextMenu(IMenuManager menu) {
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
//		menu.add(fUpdateAction);
	}

	protected void configureToolBar(IToolBarManager tbm) {
//		tbm.add(fUpdateAction);
	}
	
	public void dispose() {
		getSite().getWorkbenchWindow().getSelectionService()
			.removeSelectionListener(IDebugUIConstants.ID_DEBUG_VIEW, this);
		super.dispose();
	}

}
