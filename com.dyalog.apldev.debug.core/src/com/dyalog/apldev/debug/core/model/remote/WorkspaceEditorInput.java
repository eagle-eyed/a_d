package com.dyalog.apldev.debug.core.model.remote;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.model.APLDebugTarget;

//public class WorkspaceEditorInput implements IEditorInput {
public class WorkspaceEditorInput implements IStorageEditorInput {

	private EntityWindow entity;
	private APLDebugTarget debugTarget;
	private IProject fProject;
	private ImageDescriptor fImageDscr;
	private String fText;
	
	public WorkspaceEditorInput (EntityWindow entity, APLDebugTarget debugTarget) {
		this.entity = entity;
		this.debugTarget = debugTarget;
		this.fProject = debugTarget.fProject;
		this.fImageDscr = null;
		this.fText = entity.getTextAsSingleStr();
	}
	
	public void setImageDescriptor(ImageDescriptor imageDscr) {
		this.fImageDscr = imageDscr;
	}
	
	public IProject getProject() {
		return fProject;
	}
	
	public APLDebugTarget getDebugTarget() {
		return debugTarget;
	}
	
	public EntityWindow getEntityWindow() {
		return entity;
	}
	
	public boolean isReadOnly() {
		return entity.readOnly;
	}
	
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return fImageDscr;
	}

	@Override
	public String getName() {
		return entity.name;
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return "Workspace-based element";
	}
	
	@Override
	public int hashCode() {
		return entity.name.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		if (obj instanceof WorkspaceEditorInput) {
			WorkspaceEditorInput other = (WorkspaceEditorInput) obj;
			if (!entity.name.equals(other.entity.name))
				return false;
			else if (!fProject.equals(other.getProject()))
				return false;
		}
		return true;
	}

	@Override
	public IStorage getStorage() throws CoreException {
		return new IStorage () {
			
			public InputStream getContents() throws CoreException {
				return new ByteArrayInputStream(fText.getBytes(StandardCharsets.UTF_8));
			}
			
			public IPath getFullPath() {
				return null;
			}
			
			public String getName() {
				return WorkspaceEditorInput.this.getName();
			}
			
			public boolean isReadOnly() {
				return WorkspaceEditorInput.this.isReadOnly();
			}

			@Override
			public <T> T getAdapter(Class<T> adapter) {
				return null;
			}
		};
	}

}
