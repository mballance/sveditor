/****************************************************************************
 * Copyright (c) 2008-2011 Matthew Ballance and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Ballance - initial implementation
 ****************************************************************************/


package net.sf.sveditor.ui.editor;

import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;

public class SVPairMatcher extends DefaultCharacterPairMatcher {

	public SVPairMatcher(char pairs[]) {
		super(pairs, SVDocumentPartitions.SV_PARTITIONING);
	}
	
	/*
	public IRegion match(IDocument document, int offset) {
		try {
			return performMatch(document, offset);
		} catch (BadLocationException ble) {
			return null;
		}
	}
	 */

	/*
	private IRegion performMatch(IDocument document, int offset) throws BadLocationException {
		if (offset < 0 || document == null) return null;
		final char prevChar= document.getChar(Math.max(offset - 1, 0));
//		if ((prevChar == '<' || prevChar == '>') && !fHighlightAngularBrackets)
//			return null;
		if (prevChar == '<' && isLessThanOperator(document, offset - 1))
			return null;
		final IRegion region= super.match(document, offset);
		if (region == null) return region;
		if (prevChar == '>') {
			final int peer= region.getOffset();
			if (isLessThanOperator(document, peer)) return null;
		}
		return region;
	}
	 */

}

