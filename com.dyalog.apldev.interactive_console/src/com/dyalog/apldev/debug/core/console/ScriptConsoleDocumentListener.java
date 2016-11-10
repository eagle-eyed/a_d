package com.dyalog.apldev.debug.core.console;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import com.dyalog.apldev.debug.core.content.StringUtils;
import com.dyalog.apldev.debug.core.content.TextSelectionUtils;
import com.dyalog.apldev.debug.core.content.Tuple;
import com.dyalog.apldev.log.Log;

/**
 * Listen to the document and will:
 * 
 *  - pass the commands to the handler
 *  - add the results from the handler
 *  - show the prompt
 *  - set the color of the console regions
 */
public class ScriptConsoleDocumentListener implements IDocumentListener {

	private long lastChangeMillis;
	private int disconnectionLevel = 0;
	/**
	 * Document to which this listener is attached.
	 */
	private IDocument doc;
	private ICommandHandler handler;
	private IScriptConsoleViewer2ForDocumentListener viewer;
	private volatile boolean promptReady;
	private ScriptConsolePrompt prompt;
	private int readOnlyColumnsInCurrentBeforePrompt;
	/**
	 * Additional viewers for the same document
	 */
	private List<WeakReference<IScriptConsoleViewer2ForDocumentListener>> otherViewers
			= new ArrayList<WeakReference<IScriptConsoleViewer2ForDocumentListener>>();
	/**
	 * Console line trackers (for hyperlinking)
	 */
	private List<IConsoleLineTracker> consoleLineTrackers;
	/**
	 * Strategy used for indenting / tabs
	 */
	private IHandleScriptAutoEditStrategy strategy;
	private ScriptConsoleHistory history;
	private int historyFullLine;
	/**
	 * The commands that should be initially set in the console
	 */
	private String initialCommands;
	private boolean fExecuting = false;
	/**
	 * Interpreter response when prompt type changed
	 */
	final ICallback<Object, InterpreterResponse> fOnResponseReceived;

	private void setPromptReady(boolean state) {
		promptReady = state;
	}

	private void setExecuting(boolean state) {
		fExecuting = state;
	}
	
