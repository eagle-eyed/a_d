package com.dyalog.apldev.debug.core.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.internal.console.IOConsolePartition;
import org.eclipse.ui.internal.console.IOConsolePartitioner;
import org.eclipse.ui.internal.console.IOConsoleViewer;
import org.osgi.framework.Bundle;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;
import com.dyalog.apldev.debug.core.model.APLDebugTarget;

public class SessionConsole {
	
	private APLDebugTarget fTarget;
	// console
	private IOConsole fConsoleSession;
	private IOConsole fConsoleRIDE;
    private IOConsoleOutputStream foutSession;
    private IOConsoleInputStream finSession;
    private PrintWriter fConsoleSessionWriter;
    private BufferedReader fConsoleSessionReader;
    private ConsoleSessionReadJob fConsoleSessionReadJob;
    private IOConsoleOutputStream foutRIDE;
    private IOConsoleInputStream finRIDE;
    private PrintWriter fConsoleRIDEWriter;
    private BufferedReader fConsoleRIDEReader;
    private ConsoleRIDEReadJob fConsoleRIDEReadJob;

	private boolean fConsolesOpened = false;
	private IOConsoleOutputStream fProcConsoleStdOut;
	private IOConsoleOutputStream fProcConsoleErrOut;
	private PrintWriter fConsoleProcWriter;
	
	private IOConsoleViewer fSessionViewer;
	private AplDevConsole fAplDevConsole;
	private IConsoleManager fConManager;
	private IDocumentListener fRIDEListener;
	private boolean fTerminated;

	/**
	 * @deprecated use instead isTerminated 
	 */
	public boolean isOpened() {
		return fConsolesOpened;
	}

	public SessionConsole(APLDebugTarget target) throws DebugException {
		this.fTarget = target;
		this.fTerminated = false;
		
		Date date = new Date();
		
		// Create console for RIDE protocol
		ImageDescriptor descR = getImageDesc("icons/cubeR.ico");
		String name = target.getName();
		fConsoleRIDE = createConsole("RIDE: " + name, descR);
		foutRIDE = fConsoleRIDE.newOutputStream();
		finRIDE = fConsoleRIDE.getInputStream();
		fConsoleRIDEWriter = new PrintWriter(new OutputStreamWriter(foutRIDE, StandardCharsets.UTF_8));

		fConsoleRIDEWriter.println("RIDE started: " + date);
		fConsoleRIDEWriter.flush();
		addRIDEConsoleInputListener();

		// Create session console
		AplDevConsoleInterpreter consoleInterpreter = new AplDevConsoleInterpreter(fTarget);
		fTarget.setConsoleInterpter(consoleInterpreter);
		ImageDescriptor desc = APLDebugCorePlugin.getImageDescriptor("icons/cube.ico");
		fAplDevConsole = new AplDevConsole("Session: " + name, desc, consoleInterpreter);
		fConManager = ConsolePlugin.getDefault().getConsoleManager();
		fConManager.addConsoles(new IConsole[] {fAplDevConsole});

		fConsolesOpened = true;
	}
	
	public void close() {
		// Stop Dispatch Job
		if (fConsoleSessionReadJob != null)
			fConsoleSessionReadJob.stop();
		if (fConsoleRIDEReadJob != null)
			fConsoleRIDEReadJob.stop();
		// console consoles
//		try {
		if (fConsolesOpened) {
			fConsolesOpened = false;
//			finSession.close();
//			foutSession.close();
//			
//			finRIDE.close();
//			foutRIDE.close();
//		    fConsoleSession.streamClosed(foutSession);

			fConManager.removeConsoles(new IConsole[]
					{fConsoleSession, fConsoleRIDE});
			fAplDevConsole.terminate();
			fConManager.removeConsoles(new IConsole[] {fAplDevConsole});
		}
//	} catch (IOException e) {
//		DebugCorePlugin.getDefault().getLog().log(
//	            new Status (IStatus.ERROR, DebugCorePlugin.PLUGIN_ID,
//	            		"Error closing consoles", e));
//	}

	}
	private IOConsole createConsole(String name, ImageDescriptor desc){
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
//		ImageDescriptor desc = getImageDesc("icons/favicon.ico");
		Charset enc = StandardCharsets.UTF_8;
		IOConsole console = new IOConsole(name, APLDebugCorePlugin.PLUGIN_ID , desc, enc.toString(), false);
		conMan.addConsoles(new IConsole[] {console});
		return console;
	}

