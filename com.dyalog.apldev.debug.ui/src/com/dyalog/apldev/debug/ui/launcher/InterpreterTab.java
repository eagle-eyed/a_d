package com.dyalog.apldev.debug.ui.launcher;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.ui.APLDebugUIPlugin;

public class InterpreterTab extends AbstractLaunchConfigurationTab {

	// Widgets
	// Interpreter launch widgets
	private Button fLaunchButton;
	private Text fInterpreterText;
	private Button fFileBrowse;
	// Program arguments widgets
	private Text fPrgmArgumentsText;
	// Working directory widget
	private APLWorkingDirectoryBlock fWorkingDirectoryBlock;
	// Communication
	private Text fPortText;
	private Button fConnectButton;
	private Text fHostText;
	private Button fRideButton;
	
	public InterpreterTab(MainModuleTab mainModuleTab) {
		fWorkingDirectoryBlock = new APLWorkingDirectoryBlock();
	}

	protected AbstractLaunchConfigurationTab createWorkingDirectoryBlock(MainModuleTab mainModuleTab) {
		return new WorkingDirectoryBlock(mainModuleTab);
	}
	
	public void createControl(Composite parent) {
		Font font = parent.getFont();
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, true);
		comp.setLayout(layout);
		comp.setFont(font);

		GridData gd = new GridData(GridData.FILL_BOTH);
		comp.setLayoutData(gd);
		setControl(comp);
		
