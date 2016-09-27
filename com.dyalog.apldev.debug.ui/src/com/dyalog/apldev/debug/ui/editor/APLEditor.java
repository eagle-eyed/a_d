package com.dyalog.apldev.debug.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;

//import org.eclipse.jface.action.IAction;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
//import org.eclipse.ui.texteditor.ContentAssistAction;
//import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.model.APLDebugTarget;
import com.dyalog.apldev.debug.core.model.remote.EntityWindow;
import com.dyalog.apldev.debug.core.model.remote.WorkspaceEditorInput;
import com.dyalog.apldev.debug.ui.APLDebugUIPlugin;

/**
 * APL editor
 */
public class APLEditor extends AbstractDecoratedTextEditor {
//public class APLEditor extends TextEditor {

	/**
	 * Creates a APL editor
	 */
	public APLEditor() {
		super();
		setSourceViewerConfiguration(new PDASourceViewerConfiguration());
		setRulerContextMenuId("apl.editor.rulerMenu");
		setEditorContextMenuId("apl.editor.editorMenu");
		
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (input instanceof WorkspaceEditorInput) {
			WorkspaceEditorInput inputWS = (WorkspaceEditorInput) input;
			
			inputWS.setImageDescriptor(
					APLDebugUIPlugin.getImageCache().getDescriptor(APLDebugUIPlugin.MAIN_ICON));
			super.init(site, inputWS);
		} else {
			super.init(site, input);
		}
	}
	
	@Override
	public Image getTitleImage() {
		return getEditorInput().getImageDescriptor().createImage();
	}
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
	}
	
	@Override
		public void doSave(IProgressMonitor progressMonitor) {
			super.doSave(progressMonitor);

			// get editor source location
			IEditorInput input = getEditorInput();
			String name = "";
			IProject project = null;
			APLDebugTarget[] debugTargets = new APLDebugTarget[0];
			if (input instanceof WorkspaceEditorInput) {
				// Interpreter workspace source
				WorkspaceEditorInput wsInput = (WorkspaceEditorInput) input;
				APLDebugTarget debugTarget = wsInput.getDebugTarget();
				if (debugTarget != null && !debugTarget.isTerminated()
						&& !debugTarget.isDisconnected()) {
					debugTargets = new APLDebugTarget[1];
					debugTargets[0] = debugTarget;
					name = wsInput.getName();
				}
			} else if (input instanceof IFileEditorInput) {
				// Source in Eclipse Project
				IResource resource = input.getAdapter(IResource.class);
				project = resource.getProject();
				name = resource.getName();
				String extension = resource.getFileExtension();
				if (extension.length() > 0) {
					name = name.substring(0,name.length() - extension.length() - 1);
				}
				// Check if needing to update interpreter workspace
				ILaunchManager launchMan = DebugPlugin.getDefault().getLaunchManager();
				IDebugTarget[] targets = launchMan.getDebugTargets();
				List <APLDebugTarget> debugTargetList = new ArrayList <> ();
				for (int i = 0; i < targets.length; i++) {
					if (targets[i] instanceof APLDebugTarget
							&& !targets[i].isTerminated() && !targets[i].isDisconnected()) {
						if (project.equals(
								((APLDebugTarget) targets[i]).getProject())) {
							debugTargetList.add((APLDebugTarget) targets[i]);
							break;
						}
					}
				}
				if (debugTargetList.size() > 0) {
					debugTargets = new APLDebugTarget[debugTargetList.size()];
					debugTargetList.toArray(debugTargets);
				}
			}
			String[] text = new String[0];
			if (debugTargets.length > 0) {
				// get text from editor
				IDocument doc = getDocumentProvider().getDocument(getEditorInput());
				text = new String[doc.getNumberOfLines()];
				try {
					for (int i = 0; i < text.length; i++) {
						String delimiter = doc.getLineDelimiter(i);
						int delimiterLen = delimiter != null ? delimiter.length() : 0;
							text[i] = doc.get(doc.getLineOffset(i),
									doc.getLineLength(i) - delimiterLen);
					}
				} catch (BadLocationException e) {
					APLDebugCorePlugin.log(e);
					return;
				}
			}
			
			for (int i = 0; i < debugTargets.length; i++) {
					// check if debug target belong to current project
					debugTargets[i].loadToWorkspace(text, name);
			}
		}

//	protected void createActions() {
//		super.createActions();
//		ResourceBundle bundle = ResourceBundle.getBundle(
//				"apldev.debug.ui.editor.APLEditorMessages");
//		IAction action = new ContentAssistAction(bundle, "ContentAssistProposal.", this);
//		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
//		setAction("ContentAssistProposal", action);
//	}
	
}
