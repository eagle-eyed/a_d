package com.dyalog.apldev.debug.core.model.remote;

/**
 * APL entity window 
 */
public class EntityWindow {

	int currentRow;
	private boolean debugger;
	/**
	 * 1 - defined function;
	 * 2 - simple character array;
	 * 4 - simple numeric array;
	 * 8 - mixed simple array;
	 * 16 - nested array;
	 * 32 - ⎕OR object
	 * 64 - native file;
	 * 128 - simple character vector;
	 * 256 - APL class;
	 * 1024 - APL interface;
	 * 2048 - APL session;
	 * 4096 - external function
	 */
	public final int entityType;
	public final String name;
	int offset;
	public final boolean readOnly;
	int size;
	private int[] stop;
	String[] text;
	private int tid;
	private String tname;
	public final int token;
	private int lineNum;
	private Object lock = new Object();
	private boolean fClosed;
	

	public EntityWindow(int currentRow, boolean debugger, int entityType, String name, int offset, boolean readOnly,
			int size, int[] stop, String[] text, int tid, String tname, int token) {
		this.currentRow = currentRow;
		this.debugger = debugger;
		this.entityType = entityType;
		this.name = name;
		this.offset = offset;
		this.readOnly = readOnly;
		this.size = size;
		this.stop = stop;
		this.text = text;
		this.tid = tid;
		this.tname = tname;
		this.token = token;
	}

	public int getThreadId() {
		return tid;
	}

	public String getName() {
		return name;
	}
	
	public String getThreadName() {
		return tname;
	}
	
	public int[] getStop() {
		return stop;
	}

	public void setStop(int[] stop) {
		synchronized (lock) {
			this.stop = stop;
		}
	}

	/**
	 * Add linebreakpoint to list
	 * 
	 * @return false if breakpoint was already present in list
	 */
	public boolean addStop(int lineNumber) {
		synchronized (lock ) {
			boolean add = true;
			// if value already in list
			for (int i = 0; i < stop.length; i++) {
				if (lineNumber == stop[i]) {
					add = false;
					break;
				}
			}
			if (add) {
				int[] updateStop = new int[stop.length + 1];
				int i = 0;
				for ( ; i < stop.length; i++) {
					updateStop[i] = stop[i];
				}
				updateStop[i] = lineNumber;
				this.stop = updateStop;
			}
			return add;
		}
	}
	
	/**
	 * Remove linebreakpoint from list
	 * 
	 * @return false if breakpoint not in list
	 */
	public boolean removeStop(int lineNumber) {
		synchronized(lock) {
			boolean containLine = false;
			int index = 0;
			for ( ; index < stop.length; index++) {
				if (stop[index] == lineNumber) {
					containLine = true;
					break;
				}
			}
			if (containLine) {
				int[] updateStop = new int[stop.length - 1];
				for (int j = 0; j < updateStop.length; j++) {
					if (j < index)
						updateStop[j] = stop[j];
					else
						updateStop[j] = stop[j + 1];
				}
				// update line breakpoint list
				stop = updateStop;
			}
			return containLine;
		}
	}
	
	/**
	 * Check if line breakpoint set 
	 */
	public boolean isStop (int lineNumber) {
		boolean result = false;
		for (int i = 0; i < stop.length; i++) {
			if (stop[i] == lineNumber) {
				result = true;
				break;
			}
		}
		return result;
	}
	
	public int getLineNumber() {
		return lineNum;
	}
	
	public void setLineNumer(int lineNum) {
		this.lineNum = lineNum;
	}

	public boolean isDebug() {
		return debugger;
	}

	/**
	 * Token content
	 */
	public String[] getTextAsArray() {
		return text;
	}
	
	public String getTextAsSingleStr() {
		String source = text[0];
		for (int i = 1; i < text.length; i++) {
			source += "\n" + text[i];
		}
		return source;
	}
	
	public void setClosed() {
		fClosed = true;
	}
	
	/**
	 * Return if was send close window command and entity could not be actual
	 */
	public boolean isClosed() {
		return fClosed;
	}

}