		createInterpreterPathConfigComponent(comp);
		createProgramArgumentsConfigBlock(comp);
		fWorkingDirectoryBlock.createControl(comp);
		createInterpreterCommunictationConfigComponent(comp);
	}

	/**
	 * Creates the component set for launching interpreter
	 * @param parent the parent to add this component to
	 */
	private void createInterpreterPathConfigComponent(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setLayout(new GridLayout(5, false));
		group.setText("Interpreter path");
		group.setFont(parent.getFont());
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		group.setLayoutData(gridData);

		Composite comp = new Composite(group, SWT.NONE);
		GridLayout layout = new GridLayout(5, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		comp.setLayout(layout);
		comp.setFont(parent.getFont());
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = 2;
		comp.setLayoutData(gridData);

		fLaunchButton = createCheckButton(comp, "Launch Interpreter");
		fLaunchButton.setLayoutData(new GridData (SWT.BEGINNING, SWT.NORMAL, false, false));
		fLaunchButton.addSelectionListener(fListener);

		fInterpreterText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		fInterpreterText.setFont(parent.getFont());
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 4;
		fInterpreterText.setLayoutData(gd);
		fInterpreterText.addModifyListener(fListener);
		
		Composite bcomp = new Composite(comp, SWT.NONE);
		GridLayout ld = new GridLayout(1, false);
		bcomp.setLayout(ld);
		bcomp.setFont(parent.getFont());
		gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gd.horizontalSpan = 5;
		bcomp.setLayoutData(gd);
		ld.marginHeight = 1;
		ld.marginWidth = 0;
		fFileBrowse = createPushButton(bcomp, "Browse...", null);
		fFileBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String filePath = fInterpreterText.getText();
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
				filePath = dialog.open();
				if (filePath != null) {
					fInterpreterText.setText(filePath);
				}
			}
		});
	}

	private WidgetListener fListener = new WidgetListener();
	/**
	 * A listener to update for text changes and widget selection
	 */
	private class WidgetListener extends SelectionAdapter implements ModifyListener {
		public void modifyText(ModifyEvent e) {
			if (e.getSource() == fInterpreterText) {
//				fInterpreterText.setData(true);
			}
			updateLaunchConfigurationDialog();
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			Object source = e.getSource();
			if (source == fLaunchButton) {
				handleLaunchButtonSelected();
				updateLaunchConfigurationDialog();
			} else if (source == fConnectButton) {
				handleConnectButtonSelected();
				updateLaunchConfigurationDialog();
			} else if (source == fRideButton) {
				updateLaunchConfigurationDialog();
			}
		}
	}
	
	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		boolean result = super.isValid(launchConfig);
		
		if (result) {
			setErrorMessage(null);
			setMessage(null);
			if (fLaunchButton.getSelection()) {
				File file = new File(fInterpreterText.getText());
				if (!file.exists()) {
					setMessage("Launch path isn't the file.");
					result = true;
				} else if (!file.isFile()) {
					setErrorMessage("Specified path is not a file");
					result = false;
				}
			} else if (!fLaunchButton.getSelection() && !fConnectButton.getSelection()) {
				setErrorMessage("Select launch interpreter or connect to server");
				result = false;
			}
			if (result) {
				result = fWorkingDirectoryBlock.isValid(launchConfig);
			}
		}
		return result;
	}
	
	private void handleLaunchButtonSelected() {
		boolean launch = fLaunchButton.getSelection();
			fInterpreterText.setEnabled(launch);
			fFileBrowse.setEnabled(launch);
	}
	
	private void handleConnectButtonSelected() {
		boolean connect = fConnectButton.getSelection();
		fHostText.setEnabled(connect);
		fPortText.setEnabled(connect);
	}
	
	private void createProgramArgumentsConfigBlock(Composite comp) {

		Font font = comp.getFont();
		Group group = new Group(comp, SWT.NONE);
		group.setFont(font);
		GridLayout layout = new GridLayout();
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		group.setText("Program &arguments");
		
		fPrgmArgumentsText = new Text(group, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		fPrgmArgumentsText.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				switch (e.detail) {
				case SWT.TRAVERSE_ESCAPE:
				case SWT.TRAVERSE_PAGE_NEXT:
				case SWT.TRAVERSE_PAGE_PREVIOUS:
					e.doit = true;
					break;
				case SWT.TRAVERSE_RETURN:
				case SWT.TRAVERSE_TAB_NEXT:
				case SWT.TRAVERSE_TAB_PREVIOUS:
					if ((fPrgmArgumentsText.getStyle() & SWT.SINGLE) != 0) {
						e.doit = true;
					} else {
						if (!fPrgmArgumentsText.isEnabled() || (e.stateMask & SWT.MODIFIER_MASK) != 0) {
							e.doit = true;
						}
					}
					break;
				}
			}
		});
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 40;
		gd.widthHint = 100;
		fPrgmArgumentsText.setLayoutData(gd);
		fPrgmArgumentsText.setFont(font);
		fPrgmArgumentsText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				scheduleUpdateJob();
			}
			
		});
		
		Button pgrmArgVariableButton = createPushButton(group, "Var&iables", null);
		pgrmArgVariableButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		pgrmArgVariableButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
				dialog.open();
				String variable = dialog.getVariableExpression();
				if (variable != null) {
					fPrgmArgumentsText.insert(variable);
				}
			}
		});
	}
	
	private void createInterpreterCommunictationConfigComponent(Composite parent) {

		Group group = new Group(parent, SWT.NONE);
		group.setLayout(new GridLayout(5, false));
		group.setText("Interpreter communication");
		group.setFont(parent.getFont());
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		group.setLayoutData(gridData);

		Composite comp = new Composite(group, SWT.NONE);
		GridLayout layout = new GridLayout(7, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		comp.setLayout(layout);
		comp.setFont(parent.getFont());
		gridData = new GridData(GridData.FILL_BOTH);
//		gridData2.horizontalSpan = 1;
		comp.setLayoutData(gridData);
		
		fConnectButton = createCheckButton(comp, "Connect to Server: ");
		fConnectButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.NORMAL, false, false));
		fConnectButton.addSelectionListener(fListener);
		
		Label label = new Label(comp, SWT.NONE);
		label.setText("Host:");
		label.setLayoutData(new GridData());
		
		fHostText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		fHostText.setToolTipText("Server host");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
//		gridData.widthHint = 150;
		fHostText.setLayoutData(gridData);
		fHostText.addModifyListener(fListener);
		
		Label portLabel = new Label (comp, SWT.NONE);
		portLabel.setText("Port:");
		portLabel.setLayoutData(new GridData());
		
		fPortText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		fPortText.setToolTipText("Communication port");
		gridData = new GridData();
