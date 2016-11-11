package com.dyalog.apldev.interactive_console.actions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.jface.text.IDocument;

public class AplPairMatcher {

	private char[] fPairs;

	public AplPairMatcher(char[] pairs) {
		fPairs = pairs;
	}

	public int searchForAnyOpeningPeer(int replaceOffset, IDocument doc) {
//		try {
//			fReader.configureBackwardReader(document, offset, true, true, true);
//			
//			Map< Character, Integer > stack = new HashMap< Character, Integer >();
//			HashSet< Character > closing = new HashSet< Character >();
//			HashSet< Character > opening = new HashSet< Character >();
//			
//			for (int i = 0; i < fPairs.length; i++) {
//				
//			}
//		}
		return 0;
	}

	public int searchForClosingPeer(int openingPeerOffset, char c, char peer, IDocument doc) {
		// TODO Auto-generated method stub
		return 0;
	}
}
