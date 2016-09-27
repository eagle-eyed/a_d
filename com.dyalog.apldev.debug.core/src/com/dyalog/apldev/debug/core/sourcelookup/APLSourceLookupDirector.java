package com.dyalog.apldev.debug.core.sourcelookup;

import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

/**
 * APL source lookup director. For APL source lookup there is one source
 * lookup participant.
 */
public class APLSourceLookupDirector extends AbstractSourceLookupDirector {

	public void initializeParticipants() {
		addParticipants(new ISourceLookupParticipant[] {new APLSourceLookupParticipant()});
	}

}
