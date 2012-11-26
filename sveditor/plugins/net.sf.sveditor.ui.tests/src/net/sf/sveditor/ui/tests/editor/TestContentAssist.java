/****************************************************************************
 * Copyright (c) 2008-2010 Matthew Ballance and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Ballance - initial implementation
 ****************************************************************************/


package net.sf.sveditor.ui.tests.editor;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.Tuple;
import net.sf.sveditor.core.tests.CoreReleaseTests;
import net.sf.sveditor.core.tests.utils.TestUtils;
import net.sf.sveditor.ui.editor.SVEditor;
import net.sf.sveditor.ui.editor.SVSourceViewerConfiguration;
import net.sf.sveditor.ui.tests.editor.utils.AutoEditTester;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;

public class TestContentAssist extends EditorTestCaseBase {

	public void testContentAssistBuiltinShadow() throws CoreException, InterruptedException, BadLocationException {
		SVCorePlugin.getDefault().enableDebug(true);

		CoreReleaseTests.clearErrors();

		IProject p = TestUtils.createProject(getName());
		
		TestUtils.copy("", p.getFile("cls1.svh"));
		String path = "${workspace_loc}/" + getName() + "/cls1.svh";
		
		Tuple<SVEditor, AutoEditTester> ret = openAutoEditTester(path);
		SVEditor editor = ret.first();
		AutoEditTester auto_edit = ret.second();
		
		String content = 
				"class cls1;\n" +
				"	int			int_q[$];\n" +
				"	function void f1();\n" +
				"		int_q.\n" +
				"	endfunction\n" +
				"endclass\n"
				;

		auto_edit.type(content);

		fLog.debug("Caret offset: " + auto_edit.getCaretOffset());
		
		fLog.debug("OffsetOf(4): " + auto_edit.getDocument().getLineOffset(4));

		SVSourceViewerConfiguration config = editor.getSourceViewerConfig();
		
		IContentAssistant ca = config.getContentAssistant(editor.sourceViewer());
		IContentAssistProcessor cap = ca.getContentAssistProcessor(IDocument.DEFAULT_CONTENT_TYPE);
		
		System.out.println("cap class: " + cap.getClass().getName());
	
		int offset = auto_edit.getDocument().getLineOffset(3);
		offset += "        int_q.".length()-2;
		fLog.debug("Content: \"" + auto_edit.getDocument().get(offset, 8) + "\"");
		
		fLog.debug("Offset: " + offset);
		
		ICompletionProposal proposals[] = cap.computeCompletionProposals(
				editor.getTextViewer(), offset);
	
		for (ICompletionProposal prop : proposals) {
			fLog.debug("Proposal: " + prop.getDisplayString());
		}

		// Ensure expected proposals are present
		
		
	}
}
