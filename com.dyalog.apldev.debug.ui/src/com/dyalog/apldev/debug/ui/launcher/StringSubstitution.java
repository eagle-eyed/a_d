package com.dyalog.apldev.debug.ui.launcher;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;

public class StringSubstitution {
	private Map<String, String> variableSubstitution = null;

	public String performStringSubstitution(String expression, boolean reportUndefinedVariables) 
				throws CoreException {
		VariablesPlugin plugin = VariablesPlugin.getDefault();
		expression = plugin.getStringVariableManager().performStringSubstitution(expression, reportUndefinedVariables);
		return expression;
	}

}