	private ImageDescriptor getImageDesc(String path) {
		ImageDescriptor desc = ImageDescriptor.getMissingImageDescriptor();
//		Bundle bundle = Platform.getBundle(org.eclipse.debug.examples.core.pda.PLUGIN_ID);
		Bundle bundle = Platform.getBundle(APLDebugCorePlugin.PLUGIN_ID);
		URL url = null;
		if (bundle != null) {
			url = FileLocator.find(bundle, new Path(path), null);
			if (url != null) {
				desc = ImageDescriptor.createFromURL(url);
			}
		}
		return desc;
	}

	/**
	 * Stop console interaction 
	 */
	public void terminate() {
		if (fTerminated)
			return;
		removeRIDEConsoleInputListener();
		fTerminated = true;
	}
	
	/**
	 * Check if console alive
	 */
	public boolean isTerminated() {
		return fTerminated;
	}
	
	/**
	 * Remove RIDE console input listener
	 */
	private void removeRIDEConsoleInputListener() {
		if (fRIDEListener != null)
			fConsoleRIDE.getDocument()
				.removeDocumentListener(fRIDEListener);
	}
	
	/**
	 * Add RIDE console input listener
	 */
	@SuppressWarnings("restriction")
	private void addRIDEConsoleInputListener() {
		if (fRIDEListener == null) {
			fRIDEListener = new IDocumentListener() {

				@Override
				public void documentAboutToBeChanged(DocumentEvent event) {
					// check if present a new line symbol
					if (event.fText.indexOf('\r') != -1 || event.fText.indexOf('\n') != -1) {
						try {
							ITypedRegion partition = event.fDocument.getPartition(event.fOffset);
							if (partition instanceof IOConsolePartition) {
								IOConsolePartition p = (IOConsolePartition) partition;
								// check if that is user input
								if (p.getType().equals(IOConsolePartition.INPUT_PARTITION_TYPE)) {
									if (event.fText.length() <= 2) {
										final String inputFound = p.getString();
										fTarget.postRIDECommand(inputFound);
									}
								}
							}
						} catch (BadLocationException e) {
							APLDebugCorePlugin.log(IStatus.ERROR, "Error in RIDE console Listener.", e);
						}
						
					}
					
				}
	
				@Override
				public void documentChanged(DocumentEvent event) {
					// check if new line symbols present
					if (event.fText.indexOf('\r') != -1 || event.fText.indexOf('\n') != -1) {
						try {
							ITypedRegion partition;
							partition = event.fDocument.getPartition(event.fOffset);
							if (partition instanceof IOConsolePartition) {
								IOConsolePartition p = (IOConsolePartition) partition;
								if (p.getType().equals(IOConsolePartition.INPUT_PARTITION_TYPE)) {
									if (event.fText.length() > 2) {
										// user pasted something
										fTarget.postRIDECommand(event.fText);
									}
								}
							}
						} catch (BadLocationException e) {
							APLDebugCorePlugin.log(IStatus.ERROR, "Can't listen what paste to RIDE console", e);
						}
					}
					
				}
			};
		}
		fConsoleRIDE.getDocument().addDocumentListener(fRIDEListener);
	}

