package com.dyalog.apldev.interactive_console.console;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.ide.ResourceUtil;

/**
 * Help create snippets of APL code
 */

public class APLSnippetUtils {

	/**
	 * Get a IScriptConsoleCodeGenerator adapter object for object
	 * @param selection
	 * @return
	 */
	public static IScriptConsoleCodeGenerator getScriptConsoleCodeGeneratorAdapter(Object object) {
		if (object == null) {
			return null;
		}
		if (object instanceof IScriptConsoleCodeGenerator) {
			return (IScriptConsoleCodeGenerator) object;
		}
		Object adaptedNode = ResourceUtil.getAdapter(object,
				IScriptConsoleCodeGenerator.class, true);
		if (adaptedNode instanceof IScriptConsoleCodeGenerator) {
			return (IScriptConsoleCodeGenerator) adaptedNode;
		}
		return null;
	}

}
