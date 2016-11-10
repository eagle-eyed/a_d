package com.dyalog.apldev.debug.core.console;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension2;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.console.TextConsoleViewer;

import com.dyalog.apldev.debug.core.console.actions.AbstractHandleBackspaceAction;
import com.dyalog.apldev.debug.core.console.actions.HandleDeletePreviousWord;
import com.dyalog.apldev.debug.core.console.actions.HandleLineStartAction;
import com.dyalog.apldev.debug.core.content.KeyBindingHelper;
import com.dyalog.apldev.debug.core.content.StringUtils;
import com.dyalog.apldev.log.Log;

/**
 * This is the viewer for the console. It's responsible for making sure that
 * the actions the user does are issued in the correct places in the document
 * and that only editable places are actually editable
 */
public class ScriptConsoleViewer extends TextConsoleViewer implements
		IScriptConsoleViewer, IScriptConsoleViewer2ForDocumentListener {

	/**
	 * Console itself
	 */
	protected ScriptConsole console;
	/**
	 * Listeners and acts to document changes (and passes them to the shell)
	 */
	private ScriptConsoleDocumentListener listener;
	/**
	 * Holds the command history for the console
	 */
	private ScriptConsoleHistory history;
	/**
	 * Attribute defines if this is the main viewer (other viewers may associated to the same document)
	 */
	private boolean isMainViewer;
	/**
	 * Provides the colors for the console
	 */
	private IConsoleStyleProvider styleProvider;
	/**
	 * Marks if a history request just started
	 */
	volatile int inHistoryRequests = 0;
	volatile boolean changedAfterLastHistoryRequest = false;
	/**
	 * determining if currently requesting a completion
	 */
	private boolean inCompletion = false;
	private IQuickAssistAssistant fQuickAssistant;
	protected boolean fQuickAssistAssistantInstalled;
	private final boolean focusOnStart;
	/**
	 * Handles a backspace (prevent deleting readonly parts)
	 */
	private AbstractHandleBackspaceAction handleBackspaceAction;
	private boolean showInitialCommands;
	/**
	 * Should tab completion be enabled in this interpreter
	 */
	private boolean tabCompletionEnabled;
	private boolean asyncSetCaretOffset;
	private int desiredCaretOffset;

	/**
	 * This class is responsible for checking if commands should be issued or
	 * not given the command requested and updating the caret to the correct
	 * position for it to happen (if needed).
	 */
	private final class KeyChecker implements VerifyKeyListener {

		private Method fHideMethod;
		
		private Method getHideMethod() {
			if (fHideMethod == null) {
				try {
					fHideMethod = ScriptConsoleViewer.this.fContentAssistant
							.getClass().getDeclaredMethod("hide");
					fHideMethod.setAccessible(true);
				} catch (Exception e) {
					Log.log(e);
				}
			}
			return fHideMethod;
		}
		
		@Override
		public void verifyKey(VerifyEvent event) {
			try {
				if (event.character == SWT.CR && event.stateMask == SWT.SHIFT) {
					// if called open source (pressed shift enter)
					IDocument doc = getDocument();
					int pos = getCaretOffset();
					int line = doc.getLineOfOffset(pos);
					int lineOffset = doc.getLineOffset(line);
					int lineLength = doc.getLineLength(line);
					String str = doc.get(lineOffset, lineLength);
					int linePos = pos - lineOffset;
					
//					IWorkbench workbench = PlatformUI.getWorkbench();
//					IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

					listener.handleEditSource(str, linePos, pos);
					event.doit = false;
//					getTextWidget().setCaretOffset(getDocument().getLength());
				} else
				
				if (event.character != '\0') {// if printable character
					if(Character.isLetter(event.character)
							&& (event.stateMask == 0
								|| (event.stateMask & SWT.SHIFT) != 0)
							|| Character.isWhitespace(event.character)) {
						if (!isSelectedRangeEditable()) {
							getTextWidget().setCaretOffset(getDocument()
									.getLength());
						}
					}
					
					if (!isSelectedRangeEditable()) {
						event.doit = false;
						return;
					}
					
					if (event.character == SWT.CR || event.character == SWT.LF){
						// if pressed enter with the shift and in a completion mode, stop it
						if (inCompletion && (event.stateMask & SWT.SHIFT) != 0) {
							// Work-around the fact that hide() is a protected method
							Method hideMethod = getHideMethod();
							if (hideMethod != null) {
								hideMethod.invoke(ScriptConsoleViewer.this.fContentAssistant);
							}
						}
						
						if (!inCompletion) {
							/* in a new line, always set the caret to the end of the document
							* (if not in completion) (note that when we make a hide in the
							* previous 'if', it will automatically exit the completion mode
							* (so, it'll also get into this part of the code)
							*/
							getTextWidget().setCaretOffset(getDocument().getLength());
						}
						return;
					}
					
					if (event.character == SWT.ESC){
						if (!inCompletion) {
							// while in a completion, ESC won't clear the line
							listener.setCommandLine("");
						}
						return;
					}
				} else { // not printable character
					if (isCaretInEditableRange()) {
						if (!inCompletion && event.keyCode == SWT.PAGE_UP) {
							event.doit = false;
							List<String> commandsToExecute = ScriptConsoleHistorySelector
									.select(history);
							if (commandsToExecute != null) {
								// remove the current command (substituted by the one gotten from page up)
								listener.setCommandLine("");
								IDocument d = getDocument();
								// Pass them all at once (let the document listener separate the command in lines).
//								d.replace(d.getLength(), 0, StringUtils.join("\n", commandsToExecute) + "\n");
								d.replace(d.getLength(), 0, StringUtils.join("\n", commandsToExecute));
//								d.replace(d.getLength(), 0, commandsToExecute);
							}
							return;
						}
					}
				}
			} catch (Exception e) {
				Log.log(e);
			}
		}
		
	}
	
	/**
	 * This is the text widget that's used to edit the console
	 */
	private class ScriptConsoleStyledText extends StyledText {
		/**
		 * Handles a delete previous word (should guarantee that it does not
		 * delete thing that are not in the last line -- nor in the prompt)
		 */
		private HandleDeletePreviousWord handleDeletePreviousWord;
		/**
		 * Handles a lint start action (home) stays within the same line
		 * changing from the 1st char of text, beginning of prompt, beginning
		 * of line
		 */
		private HandleLineStartAction handleLineStartAction;
		/**
		 * Contains the caret offset that has been set from the console API
		 */
		private volatile int internalCaretSet = -1;
		/**
		 * Set to true when drag source/target are the same console
		 */
		private boolean thisConsoleInitiatedDrag = false;
		
		public ScriptConsoleStyledText(Composite parent, int style) {
			super(parent, style);
			/**
			 * The StyledText will change the caretOffset that updated during
			 * the modifications, so, the verify and the extended modify listener
			 * will keep track if it actually does that and will reset the caret
			 * to the position we actually added it.
			 */
			addVerifyListener(new VerifyListener() {
				
				@Override
				public void verifyText(VerifyEvent e) {
					internalCaretSet = -1;
				}
			});
			
			/**
			 * Set to the location
			 */
			addExtendedModifyListener(new ExtendedModifyListener() {
				@Override
				public void modifyText(ExtendedModifyEvent event) {
					if (internalCaretSet != -1) {
						if (internalCaretSet != getCaretOffset()) {
							setCaretOffset(internalCaretSet);
						}
						internalCaretSet = -1;
					}
				}
			});
			
			initDragDrop();
			handleDeletePreviousWord = new HandleDeletePreviousWord();
			handleLineStartAction = new HandleLineStartAction();
		}
		
		private void initDragDrop() {
			DragSource dragSource = new DragSource(this, DND.DROP_COPY | DND.DROP_MOVE);
			dragSource.addDragListener(new DragSourceAdapter());
			dragSource.setTransfer(new Transfer[] { org.eclipse.swt.dnd.TextTransfer
					.getInstance() });
			DropTarget dropTarget = new DropTarget(this, DND.DROP_COPY | DND.DROP_MOVE);
			dropTarget.setTransfer(new Transfer[] { LocalSelectionTransfer.getTransfer(),
					org.eclipse.swt.dnd.TextTransfer.getInstance() });
			dropTarget.addDropListener(new DragTargetAdapter());
		}
		
		private final class DragSourceAdapter implements DragSourceListener {
			private Point selection;
			private String selectionText = null;
			private boolean selectionIsEditable;
			
			@Override
			public void dragStart(DragSourceEvent event) {
				thisConsoleInitiatedDrag = false;
				selectionText = null;
				event.doit = false;
				if (getSelectedRange().y > 0) {
					String temp_selection = new ClipboardHandler().getPlainText(
							getDocument(), getSelectedRange());
					if (temp_selection != null && temp_selection.length() > 0) {
						event.doit = true;
						selectionText = temp_selection;
						selection = getSelection();
						selectionIsEditable = isSelectedRangeEditable();
					}
				}
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
					event.data = selectionText;
					thisConsoleInitiatedDrag = true;
				}
			}
			
			@Override
			public void dragFinished(DragSourceEvent event) {
				try {
					if (event.detail == DND.DROP_MOVE && selectionIsEditable) {
						Point newSelection = getSelection();
						int length = selection.y - selection.x;
						int delta = 0;
						if (newSelection.x < selection.x){
							delta = length;
						}
						replaceTextRange(selection.x + delta, length, "");
					}
				} finally {
					thisConsoleInitiatedDrag = false;
				}
			}
		}
		
		private final class DragTargetAdapter implements DropTargetListener {
			
			private SafeScriptConsoleCodeGenerator getSafeGenerator() {
				ISelection selection = LocalSelectionTransfer.getTransfer()
						.getSelection();
				IScriptConsoleCodeGenerator codeGenerator = APLSnippetUtils
						.getScriptConsoleCodeGeneratorAdapter(selection);
				return new SafeScriptConsoleCodeGenerator(codeGenerator); 
			}
			
			/**
			 * Cancel the drop if it's anything to drop
			 */
			private boolean forceDropNone(DropTargetEvent event) {
				if (LocalSelectionTransfer.getTransfer()
						.isSupportedType(event.currentDataType)) {
					IScriptConsoleCodeGenerator codeGenerator = getSafeGenerator();
					if (codeGenerator == null || codeGenerator.hasAplCode() == false) {
						return true;
					}
				}
				return false;
			}
			
			private void adjustEventDetail(DropTargetEvent event) {
				if (forceDropNone(event)) {
					event.detail = DND.DROP_NONE;
				} else if (!thisConsoleInitiatedDrag &&
						(event.operations & DND.DROP_COPY) != 0) {
					event.detail = DND.DROP_COPY;
				} else if ((event.operations & DND.DROP_MOVE) != 0) {
					event.detail = DND.DROP_MOVE;
				} else if ((event.operations & DND.DROP_COPY) != 0) {
					event.detail = DND.DROP_COPY;
				} else {
					event.detail = DND.DROP_NONE;
				}
			}
			
			@Override
			public void dragEnter(DropTargetEvent event) {
				thisConsoleInitiatedDrag = false;
				adjustEventDetail(event);
			}
			
			@Override
			public void dragOver(DropTargetEvent event) {
				event.feedback |= DND.FEEDBACK_SCROLL;
			}
			
			@Override
			public void dragOperationChanged(DropTargetEvent event) {
				adjustEventDetail(event);
			}
			
			@Override
			public void dropAccept(DropTargetEvent event) {
				adjustEventDetail(event);
			}
			
			@Override
			public void drop(DropTargetEvent event) {
				if (event.operations == DND.DROP_NONE) {
					// noting to do
					return;
				}
				
				String text = null;
				if (TextTransfer.getInstance().isSupportedType(event.currentDataType)) {
					text = (String) event.data;
				} else if (LocalSelectionTransfer.getTransfer()
						.isSupportedType(event.currentDataType)) {
					IScriptConsoleCodeGenerator codeGenerator = getSafeGenerator();
					if (codeGenerator != null) {
						text = codeGenerator.getAplCode();
					}
				}
				
				if (text != null && text.length() > 0) {
					Point selectedRange = getSelectedRange();
					if (selectedRange.x < getLastLineOffset()) {
						changeSelectionToEditableRange();
					} else {
						int commandLineOffset = getCommandLineOffset();
						if (selectedRange.x < commandLineOffset) {
							setSelectedRange(commandLineOffset, 0);
						}
					}
					
					Point newSelection = getSelection();
					try {
						getDocument().replace(newSelection.x, 0, text);
					} catch (BadLocationException e) {
						return;
					}
					setSelectionRange(newSelection.x, text.length());
					changeSelectionToEditableRange();
				}
			}
			
			@Override
			public void dragLeave(DropTargetEvent event) {
				
			}
		}
		
		/**
		 * Overridden to keep track of changes in the caret
		 */
		@Override
		public void setCaretOffset(int offset) {
			internalCaretSet = offset;
			try {
			super.setCaretOffset(offset);
			} catch (SWTException e) {
				System.err.println(e);
			}
		}
		
		/**
		 * Execute some action
		 */
		@Override
		public void invokeAction(int action) {
			switch (action) {
			case ST.LINE_START:
				if (handleLineStartAction.execute(getDocument(), getCaretOffset(),
						getCommandLineOffset(), ScriptConsoleViewer.this)) {
					return;
				} else {
					super.invokeAction(action);
				}
			}
			
			if (isSelectedRangeEditable()) {
				try {
					int historyChange = 0;
					switch (action) {
					case ST.LINE_UP:
						historyChange = 1;
						break;
					case ST.LINE_DOWN:
						historyChange = 2;
						break;
					case ST.DELETE_PREVIOUS:
						handleBackspaceAction.execute(getDocument(),
								(ITextSelection) ScriptConsoleViewer.this.getSelection(),
								getCommandLineOffset());
						return;
					case ST.DELETE_WORD_PREVIOUS:
						handleDeletePreviousWord.execute(getDocument(),
								getCaretOffset(), getCommandLineOffset());
						return;
					}
					
					if (historyChange != 0) {
						if (changedAfterLastHistoryRequest) {
							// only set a new match if it didn't change since the last time
							history.setMatchStart(getCommandLine());
						}
						boolean didChange;
						if (historyChange == 1) {
							didChange = history.prev();
						} else {
							didChange = history.next();
						}
						
						if (didChange) {
							inHistoryRequests += 1;
							try {
								listener.setCommandLine(history.get());
								setCaretOffset(getDocument().getLength());
							} finally {
								inHistoryRequests -= 1;
							}
						}
						changedAfterLastHistoryRequest = false;
						return;
					}
				} catch (BadLocationException e) {
					Log.log(e);
					return;
				}
				
				super.invokeAction(action);
				
			} else {
				// not in editable range
				super.invokeAction(action);
			}
		}
		
		@Override
		public void cut() {
			changeSelectionToEditableRange();
			super.cut();
		}
		
		@Override
		public void paste() {
			changeSelectionToEditableRange();
			super.paste();
		}
		
		@Override
		public void copy() {
			copy(DND.CLIPBOARD);
		}
		
		@Override
		public void copy(int clipboardType) {
			// copy without prompt content
			checkWidget();
			
			Point selectedRange = getSelectedRange();
			if (selectedRange.y > 0) {
				IDocument doc = getDocument();

				new ClipboardHandler().putIntoClipboard(doc, selectedRange,
						clipboardType, getDisplay());
			}
		}
		
		/**
		 * 
		 */
		protected void changeSelectionToEditableRange() {
			Point range = getSelectedRange();
			int commandLineOffset = getCommandLineOffset();
			
			int minOffset = range.x;
			int maxOffset = range.x + range.y;
			boolean changed = false;
			boolean goToEnd = false;
			
			if (minOffset < commandLineOffset) {
				minOffset = commandLineOffset;
				changed = true;
			}
			
			if (maxOffset < commandLineOffset) {
				maxOffset = commandLineOffset;
				changed = true;
				// Only go to the end of the buffer if the max offset isn't in range
				goToEnd = true;
			}
			
			if (changed) {
//				minOffset = range.x;
//				maxOffset = range.x + range.y;
//				goToEnd = false;
				setSelectedRange(minOffset, maxOffset - minOffset);
			}
			
			if (goToEnd) {
				setCaretOffset(getDocument().getLength());
			}
		}
	}
	
	public ScriptConsoleViewer(Composite parent, ScriptConsole console,
			final IScriptConsoleContentHandler contentHandler,
			IConsoleStyleProvider styleProvider,
			String initialCommands, boolean focusOnStart,
			AbstractHandleBackspaceAction handleBackspaceAction,
			IHandleScriptAutoEditStrategy strategy,
			boolean tabCompletionEnabled, boolean showInitialCommands) {
		super(parent, console);
		
		this.showInitialCommands = showInitialCommands;
		this.handleBackspaceAction = handleBackspaceAction;
		this.focusOnStart = focusOnStart;
		this.tabCompletionEnabled = tabCompletionEnabled;
		this.console = console;
//		this.getTextWidget().setBackground(console.getAplDevConsoleBackground());
		
		ScriptConsoleViewer existingViewer = this.console.getViewer();
		
		if (existingViewer == null) {
			this.isMainViewer = true;
			this.console.setViewer(this);
			this.styleProvider = styleProvider;
			this.history = console.getHistory();
			this.listener = new ScriptConsoleDocumentListener(this, console,
					console.getPrompt(), console.getHistory(), console.createLineTrackers(console),
					initialCommands, strategy);
			this.listener.setDocument(getDocument());
		} else {
			this.isMainViewer = false;
			this.styleProvider = existingViewer.styleProvider;
			this.history = existingViewer.history;
			this.listener = existingViewer.listener;
			this.listener.addViewer(this);
		}
		
		final StyledText styledText = getTextWidget();
		
		// Added because we don't want the console to close when the user presses ESC
		// as it would when it's on a floating window
		styledText.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					e.doit = false;
				}
			}
		});
		
		getDocument().addDocumentListener(new IDocumentListener() {

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
				
			}

			@Override
			public void documentChanged(DocumentEvent event) {
				if (inHistoryRequests == 0) {
					changedAfterLastHistoryRequest = true;
				}
			}
			
		});
		
		styledText.addFocusListener(new FocusListener() {
			/**
			 * When the initial focus is gained, set the caret position to the
			 * last position (just after the prompt)
			 */
			@Override
			public void focusGained(FocusEvent e) {
				setCaretOffset(getDocument().getLength(), true);
				// just a 1-time listener
				styledText.removeFocusListener(this);
			}
			
			@Override
			public void focusLost(FocusEvent e) {
				
			}
		});
		
		styledText.addVerifyKeyListener(new KeyChecker());
		// verify if it was a content assist
		styledText.addVerifyKeyListener(new VerifyKeyListener() {
			@Override
			public void verifyKey(VerifyEvent event) {
				if (KeyBindingHelper.matchesContentAssistKeybinding(event)
						|| KeyBindingHelper.matchesQuickAssistKeybinding(event)) {
					event.doit = false;
					return;
				}
			}
		});
		
		// tab completion
		styledText.addVerifyKeyListener(new VerifyKeyListener() {
			@Override
			public void verifyKey(VerifyEvent event) {
				if (!ScriptConsoleViewer.this.tabCompletionEnabled ||
						inCompletion) {
					// already doing a code-completion
					return;
				}
				if (event.character == SWT.TAB && !listener.getCommandLine()
						.trim().isEmpty()) {
					// show completions when the user tabs in the console
					listener.hadleConsoleTabCompletions();
					event.doit = false;
				}
			}
		});
		
		// execute the content assist
		styledText.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (getCaretOffset() >= getCommandLineOffset()) {
					if (KeyBindingHelper.matchesContentAssistKeybinding(e)) {
						contentHandler.contentAssistRequired();
					} else if (KeyBindingHelper.matchesQuickAssistKeybinding(e)) {
						contentHandler.quickAssistRequired();
					}
				}
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				
			}
		});
	}

	public ScriptConsole getConsole() {
		return console;
	}
	
	/**
	 * Listen to the completions
	 */
	@Override
	public void configure(SourceViewerConfiguration configuration) {
		super.configure(configuration);
		ICompletionListener completionListener = new ICompletionListener() {
			
			@Override
			public void assistSessionStarted(ContentAssistEvent event) {
				inCompletion = true;
			}
			
			@Override
			public void assistSessionEnded(ContentAssistEvent event) {
				inCompletion = false;
			}
			
			@Override
			public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
				
			}
		};
		
		if (fContentAssistant != null) {
			((IContentAssistantExtension2) fContentAssistant)
				.addCompletionListener(completionListener);
		}
		if (fQuickAssistAssistant != null) {
			fQuickAssistAssistant.addCompletionListener(completionListener);
		}
		if (isMainViewer) {
			clear(showInitialCommands);
		}
		if (focusOnStart) {
			this.getTextWidget().setFocus();
		}
	}
	
	public IContentAssistant getContentAssist() {
		return fContentAssistant;
	}
	
	public IQuickAssistAssistant getQuickFixContentAssist() {
		return fQuickAssistAssistant;
	}
	
	/**
	 * @return true if the caret is currently in a edited command line
	 */
	protected boolean isCaretInEditableRange() {
		return getTextWidget().getCaretOffset() >= getCommandLineOffset();
	}
	
	/**
	 * Creates the styled text for the console
	 */
	@Override
	protected StyledText createTextWidget(Composite parent, int styles) {
		return new ScriptConsoleStyledText(parent, styles);
	}
	
	@Override
	public void revealEndOfDocument() {
		super.revealEndOfDocument();
	}
	
	/**
	 * 
	 * @return the number of characters visible on a line
	 */
	@Override
	public int getConsoleWidthInCharacters() {
		return getTextWidget().getSize().x / getWidthInPixels("a");
	}

	/**
	 * 
	 * @return the style provider that should be used
	 */
	@Override
	public IConsoleStyleProvider getStyleProvider() {
		return styleProvider;
	}

	@Override
	public IScriptConsoleSession getConsoleSession() {
		return this.console.getSession();
	}

	@Override
	public String getCommandLine() {
		return listener.getCommandLine();
	}

	@Override
	public int getCommandLineOffset() {
		try {
			return listener.getCommandLineOffset();
		} catch (BadLocationException e) {
			return -1;
		}
	}

	public int getLastLineOffset() {
		try {
			return listener.getLastLineOffset();
		} catch (BadLocationException e) {
			return -1;
		}
	}

	/**
	 * Clear the contents of the document
	 */
	public void clear (boolean addInitialCommands) {
		listener.clear(addInitialCommands);
	}
	
	public long getLastChangeMillis() {
		return listener.getLastChangeMillis();
	}
	
	public void discardCommandLine() {
		listener.discardCommandLine();
	}
	/**
	 * 
	 * @return the document caret offset
	 */
	@Override
	public int getCaretOffset() {
		if (asyncSetCaretOffset)
			return desiredCaretOffset;
		return getTextWidget().getCaretOffset();
	}

	/**
	 * Sets the new caret position in the console
	 * 
	 * @param offset
	 * @param async
	 */
	@Override
	public void setCaretOffset(int offset, boolean async) {

		final StyledText textWidget = getTextWidget();
		if (textWidget != null) {
			if (textWidget.isDisposed())
				System.out.println("disposed widget");
			desiredCaretOffset = offset;
			if (async) {
				Display display = textWidget.getDisplay();
				if (display != null) {
					asyncSetCaretOffset = true;
//					desiredCaretOffset = offset;
					display.asyncExec(new Runnable() {
						@Override
						public void run() {
							
								
							asyncSetCaretOffset = false;
//							textWidget.setCaretOffset(offset);
							textWidget.setCaretOffset(desiredCaretOffset);
						}
					});
				}
			} else {
				asyncSetCaretOffset = false;
				textWidget.setCaretOffset(offset);
			}
		}
	}

	@Override
	public Object getInterpreterInfo() {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected boolean isSelectedRangeEditable() {
		Point range = getSelectedRange();
		int commandLineOffset = getCommandLineOffset();
		
		if (range.x < commandLineOffset) {
			return false;
		}
	// TODO if partly overlapped
		if ((range.x + range.y) < commandLineOffset) {
			return false;
		}
		
		return true;
	}
	
	protected boolean isCaretInLastLine() throws BadLocationException {
		return getTextWidget().getCaretOffset() >= listener.getLastLineOffset();
	}
	
}