//		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
		gridData.widthHint = 50;
		fPortText.setLayoutData(gridData);
		fPortText.addModifyListener(fListener);
		fPortText.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent event) {
				event.doit = event.text.length() == 0 || Character.isDigit(event.text.charAt(0));
			}
		});
		
		Composite rcomp = new Composite(group, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		rcomp.setLayout(gl);
		rcomp.setFont(parent.getFont());
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gd.horizontalSpan = 5;
		rcomp.setLayoutData(gd);
		gl.marginHeight = 1;
		gl.marginWidth = 0;
		fRideButton = createCheckButton(rcomp, "RIDE console");
//		fRideButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.NORMAL, false, false));
		fRideButton.addSelectionListener(fListener);
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(APLDebugCorePlugin.ATTR_PROGRAM_ARGUMENTS, (String) null);
		fWorkingDirectoryBlock.setDefaults(config);
	}

	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			fPrgmArgumentsText.setText(configuration.getAttribute(APLDebugCorePlugin.ATTR_PROGRAM_ARGUMENTS, ""));
			
			fWorkingDirectoryBlock.initializeFrom(configuration);
			
			fLaunchButton.setSelection(configuration.getAttribute(APLDebugCorePlugin.ATTR_LAUNCH_INTERPRETER, true));
			String defaultPath;
			String os = Platform.getOS();
			switch (os) {
				case Constants.OS_WIN32:
					defaultPath = APLDebugCorePlugin.ATTR_DEFAULT_INTERPRETER_PATH_WIN;
					break;
				case Constants.OS_LINUX:
					defaultPath = APLDebugCorePlugin.ATTR_DEFAULT_INTERPRETER_PATH_LINUX;
					break;
				case Constants.OS_MACOSX:
					defaultPath = APLDebugCorePlugin.ATTR_DEFAULT_INTERPRETER_PATH_MACOSX;
					break;
				default:
					defaultPath = "dyalog";
			}
			fInterpreterText.setText(configuration.getAttribute(APLDebugCorePlugin.ATTR_INTERPRETER_PATH,
					defaultPath));
			fConnectButton.setSelection(configuration.getAttribute(APLDebugCorePlugin.ATTR_INTERPRETER_CONNECT, false));
			fHostText.setText(configuration.getAttribute(APLDebugCorePlugin.ATTR_INTERPRETER_HOST, "localhost"));
			fPortText.setText(configuration.getAttribute(APLDebugCorePlugin.ATTR_INTERPRETER_PORT, "4502"));
			handleLaunchButtonSelected();
			handleConnectButtonSelected();
			fRideButton.setSelection(configuration.getAttribute(APLDebugCorePlugin.ATTR_SHOW_RIDE, false));
		} catch (CoreException e) {
			setErrorMessage("Exception occurred reading configuration:"
					+ e.getStatus().getMessage());
			APLDebugCorePlugin.log(e);
		}
	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
	
		configuration.setAttribute(APLDebugCorePlugin.ATTR_LAUNCH_INTERPRETER, fLaunchButton.getSelection());
		configuration.setAttribute(APLDebugCorePlugin.ATTR_INTERPRETER_PATH, fInterpreterText.getText().trim());
		configuration.setAttribute(APLDebugCorePlugin.ATTR_INTERPRETER_CONNECT, fConnectButton.getSelection());
		configuration.setAttribute(APLDebugCorePlugin.ATTR_INTERPRETER_HOST, fHostText.getText().trim());
		configuration.setAttribute(APLDebugCorePlugin.ATTR_INTERPRETER_PORT, fPortText.getText().trim());
		configuration.setAttribute(APLDebugCorePlugin.ATTR_PROGRAM_ARGUMENTS, 
				getAttributeValueFrom(fPrgmArgumentsText));
		fWorkingDirectoryBlock.performApply(configuration);
		configuration.setAttribute(APLDebugCorePlugin.ATTR_SHOW_RIDE, fRideButton.getSelection());
	}

	/**
	 * Returns the string in the text widget, or <code>null</code> if empty.
	 */
	private String getAttributeValueFrom(Text text) {
		String content = text.getText().trim();
		if (content.length() > 0) {
			return content;
		}
		return null;
	}
	
	public String getName() {
		return "Interpreter";
	}

	@Override
	public Image getImage() {
		return APLDebugUIPlugin.getImageCache().get(APLDebugUIPlugin.MAIN_ICON);
	}
	
	@Override
	public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
		super.setLaunchConfigurationDialog(dialog);
		fWorkingDirectoryBlock.setLaunchConfigurationDialog(dialog);
	}
	
	@Override
	public String getErrorMessage() {
		String m = super.getErrorMessage();
		if (m == null) {
			return fWorkingDirectoryBlock.getErrorMessage();
		}
		return m;
	}
	
	@Override
	public String getMessage() {
		String m = super.getMessage();
		if (m == null) {
			return fWorkingDirectoryBlock.getMessage();
		}
		return m;
	}
	
	@Override
	public String getId() {
		return "com.dyalog.APLDev.debug.ui.InterpreterTab";
	}
	
	@Override
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		fWorkingDirectoryBlock.initializeFrom(workingCopy);
	}
	
}