	/**
	 * Add RIDE console input listener
	 */
	@SuppressWarnings("restriction")
	public void addSessionConsoleInputListener() {
		fConsoleSession.getDocument().addDocumentListener(new IDocumentListener() {

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
				// check if present a new line symbol
				if (event.fText.indexOf('\r') != -1 || event.fText.indexOf('\n') != -1) {
					try {
						ITypedRegion partition = event.fDocument.getPartition(event.fOffset);
						if (partition instanceof IOConsolePartition) {
							IOConsolePartition p = (IOConsolePartition) partition;
							// check if that is user input
							if (p.getType().equals(IOConsolePartition.INPUT_PARTITION_TYPE)) {
								if (event.fText.length() <= 2) {
									final String inputFound = p.getString();
									if (inputFound.length() > 0)
										fTarget.postSessionPrompt(inputFound);
								}
							}
						}
					} catch (BadLocationException e) {
						APLDebugCorePlugin.log(IStatus.ERROR, "Error in RIDE console Listener.", e);
					}
					
				}
				
			}

			@Override
			public void documentChanged(DocumentEvent event) {
				// check if new line symbols present
				if (event.fText.indexOf('\r') != -1 || event.fText.indexOf('\n') != -1) {
					try {
						ITypedRegion partition;
						partition = event.fDocument.getPartition(event.fOffset);
						if (partition instanceof IOConsolePartition) {
							IOConsolePartition p = (IOConsolePartition) partition;
							if (p.getType().equals(IOConsolePartition.INPUT_PARTITION_TYPE)) {
								if (event.fText.length() > 2) {
									// user pasted something
//									fTarget.postSessionPrompt(event.fText);
								}
							}
						}
					} catch (BadLocationException e) {
						APLDebugCorePlugin.log(IStatus.ERROR, "Can't listen what paste to RIDE console", e);
					}
				}
				
			}
			
		});
	}
	
	/**
	 *  Add process console input listener	
	 */
	@SuppressWarnings("restriction")
	public void addProcessConsoleInputListener() {
		boolean debug = true;
		if (debug) return;
		IConsole console = DebugUITools.getConsole(fTarget.getProcess());
		if (console instanceof ProcessConsole) {
			final ProcessConsole c = (ProcessConsole) console;
	        fProcConsoleStdOut = c.getStream(IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM);
	        fProcConsoleErrOut = c.getStream(IDebugUIConstants.ID_STANDARD_ERROR_STREAM);
		    try {
				fConsoleProcWriter = new PrintWriter(new OutputStreamWriter(fProcConsoleStdOut,"UTF8"));
//				fConsoleProcReader = new BufferedReader(new InputStreamReader(fProcConsoleIn,"UTF8"));
			} catch (UnsupportedEncodingException e) {
				APLDebugCorePlugin.getDefault().getLog().log(
			            new Status (IStatus.ERROR, APLDebugCorePlugin.PLUGIN_ID,
			            		"RIDE console unsupported encoding", e));
			}

//			final List<IConsoleInputListener> participants = ExtensionHelper
//					.getParticipants(ExtensionHelper.APLDEV_DEBUG_CONSOLE_INPUT_LISTENER);
//			final APLDebugTarget target = this;
			c.getDocument().addDocumentListener(new IDocumentListener() {
				
				@Override
				public void documentAboutToBeChanged(DocumentEvent event) {
					// check if present a new line symbol
					if (event.fText.indexOf('\r') != -1 || event.fText.indexOf('\n') != -1) {
						try {
							ITypedRegion partition = event.fDocument.getPartition(event.fOffset);
							if (partition instanceof IOConsolePartition) {
								IOConsolePartition p = (IOConsolePartition) partition;
								
								// check if that is user input
								if (p.getType().equals(IOConsolePartition.INPUT_PARTITION_TYPE)) {
									if (event.fText.length() <= 2) {
										final String inputFound = p.getString();
										WriteRIDE("PROC_TYPE: " + inputFound);
//										for (IConsoleInputListener listener : partcipants) {
//											listener.newLineReceived(inputFound, target);
//										}
									}
								}
							}
						} catch (Exception e) {
							APLDebugCorePlugin.getDefault().getLog().log(
						            new Status (IStatus.ERROR, APLDebugCorePlugin.PLUGIN_ID,
						            		"Error listen input for process console", e));
						}
					}
				}
				
				@Override
				public void documentChanged(DocumentEvent event) {
					fSessionViewer.getTextWidget().setCaretOffset(Integer.MAX_VALUE);
					// only report when have a new line
					if (event.fText.indexOf('\r') != -1 || event.fText.indexOf('\n') != -1) {
						try {
							ITypedRegion partition = event.fDocument.getPartition(event.fOffset);
							if (partition instanceof IOConsolePartition) {
								IOConsolePartition p = (IOConsolePartition) partition;
								
								if (p.getType().equals(IOConsolePartition.INPUT_PARTITION_TYPE)) {
									if (event.fText.length() > 2) {
										// user pasted something
										WriteRIDE("PROC_PASTE: " + event.fText);
//										for (IOConsoleInputListener listener : participants) {
//											listener.pasteReceived(event.fText, target);
//										}
									}
								}
							}
						} catch (Exception e) {
							APLDebugCorePlugin.getDefault().getLog().log(
						            new Status (IStatus.ERROR, APLDebugCorePlugin.PLUGIN_ID,
						            		"Error listen input for process console", e));
						}
					}
				}
			});
		}
	}


	/**
	 * Listens to input steam form the Session console
	 */
	class ConsoleSessionReadJob extends Job {
		private boolean stop = false;

		public void stop() {
			this.stop = true;
		}

		public ConsoleSessionReadJob() {
			super("Session console Dispatch");
			setSystem(true);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			String message = "";
			while (!fTarget.isTerminated() && !stop) {
				try {
//					int c = finSession.read();
//					if (c != -1)
//						System.out.println(c);
//					int n = finSession.available();
					message = fConsoleSessionReader.readLine();
					fTarget.postSessionPrompt(message);
//					fConsoleRIDEWriter.println("S >> " + message);
//					fConsoleRIDEWriter.flush();
				} catch (IOException e) {
					APLDebugCorePlugin.getDefault().getLog().log(
				            new Status (IStatus.ERROR, APLDebugCorePlugin.PLUGIN_ID,
				            		"Error reading session input", e));
				        continue;
				}
			}
			return Status.OK_STATUS;
		}
		
	}
	
	public Job asJob() {
		Job job = new Job("JRuby REPL") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				while (!monitor.isCanceled()) {
					try {
						fConsoleSessionReader.readLine();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		return job;
	}
	
	/**
	 * Listens to input steam form the Session console
	 */
	class ConsoleRIDEReadJob extends Job {

		private boolean stop = false;

		public void stop() {
			this.stop = true;
		}

		public ConsoleRIDEReadJob() {
			super("RIDE console Dispatch");
			setSystem(true);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			String message = "";
			while (!fTarget.isTerminated() && !stop) {
				try {
//					int c = finRIDE.read();
//					if (c != -1)
//						System.out.println(c);
//					int n = finRIDE.available();
					message = fConsoleRIDEReader.readLine();
					fConsoleSessionWriter.println("R >> " + message);
					fConsoleSessionWriter.flush();
					fTarget.postCommand(message);
//					try {
//						fTarget.sendRequestNoReply(message);
//					} catch (DebugException e) {
//						DebugCorePlugin.log(IStatus.ERROR,
//								"Error sending message to Interpreter from RIDE console", e);
//					        continue;
//					}
				} catch (IOException e) {
					APLDebugCorePlugin.log(IStatus.ERROR,
				            		"Error reading session input", e);
				        continue;
				}
			}
			return Status.OK_STATUS;
		}
		
	}

	public void WriteRIDE(String message) {
		if (fConsoleRIDEWriter != null) {
			fConsoleRIDEWriter.println(message);
			fConsoleRIDEWriter.flush();
		}
	}
	
	@SuppressWarnings("restriction")
	public void WriteSession(String message) {
		boolean var = false;
		if (var) {
		IDocument doc = fConsoleSession.getDocument();
		IDocumentPartitioner partitioner = doc.getDocumentPartitioner();
		IOConsolePartitioner ioPart = (IOConsolePartitioner) partitioner;

//		try {
//			ioPart.streamAppended(foutSession, message);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		int offset = 0;
		int lines = doc.getNumberOfLines();
		try {
			offset = doc.getLineOffset(lines - 1);
		} catch (BadLocationException e1) {
			e1.printStackTrace();
		}
		final int offsetOut = offset-1;
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				try {
					doc.replace(offsetOut, 0, message);

					ConsolePlugin plugin = ConsolePlugin.getDefault();
					IConsoleManager conMan = plugin.getConsoleManager();

					conMan.warnOfContentChange(fConsoleSession);

				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		} else {
		foutSession.setEncoding(StandardCharsets.UTF_8.toString());
		try {
			foutSession.write(message);
			foutSession.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		fConsoleSessionWriter.println(message);
////		fConsoleSessionWriter.print("      ");
//		fConsoleSessionWriter.flush();

		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		conMan.warnOfContentChange(fConsoleSession);
		

		}
		
//		fConsoleSession.getDocument().set setCaretOffset
	}
	
	public void WriteProc(String message) {
		fConsoleProcWriter.println(message);
		fConsoleProcWriter.flush();
	}
	//
	
//	public void  setReadOnly() {
//	        ConsolePlugin.getStandardDisplay().asyncExec(new Runnable() {
//	            public void run() {
//	                StyledText text = getTextWidget();
//	                if (text != null && !text.isDisposed()) {
//	                   text.setEditable(false);
//	               }
//	           }
//	       });
//	   }

	/**
	 * Append receive content to RIDE console
	 */
	public void appendReceiveRide(String cmd, String sign) {
		Calendar now = Calendar.getInstance();
		int hour = now.get(Calendar.HOUR);
		int min = now.get(Calendar.MINUTE);
		int sec = now.get(Calendar.SECOND);
		int millis = now.get(Calendar.MILLISECOND);

		WriteRIDE(format(hour, 2) + ":"
				+ format(min, 2) + ":"
				+ format(sec, 2) + "."
				+ format(millis, 3)
				+ sign + "   "	+ cmd);
	}

	/**
	 * Append send content to RIDE console
	 */
	public void appendSendRide(String cmd, String sign) {
		Calendar now = Calendar.getInstance();
		int hour = now.get(Calendar.HOUR);
		int min = now.get(Calendar.MINUTE);
		int sec = now.get(Calendar.SECOND);
		int millis = now.get(Calendar.MILLISECOND);
		WriteRIDE(format(hour, 2) + ":"
		+ format(min, 2) + ":"
		+ format(sec, 2) + "."
		+ format(millis, 3)
		+ sign + " < " + cmd);
	}
	
	/**
	 * Append "0"'s to val 
	 */
	String format(int val, int width) {
		int len = width - (int)Math.floor(Math.log10(val)) - 1;
		if (len < 1)
			return "" + val;
		char[] c = new char[len];
		for (int i = 0; i < len; i++) {
			c[i] = '0';
		}
		return new String(c) + val;
	}

}

