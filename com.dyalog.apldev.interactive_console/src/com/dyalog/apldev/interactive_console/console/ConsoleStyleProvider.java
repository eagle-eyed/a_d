package com.dyalog.apldev.interactive_console.console;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.graphics.Color;

import com.dyalog.apldev.interactive_console.console.prefs.ColorManager;
import com.dyalog.apldev.interactive_console.content.Tuple;

public class ConsoleStyleProvider implements IConsoleStyleProvider {

	@Override
	public ScriptStyleRange createPromptStyle(String prompt, int offset) {
		TextAttribute attr = ColorManager.getDefault().getConsolePromptTextAttribute();
		return getIt(prompt, offset, attr, ScriptStyleRange.PROMPT);
	}

	@Override
	public ScriptStyleRange createUserInputStyle(String content, int offset) {
		TextAttribute attr = ColorManager.getDefault().getConsoleInputTextAttribute();
		return getIt(content, offset, attr, ScriptStyleRange.STDIN);
	}

	@Override
	public Tuple<List<ScriptStyleRange>, String> createInterpreterOutputStyle(String content, int offset) {
		ColorManager colorManager = ColorManager.getDefault();
		TextAttribute attr = colorManager.getConsoleOutputTextAttribute();
		return createInterpreterStdStyle(content, offset, colorManager, attr,
				ScriptStyleRange.STDOUT);
	}

	@Override
	public Tuple<List<ScriptStyleRange>, String> createInterpreterErrorStyle(String content, int offset) {
		ColorManager colorManager = ColorManager.getDefault();
		TextAttribute attr = colorManager.getConsoleErrorTextAttribute();
		return createInterpreterStdStyle(content, offset, colorManager, attr,
				ScriptStyleRange.STDERR);
	}

	private ScriptStyleRange getIt(String content, int offset,
			TextAttribute attr, int scriptStyle) {
		// background is the default (aready set)
		Color background = attr.getBackground();
		ScriptStyleRange res;
		try {
			res = new ScriptStyleRange(offset, content.length(), attr.getForeground(),
				background, scriptStyle, attr.getStyle());
		} catch (NullPointerException e) {
			return null;
		}
		return res;
	}
	
	public Tuple<List<ScriptStyleRange>, String> createInterpreterStdStyle(
			String content, int offset, ColorManager colorManger,
			TextAttribute attr, int style) {
		Tuple<List<ScriptStyleRange>, String> ret 
			= new Tuple<List<ScriptStyleRange>, String> (new ArrayList<ScriptStyleRange>(), "");
		ret.o1.add(getIt(content, offset, attr, style));
		ret.o2 = content;
		return ret;
	}
}
