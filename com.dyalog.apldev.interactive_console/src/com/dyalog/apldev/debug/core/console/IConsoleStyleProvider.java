package com.dyalog.apldev.debug.core.console;

import java.util.List;

import com.dyalog.apldev.debug.core.content.Tuple;

public interface IConsoleStyleProvider {

	ScriptStyleRange createPromptStyle(String prompt, int offset);
	
	ScriptStyleRange createUserInputStyle(String content, int offset);
	
	Tuple<List<ScriptStyleRange>, String> createInterpreterOutputStyle(String content, int offset);
	
	Tuple<List<ScriptStyleRange>, String> createInterpreterErrorStyle(String content, int offset);
}
