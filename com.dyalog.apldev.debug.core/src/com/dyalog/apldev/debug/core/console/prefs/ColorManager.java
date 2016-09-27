package com.dyalog.apldev.debug.core.console.prefs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.dyalog.apldev.debug.core.APLDebugCorePlugin;

/**
 * Generic color manager
 */
public class ColorManager {
	
	private static ColorManager fgColorManager;
	/**
	 Cache for colors
	 */
	protected Map<RGB, Color> fColorTable = new HashMap<RGB, Color>(10);
    public static final RGB dimInp = new RGB(0, 0, 255);
    public static final RGB dimOut = new RGB(0, 0, 0);
    public static final RGB dimErr = new RGB(205, 0, 0);
    public static final RGB dimPrompt = new RGB(0, 205, 0);
	
	public static ColorManager getDefault() {
		if (fgColorManager == null) {
			fgColorManager = new ColorManager();
		}
		return fgColorManager;
	}
	
	public Color getColor(RGB rgb) {
		Display current = Display.getCurrent();
		if (current == null) {
			APLDebugCorePlugin.log(IStatus.ERROR, "Can't get color in non-ui thread", null);
		}
		Color color = fColorTable.get(rgb);
		if (color == null) {
			color = new Color(current, rgb);
			fColorTable.put(rgb, color);
		}
		return color;
	}
	
	public void dispose() {
		Iterator<Color> e = fColorTable.values().iterator();
		while (e.hasNext()) {
			e.next().dispose();
		}
		fgColorManager = null;
	}
	
	public TextAttribute getConsoleInputTextAttribute() {
		Color color = getColor(dimInp);
		return new TextAttribute(color, null, 0);
	}
	
	public TextAttribute getConsoleOutputTextAttribute() {
		Color color = getColor(dimOut);
		return new TextAttribute(color, null, 0);
	}
	
	public TextAttribute getConsoleErrorTextAttribute() {
		Color color = getColor(dimErr);
		return new TextAttribute(color, null, 0);
	}
	
	public TextAttribute getConsolePromptTextAttribute() {
		Color color = getColor(dimPrompt);
		return new TextAttribute(color, null, 0);
	}
}
