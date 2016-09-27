package com.dyalog.apldev.debug.core.console;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;

public class SafeScriptConsoleCodeGenerator
	implements IScriptConsoleCodeGenerator {

	private final IScriptConsoleCodeGenerator unsafeGenerator;
	private boolean hasAplCode;
	private String aplCode;
	
	private final class HasAplCodeRunnable implements ISafeRunnable {
		
		@Override
		public void run() throws Exception {
			hasAplCode = unsafeGenerator.hasAplCode();
		}
		
		@Override
		public void handleException(Throwable exception) {
			hasAplCode = false;
		}
	}
	
	private final class GetAplCodeRunnable implements ISafeRunnable {
		
		@Override
		public void run() throws Exception {
			aplCode = unsafeGenerator.getAplCode();
		}
		
		@Override
		public void handleException(Throwable exception) {
			aplCode = null;
		}
	}
	
	/**
	 * Create a safe wrapped generator for a possibly unsafe one.
	 * @param unsafeGenerator generator to wrap
	 */
	public SafeScriptConsoleCodeGenerator(IScriptConsoleCodeGenerator unsafeGenerator) {
		this.unsafeGenerator = unsafeGenerator;
	}
	
	/**
	 * Calls nested generators getAlpCode in a SafeRunner, on any exception returns null
	 */
	@Override
	public String getAplCode() {
		String ret;
		try {
			SafeRunner.run(new GetAplCodeRunnable());
			ret = aplCode;
		} finally {
			aplCode = null;
		}
		return ret;
	}
	
	/**
	 * Calls nested generators getAplCode in a SafeRunner, on any exception returns false
	 */
	@Override
	public boolean hasAplCode() {
		SafeRunner.run(new HasAplCodeRunnable());
		return hasAplCode;
	}
}