	/**
	 * Constructor
	 * 
	 * @param viewer this is the viewer to which this listener is attached.
	 * It's the main viewer. Other viewers may be added later through addViewer()
	 * for sharing the same listener and being properly updated.
	 * @param handler this is the object that'll handle the commands
	 * @param prompt shows the prompt to the user
	 * @param history keeps track of the commands added by the user.
	 * @param initialCommands the commands that should be initially added
	 */
	public ScriptConsoleDocumentListener(
			IScriptConsoleViewer2ForDocumentListener viewer,
			ICommandHandler handler,
			ScriptConsolePrompt prompt,
			ScriptConsoleHistory history,
			List<IConsoleLineTracker> consoleLineTrackers,
			String initialCommands,
			IHandleScriptAutoEditStrategy strategy) {

		this.lastChangeMillis = System.currentTimeMillis();
		this.strategy = strategy;
		this.prompt = prompt;
		this.handler = handler;
		this.history = history;
		this.viewer = viewer;
		this.readOnlyColumnsInCurrentBeforePrompt = 0;
		this.initialCommands = initialCommands;
		
		final ICallback<Object, Tuple<String, String>> onContentsReceived
				= new ICallback<Object, Tuple<String, String>>() {

			@Override
			public Object call(final Tuple<String, String> result) {
				if (result.o1.length() > 0 || result.o2.length() > 0) {
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							startDisconnected();
							PromptContext pc;
							try {
								pc = removeUserInput();
								IScriptConsoleSession consoleSession = viewer.getConsoleSession();
								if (result.o1.length() > 0) {
									if (consoleSession != null) {
										consoleSession.onStdoutContentsReceived(result.o1);
									}
									addToConsoleView(result.o1, true, true);
								}
								if (result.o2.length() > 0) {
									if (consoleSession != null) {
										consoleSession.onStderrContentsReceived(result.o2);
									}
									addToConsoleView(result.o2, false, true);
								}
								if (pc.removedPrompt) {
									appendInvitation(false);
									if (pc.userInput.length() > 0) {
										applyStyleToUserAddedText(pc.userInput, -1);
										appendText(pc.userInput);
										ScriptConsoleDocumentListener.this.viewer.setCaretOffset(
												doc.getLength() - pc.cursorOffset, false);
									}
								}
							} finally {
								stopDisconnected();
							}
								
//							if (pc.removedPrompt) {
//								appendText(pc.userInput);
//								ScriptConsoleDocumentListener.this.viewer.setCaretOffset(
//										doc.getLength() - pc.cursorOffset, true);
//							}
						}
					};
					RunInUiThread.async(runnable);
				}
				return null;
			}
		};
		
		handler.setOnContentsReceivedCallback(onContentsReceived);
		/**
		 * Output text to command prompt from interpreter
		 */
		final ICallback< Object, String > onEchoInput = new ICallback < Object, String >() {
			@Override
			public Object call(final String text) {
				if (fExecuting)
					return null;
				handler.setOnResponseReceived(fOnResponseReceived);

				if (text.length() > 0) {
					int initialOffset = doc.getLength();
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							if (disconnectionLevel == 0)
								startDisconnected();
							try {
//								text.replaceAll("\n", getDelimeter());
								String cmd;
								if (text.startsWith("      "))
									cmd = text.substring(6);
								else
									cmd = text;
								cmd.replace("\n", getDelimeter());
								applyStyleToUserAddedText(cmd, initialOffset);
								appendText(cmd);
								setPromptReady(false);
								setExecuting(true);
								// add to history
								history.update(cmd.replace(getDelimeter(), ""));
							} finally {
								stopDisconnected();
							}
							
						}

					};
					RunInUiThread.async(runnable);
				}
				return null;
			}
		};
		handler.setOnEchoInput(onEchoInput);
		
		fOnResponseReceived = new ICallback<Object, InterpreterResponse>() {
			
			@Override
			public Object call(final InterpreterResponse arg) {
				if ( arg.prompt == 0) {
				} else if (arg.prompt == 1 && promptReady == false) {
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							startDisconnected();
							try {
								processResult(arg);
//								promptReady = true;
							} finally {
								// restore listener
//								disconnectionLevel = 1;
								stopDisconnected();
							}
						}
					};
					RunInUiThread.async(runnable);
				}
				return null;
			}
		};
		handler.setOnResponseReceived(fOnResponseReceived);
	}

	private class PromptContext {
		public boolean removedPrompt;
		// offset from the end of the document.
		public int cursorOffset;
		public String userInput;
		
		public PromptContext(boolean removedPrompt, int cursorOffset, String userInput) {
			this.removedPrompt = removedPrompt;
			this.cursorOffset = cursorOffset;
			this.userInput = userInput;
		}
	}
	
	protected PromptContext removeUserInput() {
		if (!promptReady) {
			return new PromptContext(false, -1, "");
		}
		
		PromptContext pc = new PromptContext(true, -1, "");
		try {
			int lastLine = doc.getNumberOfLines() - 1;
			int lastLineLength = doc.getLineLength(lastLine);
			int end = doc.getLength();
			int start = end - lastLineLength;
			// There may be read-only content before the current input. So last
			// line may look like:
			// Out[10]: >>> some_user_command
			// The content before the prompt should be treated as read-only.
			int promptOffset = doc.get(start, lastLineLength).indexOf(prompt.toString());
			start += promptOffset;
			lastLineLength -= promptOffset;
			
			pc.userInput = doc.get(start, lastLineLength);
			pc.cursorOffset = end - viewer.getCaretOffset();
			doc.replace(start,  lastLineLength, "");
			
			pc.userInput = pc.userInput.replace(prompt.toString(), "");
			
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		return pc;
	}

	/**
	 * Shows the prompt for the user (e.g.: >>>)
	 */
	private void appendInvitation(boolean async) {
		int start = doc.getLength();
		String promptStr = prompt.toString();
		IConsoleStyleProvider styleProvider = viewer.getStyleProvider();
		if (styleProvider != null) {
			ScriptStyleRange style = styleProvider.createPromptStyle(promptStr, start);
			if (style != null) {
				addToPartitioner(style);
			}
		}
		appendText(promptStr);
		setCaretOffset(doc.getLength(), async);
		revealEndOfDocument();
		setPromptReady(true);
	}

	private void setCaretOffset(int offset) {
		setCaretOffset(offset, false);
	}
	
	/**
	 * Sets the caret offset for the main viewer and all the related viewer for the
	 * same document
	 */
	private void setCaretOffset(int offset, boolean async) {
		viewer.setCaretOffset(offset, async);
		for (Iterator<WeakReference<IScriptConsoleViewer2ForDocumentListener>>
				it = otherViewers.iterator(); it.hasNext();) {
			WeakReference<IScriptConsoleViewer2ForDocumentListener> ref = it.next();
			IScriptConsoleViewer2ForDocumentListener v = ref.get();
			if (v == null) {
				it.remove();
			} else {
				v.setCaretOffset(offset, async);
			}
		}
	}
	
	/**
	 * Adds some text that came as an output to stdout or stderr to the console.
	 * 
	 * @param out the text that should be added
	 * @param stdout true if it came from stdout and also if it came from stderr
	 */
	private void addToConsoleView(String out, boolean stdout, boolean textAddedIsReadOnly) {
		if (out.length() == 0) {
			return;
		}
		int start = doc.getLength();
		
		IConsoleStyleProvider styleProvider = viewer.getStyleProvider();
		Tuple<List<ScriptStyleRange>, String> style = null;
		if (styleProvider != null) {
			if (stdout) {
				style = styleProvider.createInterpreterOutputStyle(out, start);
			} else {
				style = styleProvider.createInterpreterErrorStyle(out, start);
			}
			if (style != null) {
				for (ScriptStyleRange s : style.o1) {
					addToPartitioner(s);
				}
			}
		}
		if (style != null) {
			appendText(style.o2);
			if (textAddedIsReadOnly) {
				try {
					// The text we just appended can't be changed!
					int lastLine = doc.getNumberOfLines() - 1;
					int len = doc.getLineLength(lastLine);
					this.readOnlyColumnsInCurrentBeforePrompt = len;
				} catch (BadLocationException e) {
					Log.log(e);
				}
			}
		}
		
		TextSelectionUtils ps = new TextSelectionUtils(doc, start);
		int cursorLine = ps.getCursorLine();
		int numberOfLines = doc.getNumberOfLines();
		
		// right after appending the text, let's notify line trackers
		for (int i = cursorLine; i < numberOfLines; i++) {
			try {
				int offset = ps.getLineOffset(i);
				int endOffset = ps.getEndLineOffset(i);
				
				Region region = new Region(offset, endOffset - offset);
				
				if (consoleLineTrackers != null)
					for (IConsoleLineTracker lineTracker : this.consoleLineTrackers) {
						lineTracker.lineAppended(region);
					}
			} catch (Exception e) {
				Log.log(e);
			}
		}
		revealEndOfDocument();
	}
	
	/**
	 * Adds a given style range to the partitioner (style must be added before the actual text)
	 */
	private void addToPartitioner(ScriptStyleRange style) {
		IDocumentPartitioner partitioner = this.doc.getDocumentPartitioner();
		if (partitioner instanceof ScriptConsolePartitioner) {
			ScriptConsolePartitioner scriptConsolePartitioner 
				= (ScriptConsolePartitioner) partitioner;
			scriptConsolePartitioner.addRange(style);
		}
	}
	
	/**
	 * Shows the end of the document for the main viewer and all the related viewer for the same document.
	 */
	private void revealEndOfDocument() {
		viewer.revealEndOfDocument();
		for (Iterator<WeakReference<IScriptConsoleViewer2ForDocumentListener>> it 
				= otherViewers.iterator(); it.hasNext();) {
			WeakReference<IScriptConsoleViewer2ForDocumentListener> ref = it.next();
			IScriptConsoleViewer2ForDocumentListener v = ref.get();
			if (v == null) {
				it.remove();
			} else {
				v.revealEndOfDocument();
			}
		}
	}
	
	/**
	 * Appends some text at the end of the document
	 */
	protected void appendText(String text) {
		int initialOffset = doc.getLength();
		try {
			doc.replace(initialOffset, 0, text);
		} catch (BadLocationException e) {
			Log.log(e);
		}
	}
	
	
	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {

	}

	@Override
	public void documentChanged(DocumentEvent event) {
		lastChangeMillis = System.currentTimeMillis();
		startDisconnected();
		try {
			int eventOffset = event.getOffset();
			String eventText = event.getText();
			processAddition(eventOffset, eventText);
		} finally {
			stopDisconnected();
		}
	}

	/**
	 * @return the last time the document that this console was changed
	 */
	public long getLastChangeMillis() {
		return lastChangeMillis;
	}
	
	/**
	 * Should be called right after adding some text to the console
	 * (it'll actually go on, remove the text just added and add it
	 * line-by-line in the document so that it can be correctly treated
	 * in the console).
	 * 
	 * @param offset the offset where the addition took place
	 * @param text the text that should be added
	 */
	protected void processAddition(int offset, String text) {
		// add line-by-line the contents in case when it have been copy/paste with multi-lines
		String indentString = "";
		boolean addedNewLine = false;
		boolean addedParen = false;
		boolean addedCloseParen = false;
		int addedLen = text.length();
		if (addedLen == 1) {
			if (text.equals("\r") || text.equals("\n")) {
				addedNewLine = true;
			}
			else if (text.equals("(")) {
				addedParen = true;
			}
			else if (text.equals(")")) {
				addedCloseParen = true;
			}
		} else if (addedLen == 2) {
			if (text.equals("\r\n")) {
				addedNewLine = true;
			}
		}
		
		String delim = getDelimeter();
		
		int newDeltaCaretPosition = doc.getLength() - (offset + text.length());
		// remove the text which user just entered (and enter it line-by-line later)
		try {
			// Remove the just entered text
			doc.replace(offset, text.length(), "");
			// Is the current offset, in the command line NB we do this after
			// the above as the pasted text may have new lines in it
			boolean offset_in_command_line = offset >= getCommandLineOffset();
			
			// if the offset isn't in the command line, then just append to the
			// existing command line text
			if (!offset_in_command_line) {
				offset = newDeltaCaretPosition = getCommandLineOffset();
				// Remove any existing command line text and prepend it to the text
				// we're inserting
				text = doc.get(getCommandLineOffset(), getCommandLineLength()) + text;
				doc.replace(getCommandLineOffset(),  getCommandLineLength(), "");
			} else {
				// paste is within the command line
				text = text + doc.get(offset, doc.getLength() - offset);
				doc.replace(offset, doc.getLength() - offset, "");
			}
		} catch (BadLocationException e) {
			text = "";
			Log.log(e);
		}
		
		text = StringUtils.replaceNewLines(text, delim);
		
		// add line-by-line
		int start = 0;
		int index = -1;
		List<String> commands = new ArrayList<String>();
		while ((index = text.indexOf(delim, start)) != -1) {
			String cmd = text.substring(start, index);
			cmd = convertTabs(cmd);
			commands.add(cmd);
			start = index + delim.length();
		}
		
		final String[] finalIndentString = new String[] { indentString };
		
		if (commands.size() > 0) {
			// disconnect from the document listener and reconnect after last line is executed
			startDisconnected();
			String cmd = commands.get(0);
			execCommand(addedNewLine, delim, finalIndentString, cmd, commands,
					0, text, addedParen, start, addedCloseParen, newDeltaCaretPosition);
		} else {
			onAfterAllLinesHandled(text, addedParen, start, offset, addedCloseParen,
					finalIndentString[0], newDeltaCaretPosition);
		}
	}

	/**
	 * @return the delimiter to be used to add new lines to the console
	 */
	public String getDelimeter() {
		return TextUtilities.getDefaultLineDelimiter(doc);
	}
	
	public int getCommandLineOffset() throws BadLocationException {
		int lastLine = doc.getNumberOfLines() - 1;
		int commandLineOffset = doc.getLineOffset(lastLine) + getLastLineReadOnlySize();
		if (commandLineOffset > doc.getLength()) {
			return doc.getLength();
		}
		return commandLineOffset;
	}
	
	/**
	 * @return the length of the current command line (all the currently
	 * editable area)
	 * 
	 * @throws BadLocationException
	 */
	public int getCommandLineLength() throws BadLocationException {
		int lastLine = doc.getNumberOfLines() - 1;
		int len = doc.getLineLength(lastLine) - getLastLineReadOnlySize();
		if (len <= 0)
			return 0;

		return len;
	}
	
	private String convertTabs(String cmd) {
		return strategy.convertTabs(cmd);
	}
	
	/**
	 * Stop listening to changes (so that we're able to change
	 * the document in this class without having any loops back
	 * into the function that will change it)
	 */
	protected synchronized void startDisconnected() {
		if (disconnectionLevel == 0) {
			doc.removeDocumentListener(this);
		}
		disconnectionLevel += 1;
	}
	
	/**
	 * Start listening to changes again.
	 */
	protected synchronized void stopDisconnected() {
		disconnectionLevel -= 1;
		if (disconnectionLevel < 0)
			disconnectionLevel = 0;
		if (disconnectionLevel == 0)
			doc.addDocumentListener(this);
	}

	/**
	 * This method should be called after all the lines received were processed
	 */
	private void onAfterAllLinesHandled(final String finalText, final boolean finalAddedParen,
			final int finalStart, final int finalOffset, final boolean finalAddedCloseParen,
			final String finalIndentString, final int finalNewDeltaCaretPosition) {
		
		boolean shiftsCaret = true;
		String newText = finalText.substring(finalStart, finalText.length());
		if (finalAddedParen) {
			String cmdLine = getCommandLine();
			Document parenDoc = new Document(cmdLine + newText);
			int currentOffset = cmdLine.length() + 1;
			DocCmd docCmd = new DocCmd(currentOffset, 0, "(");
			docCmd.shiftsCaret = true;
			try {
				strategy.customizeParenthesis(parenDoc, docCmd);
			} catch (BadLocationException e) {
				Log.log(e);
			}
			newText = docCmd.text + newText.substring(1);
			if (!docCmd.shiftsCaret) {
				shiftsCaret = false;
				setCaretOffset(finalOffset + (docCmd.caretOffset - currentOffset));
			}
		} else if (finalAddedCloseParen) {
			String cmdLine = getCommandLine();
			String existingDoc = cmdLine + finalText.substring(1);
			int cmdLineOffset = cmdLine.length();
			if (existingDoc.length() > cmdLineOffset) {
				Document parenDoc = new Document(existingDoc);
				DocCmd docCmd = new DocCmd(cmdLineOffset, 0, ")");
				docCmd.shiftsCaret = true;
				boolean canSkipOpenParenthesis;
				try {
					canSkipOpenParenthesis = strategy.canSkipCloseParenthesis(parenDoc, docCmd);
				} catch (BadLocationException e) {
					canSkipOpenParenthesis = false;
					Log.log(e);
				}
				if (canSkipOpenParenthesis) {
					shiftsCaret = false;
					setCaretOffset(finalOffset + 1);
					newText = newText.substring(1);
				}
			}
		}
		
		// and now add the last line (without actually handling it)
		String cmd = finalIndentString + newText;
		cmd = convertTabs(cmd);
		if (cmd == null)
			return;
		applyStyleToUserAddedText(cmd, doc.getLength());
		appendText(cmd);
		if (shiftsCaret) {
			setCaretOffset(doc.getLength() - finalNewDeltaCaretPosition);
		}
		
		history.update(getCommandLine());
	}
	
	/**
	 * @return the command line that the user entered
	 * 
	 * @throws BadLocationException
	 */
	public String getCommandLine() {
		int commandLineOffset;
		int commandLineLength;
		try {
			commandLineOffset = getCommandLineOffset();
			commandLineLength = getCommandLineLength();
		} catch (BadLocationException e1) {
			Log.log(e1);
			return "";
		}
		if (commandLineLength < 0) {
			return "";
		}
		
		try {
			return doc.get(commandLineOffset, commandLineLength);
		} catch (BadLocationException e) {
//			String msg = new FastStringBuffer(60).append("Error:bad location: offset:")
//					.append(commandLineOffset).append(" text:").append(commLineLength).toString();
			Log.log(e);
			return "";
		}
	}
	
	/**
	 * Applies the style in the text for the contents that've been just added
	 * @param cmd for computing region length
	 * @param offset for -1 means end of the document 
	 */
	private void applyStyleToUserAddedText(String cmd, int offset) {
		IConsoleStyleProvider styleProvider = viewer.getStyleProvider();
		if (styleProvider != null) {
			if (offset == -1)
				offset = doc.getLength();
			ScriptStyleRange style = styleProvider.createUserInputStyle(cmd, offset);
			if (style != null) {
				addToPartitioner(style);
			}
		}
	}
	
	/**
	 * Here is where we run things not using the UI thread. It's a recursive
	 * function. In summary, it'll run each line in the commands received in
	 * a new thread, and as each finishes, it calls itself again for the next
	 * command. The last command will reconnect to the document.
	 */
	private void execCommand(final boolean addedNewLine, final String delim,
			final String[] finalIndentString, final String cmd, final List<String> commands,
			final int currentCommand, final String text, final boolean addedParen,
			final int start, final boolean addedCloseParen, final int newDeltaCaretPosition) {

		applyStyleToUserAddedText(cmd, doc.getLength());
		// the cmd could be something as "\r\n"
		appendText(cmd);
		
		final String commandLine = getCommandLine();
//		try {
//			doc.replace(doc.getLength()-commandLine.length(), commandLine.length(), "");
//		} catch (BadLocationException e) {
//			DebugCorePlugin.log(e);
//		}
		history.update(commandLine);
		// handle the command line: send contents of the current line to the 
		// interpreter and handle results
		
		appendText(getDelimeter());

		final boolean finalAddedNewLine = addedNewLine;
		final String finalDelim = delim;
		
		final ICallback<Object, InterpreterResponse> onResponseReceived = new ICallback<Object, InterpreterResponse>() {
			
			@Override
			public Object call(final InterpreterResponse arg) {
				if (arg.prompt == 0) {
//					promptReady = false;
					return null;
				}

				if (!(arg.prompt == 1 && fExecuting == true))
					return null;
				// when we receive the response, we must handle it in the UI thread.
				Runnable runnable = new Runnable() {
					
					@Override
					public void run() {
//						startDisconnected();
						try {
							processResult(arg);
							if (finalAddedNewLine) {
								List<String> historyList = history.getAsList();
								IDocument historyDoc = new Document(StringUtils.join("\n",
										historyList.subList(historyFullLine, historyList.size())) + "\n");
								int currHistoryLen = historyDoc.getLength();
								if (currHistoryLen > 0) {
									DocCmd docCmd = new DocCmd(currHistoryLen - 1, 0, finalDelim);
									strategy.customizeNewLine(historyDoc, docCmd);
									// remove any new line added
									finalIndentString[0] = docCmd.text.replaceAll("\\r\\n|\\n|\\r", "");
									if (currHistoryLen != historyDoc.getLength()) {
										Log.log(IStatus.ERROR, "Error: the document passed to the customize NewLine should not be changed!", null);
									}
								}
							}
						} catch (Throwable e) {
							Log.log(e);
						}
						if (currentCommand + 1 < commands.size()) {
							execCommand(finalAddedNewLine, finalDelim, finalIndentString,
									commands.get(currentCommand + 1), commands, currentCommand + 1,
									text, addedParen, start, addedCloseParen, newDeltaCaretPosition);
						} else {
							// last one
							try {
								onAfterAllLinesHandled(text, addedParen, start, readOnlyColumnsInCurrentBeforePrompt,
										addedCloseParen, finalIndentString[0], newDeltaCaretPosition);
							} finally {
								// reconnect with the document
								setExecuting(false);
								stopDisconnected();
							}
						}
					}
				};
				RunInUiThread.async(runnable);
				return null;
			}
		};
		
		setExecuting(true);
		handler.beforeHandleCommand(commandLine, onResponseReceived);
		
		// Handle the command in a thread that doesn't block the U/I.
		Job j = new Job("APLDev Console Handler") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				setPromptReady(false);
				// send command to interpreter
				handler.handleCommand("      " + commandLine, onResponseReceived);
				return Status.OK_STATUS;
			}
		};
		j.setSystem(true);
		j.schedule();
	}
	
	/**
	 * Process the result from interpreter after sending command from session console
	 */
	private void processResult(final InterpreterResponse result) {
		if (result != null) {
			history.commit();
			try {
				readOnlyColumnsInCurrentBeforePrompt = getLastLineLength();
			} catch (BadLocationException e) {
				Log.log(e);
			}
			if (!result.more) {
				historyFullLine = history.getAsList().size();
			}
		}
		if (fExecuting) {
			appendInvitation(false);
			setExecuting(false);
		}
	}
	
	/**
	 * @return the length of the last line
	 */
	public int getLastLineLength() throws BadLocationException {
		int lastLine = doc.getNumberOfLines() - 1;
		return doc.getLineLength(lastLine);
	}

	/**
	 * Set the document that this class should listen
	 */
	public void setDocument(IDocument document) {
		reconnect(this.doc, document);
	}
	
	/**
	 * Sets the current command line
	 * Can be used by the up/down arrow to set a previous/next command
	 */
	public void setCommandLine(String text) throws BadLocationException {
		doc.replace(getCommandLineOffset(), getCommandLineLength(), text);
	}

	/**
	 * Stops listening changes in one document and starts listening another one
	 */
	private void reconnect(IDocument oldDoc, IDocument newDoc) {
		Assert.isTrue(disconnectionLevel == 0);
		
		if (oldDoc != null)
			oldDoc.removeDocumentListener(this);
		
		newDoc.addDocumentListener(this);
		this.doc = newDoc;
	}

	/**
	 * Adds some other viewer for the same document
	 */
	public void addViewer(IScriptConsoleViewer2ForDocumentListener scriptConsoleViewer) {
		this.otherViewers.add(new WeakReference
				<IScriptConsoleViewer2ForDocumentListener>(scriptConsoleViewer));
	}

	/**
	 * Clear the document and show the initial prompt
	 * 
	 * @param addInitialCommands indicates if the initial commands should be
	 * added to the document
	 */
	public void clear(boolean addInitialCommands) {
		startDisconnected();
		try {
			doc.set("");
			appendInvitation(true); // non in UI thread
		} finally {
			stopDisconnected();
		}
		
		if (addInitialCommands && initialCommands != null) {
			try {
				doc.replace(doc.getLength(), 0, this.initialCommands + "\n");
			} catch (BadLocationException e) {
				Log.log(e);
			}
		}
	}

	public void discardCommandLine() {
		if (!prompt.getNeedInput()) {
			final String commandLine = getCommandLine();
			if (!commandLine.isEmpty()) {
				history.commit();
			} else if (!prompt.getNeedMore()) {
				return;
			}
		}
		startDisconnected();
		try {
			try {
				doc.replace(doc.getLength(), 0, "\n");
			} catch (BadLocationException e) {
				Log.log(e);
			}
			readOnlyColumnsInCurrentBeforePrompt = 0;
			prompt.setMode(true);
			prompt.setNeedInput(false);
			appendInvitation(false);
			viewer.setCaretOffset(doc.getLength(), false);
		} finally {
			stopDisconnected();
		}
	}

	public int getLastLineOffset() throws BadLocationException {
		int lastLine = doc.getNumberOfLines() - 1;
		return doc.getLineOffset(lastLine);
	}
	
	public int getLastLineReadOnlySize() {
		return readOnlyColumnsInCurrentBeforePrompt + prompt.toString().length();
	}

	public void hadleConsoleTabCompletions() {
		final String commandLine = getCommandLine();
		final int commandLineOffset = viewer.getCommandLineOffset();
		final int caretOffset = viewer.getCaretOffset();
		
		// Don't block the UI
		Job j = new Job("Async Fetch completions") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				ICompletionProposal[] completions = handler
						.getTabCompletions(commandLine, caretOffset - commandLineOffset);
				if (completions.length == 0) {
					return Status.OK_STATUS;
				}
				
				// Evaluate all the completions
				final List<String> compList = new ArrayList<String>();
				
				boolean magicCommand = commandLine.startsWith("%")
						&& !commandLine.startsWith("%cd");
				for (ICompletionProposal completion : completions) {
					boolean magicCompletion = completion.getDisplayString().startsWith("%");
					Document doc = new Document(commandLine.substring(
							(magicCommand && magicCompletion) ? 1 : 0));
					completion.apply(doc);
					String out = doc.get().substring(
							(magicCommand && !magicCompletion) ? 1 : 0);
					if (out.startsWith("_", out.lastIndexOf('.') + 1)
							&& !commandLine.startsWith("_", commandLine.lastIndexOf('.') + 1)) {
						continue;
					}
					if (out.indexOf('(', commandLine.length()) != -1) {
						out = out.substring(0, out.indexOf('(', commandLine.length()));
					}
					compList.add(out);
				}
				// Discover the longest possible completions
				String longestCommonPrefix = null;
				for (String completion : compList) {
					if (!completion.startsWith(commandLine)) {
						continue;
					}
					if (longestCommonPrefix == null) {
						longestCommonPrefix = completion;
					} else {
						for (int i=0; i < longestCommonPrefix.length()
								&& i < completion.length(); i++) {
							if (longestCommonPrefix.charAt(i) != completion.charAt(i)) {
								longestCommonPrefix = longestCommonPrefix.substring(0, i);
								break;
							}
						}
						// Handle mismatched length
						if (longestCommonPrefix.length() > completion.length()) {
							longestCommonPrefix = completion;
						}
					}
				}
				if (longestCommonPrefix == null) {
					longestCommonPrefix = commandLine;
				}
				int length = 0;
				for (String completion : compList) {
					length = Math.max(length, completion.length());
				}
				
				final String fLongestCommonPrefix = longestCommonPrefix;
				final int maxLength = length;
				Runnable r = new Runnable() {
					@Override
					public void run() {
						// Get the viewer width + format the auto-completion output appropriately
						int consoleWidth = viewer.getConsoleWidthInCharacters();
						int formatLength = maxLength + 4;
						int completionsPerLine = consoleWidth / formatLength;
						if (completionsPerLine <= 0) {
							completionsPerLine = 1;
						}
						
						String formatString = "%-" + formatLength + "s";
						StringBuilder sb = new StringBuilder("\n");
						int i = 0;
						for (String completion : compList) {
							sb.append(String.format(formatString, completion));
							if (++i % completionsPerLine == 0) {
								sb.append("\n");
							}
						}
						sb.append("\n");
						
						String currentCommand = getCommandLine();
						try {
							startDisconnected();
							addToConsoleView(sb.toString(), true, true);
							// re-add prompt (>>)
							appendInvitation(false);
						} finally {
							stopDisconnected();
						}
						// auto-compete the command up to the longest common prefix
						if (!currentCommand.equals(commandLine)
								|| fLongestCommonPrefix.isEmpty()) {
							addToConsoleView(currentCommand, true, false);
						} else {
							addToConsoleView(fLongestCommonPrefix, true, false);
						}
					}
				};
				RunInUiThread.async(r);
	
				return Status.OK_STATUS;
			}
		};
		j.setPriority(Job.INTERACTIVE);
		j.setRule(new TabCompletionSingletonRule());
		j.setSystem(true);
		j.schedule();
	}

	private static class TabCompletionSingletonRule implements ISchedulingRule {
		@Override
		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}
		
		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return rule instanceof TabCompletionSingletonRule;
		}
	}

	public void handleEditSource(String str, int pos, int posDocument) {
		// Handle the command in a thread that doesn't block the U/I.
		handler.handleEdit(str, pos);
//		Job j = new Job("APLDev Console Handler") {
//			@Override
//			protected IStatus run(IProgressMonitor monitor) {
//				// send command to interpreter
//				handler.handleEdit(str, pos);
//				return Status.OK_STATUS;
//			}
//		};
//		j.setSystem(true);
//		j.schedule();
		
	}
}
