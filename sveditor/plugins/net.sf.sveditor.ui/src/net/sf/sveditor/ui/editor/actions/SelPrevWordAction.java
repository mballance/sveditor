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


package net.sf.sveditor.ui.editor.actions;

import java.util.ResourceBundle;

import net.sf.sveditor.core.scanner.SVCharacter;
import net.sf.sveditor.ui.editor.SVEditor;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.TextEditorAction;

public class SelPrevWordAction extends TextEditorAction {
	private SVEditor				fEditor;
	
	public SelPrevWordAction(
			ResourceBundle			bundle,
			String					prefix,
			SVEditor				editor) {
		super(bundle, prefix, editor);
		fEditor = editor;
	}

	@Override
	public void run() {
		StyledText text = fEditor.sourceViewer().getTextWidget();
		int offset = text.getCaretOffset();
		int start_offset = offset;
		
		// Adjust start_offset if selection currently set
		// In this case, we're tracking the outer bound of the selection
		if (text.getSelection() != null) {
			Point sel = text.getSelection();
			// If the caret is placed at the base of the region, extend.
			// If the caret is placed at the limit, contract
			// Otherwise, just reset
			if (sel.x == offset) {
				start_offset = sel.y;
			} else if (sel.y == offset) {
				start_offset = sel.x;
			}
		}
		
		String str = text.getText();
		
		offset--;
		if (offset < 0) {
			return;
		}
		
		int ch = str.charAt(offset);
		if (SVCharacter.isSVIdentifierPart(ch)) {
			// scan back to end or next non-id_part
			while (offset >= 0) {
				ch = str.charAt(offset);
				if (!SVCharacter.isSVIdentifierPart(ch)) {
					break;
				}
				offset--;
			}
		} else if (Character.isWhitespace(ch)) {
			// scan back to end or end of whitespace
			while (offset >= 0) {
				ch = str.charAt(offset);
				if (!Character.isWhitespace(ch)) {
					break;
				}
				offset--;
			}
		} else {
			offset--;
		}
		
		if (offset < 0) {
			offset = 0;
		}
		offset++;
		text.setCaretOffset(offset);
		text.setSelection(start_offset, offset);
	}
}
