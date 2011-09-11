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


package net.sf.sveditor.core.tests.content_assist;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;

import junit.framework.TestCase;
import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.StringInputStream;
import net.sf.sveditor.core.Tuple;
import net.sf.sveditor.core.content_assist.SVCompletionProposal;
import net.sf.sveditor.core.db.ISVDBFileFactory;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBMarker;
import net.sf.sveditor.core.db.index.SVDBIndexRegistry;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;
import net.sf.sveditor.core.scanutils.StringBIDITextScanner;
import net.sf.sveditor.core.tests.TestNullIndexCacheFactory;
import net.sf.sveditor.core.tests.TextTagPosUtils;

public class TestModuleContentAssist extends TestCase {
	private ContentAssistIndex			fIndex;

	public void setUp() {
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		rgy.init(new TestNullIndexCacheFactory());
		fIndex = new ContentAssistIndex();
		fIndex.init(new NullProgressMonitor());
	}

	public void testModulePortAssist() {
		LogHandle log = LogFactory.getLogHandle("testModulePortAssist");
		SVCorePlugin.getDefault().enableDebug(false);
		
		String doc1 = 
				"module m1(input AAAA, output BBBB);\n" +
				"endmodule\n" +
				"\n" +
				"module m2;\n" +
				"\n" +
				"	m1 m(.A<<PORT>>\n" +
				"endmodule\n"
				;
			
			Tuple<SVDBFile, TextTagPosUtils> ini = contentAssistSetup(doc1);
			TextTagPosUtils tt_utils = ini.second();
			ISVDBFileFactory factory = SVCorePlugin.createFileFactory(null);
			
			List<SVDBMarker> markers = new ArrayList<SVDBMarker>();
			SVDBFile file = factory.parse(tt_utils.openStream(), "doc1", markers);
			StringBIDITextScanner scanner = new StringBIDITextScanner(tt_utils.getStrippedData());

			TestCompletionProcessor cp = new TestCompletionProcessor(
					"testModulePortAssist", file, fIndex);
			
			scanner.seek(tt_utils.getPosMap().get("PORT"));

			cp.computeProposals(scanner, file, tt_utils.getLineMap().get("PORT"));
			List<SVCompletionProposal> proposals = cp.getCompletionProposals();
			
			validateResults(new String[] {"AAAA"}, proposals);
			LogFactory.removeLogHandle(log);
	}

	public void testModulePortAssistNoPrefix() {
		LogHandle log = LogFactory.getLogHandle("testModulePortAssist");
		SVCorePlugin.getDefault().enableDebug(false);
		
		String doc1 = 
				"module m1(input AAAA, output BBBB);\n" +
				"endmodule\n" +
				"\n" +
				"module m2;\n" +
				"\n" +
				"	m1 m(.<<PORT>>\n" +
				"endmodule\n"
				;
			
			Tuple<SVDBFile, TextTagPosUtils> ini = contentAssistSetup(doc1);
			TextTagPosUtils tt_utils = ini.second();
			ISVDBFileFactory factory = SVCorePlugin.createFileFactory(null);
			
			List<SVDBMarker> markers = new ArrayList<SVDBMarker>();
			SVDBFile file = factory.parse(tt_utils.openStream(), "doc1", markers);
			StringBIDITextScanner scanner = new StringBIDITextScanner(tt_utils.getStrippedData());

			TestCompletionProcessor cp = new TestCompletionProcessor(
					"testModulePortAssistNoPrefix", file, fIndex);
			
			scanner.seek(tt_utils.getPosMap().get("PORT"));

			cp.computeProposals(scanner, file, tt_utils.getLineMap().get("PORT"));
			List<SVCompletionProposal> proposals = cp.getCompletionProposals();
			
			validateResults(new String[] {"AAAA", "BBBB"}, proposals);
			LogFactory.removeLogHandle(log);
	}

	public void testInitialBlockVariableAssist() {
		LogHandle log = LogFactory.getLogHandle("testInitialBlockVariableAssist");
		SVCorePlugin.getDefault().enableDebug(false);
		
		String doc1 = 
				"module m1(input AAAA, output BBBB);\n" +
				"	initial begin\n" +
				"		int AAAA, AABB, c;\n" +
				"		c = A<<MARK>>\n" +
				"	end\n" +
				"endmodule\n"
				;
			
			Tuple<SVDBFile, TextTagPosUtils> ini = contentAssistSetup(doc1);
			TextTagPosUtils tt_utils = ini.second();
			ISVDBFileFactory factory = SVCorePlugin.createFileFactory(null);
			
			List<SVDBMarker> markers = new ArrayList<SVDBMarker>();
			SVDBFile file = factory.parse(tt_utils.openStream(), "doc1", markers);
			StringBIDITextScanner scanner = new StringBIDITextScanner(tt_utils.getStrippedData());

			TestCompletionProcessor cp = new TestCompletionProcessor(
					"testModulePortAssist", file, fIndex);
			
			scanner.seek(tt_utils.getPosMap().get("MARK"));

			cp.computeProposals(scanner, file, tt_utils.getLineMap().get("MARK"));
			List<SVCompletionProposal> proposals = cp.getCompletionProposals();
			
			validateResults(new String[] {"AAAA", "AABB"}, proposals);
			LogFactory.removeLogHandle(log);
	}

	public void testInitialBlockVarFieldAssist() {
		LogHandle log = LogFactory.getLogHandle("testInitialBlockVariableAssist");
		SVCorePlugin.getDefault().enableDebug(false);
		
		String doc1 = 
				"	class foo;\n" +
				"		int AAAA;\n" +
				"		int AABB;\n" +
				"	endclass\n" +
				"\n" +
				"module m1(input AAAA, output BBBB);\n" +
				"	initial begin\n" +
				"		foo c;\n" +
				"		c.A<<MARK>>\n" +
				"	end\n" +
				"endmodule\n"
				;
			
			Tuple<SVDBFile, TextTagPosUtils> ini = contentAssistSetup(doc1);
			TextTagPosUtils tt_utils = ini.second();
			ISVDBFileFactory factory = SVCorePlugin.createFileFactory(null);
			
			List<SVDBMarker> markers = new ArrayList<SVDBMarker>();
			SVDBFile file = factory.parse(tt_utils.openStream(), "doc1", markers);
			StringBIDITextScanner scanner = new StringBIDITextScanner(tt_utils.getStrippedData());

			TestCompletionProcessor cp = new TestCompletionProcessor(
					"testModulePortAssist", file, fIndex);
			
			scanner.seek(tt_utils.getPosMap().get("MARK"));

			cp.computeProposals(scanner, file, tt_utils.getLineMap().get("MARK"));
			List<SVCompletionProposal> proposals = cp.getCompletionProposals();
			
			validateResults(new String[] {"AAAA", "AABB"}, proposals);
			LogFactory.removeLogHandle(log);
	}

	public void testInterfaceTaskVarAssist() {
		String testname = "testInterfaceTaskVarAssist";
		LogHandle log = LogFactory.getLogHandle(testname);
		SVCorePlugin.getDefault().enableDebug(true);
		
		String doc1 = 
				"interface i1();\n" +
				"\n" +
				"\n" +
				"	int			AAAA;\n" +
				"	int			AABB;\n" +
				"	int			BBBB;\n" +
				"\n" +
				"	task f;\n" +
				"		AA<<MARK>>\n" +
				"	endtask\n" +
				"endinterface\n"
				;
			
			Tuple<SVDBFile, TextTagPosUtils> ini = contentAssistSetup(doc1);
			TextTagPosUtils tt_utils = ini.second();
			ISVDBFileFactory factory = SVCorePlugin.createFileFactory(null);
			
			List<SVDBMarker> markers = new ArrayList<SVDBMarker>();
			SVDBFile file = factory.parse(tt_utils.openStream(), "doc1", markers);
			StringBIDITextScanner scanner = new StringBIDITextScanner(tt_utils.getStrippedData());

			TestCompletionProcessor cp = new TestCompletionProcessor(testname, file, fIndex);
			
			scanner.seek(tt_utils.getPosMap().get("MARK"));

			cp.computeProposals(scanner, file, tt_utils.getLineMap().get("MARK"));
			List<SVCompletionProposal> proposals = cp.getCompletionProposals();
			
			validateResults(new String[] {"AAAA", "AABB"}, proposals);
			LogFactory.removeLogHandle(log);
	}
		
	/*************** Utility Methods ********************/
	private Tuple<SVDBFile, TextTagPosUtils> contentAssistSetup(String doc) {
		TextTagPosUtils tt_utils = new TextTagPosUtils(new StringInputStream(doc));
		ISVDBFileFactory factory = SVCorePlugin.createFileFactory(null);
		
		List<SVDBMarker> markers = new ArrayList<SVDBMarker>();
		SVDBFile file = factory.parse(tt_utils.openStream(), "doc", markers);
		fIndex.setFile(file);

		return new Tuple<SVDBFile, TextTagPosUtils>(file, tt_utils);
	}
	
	private static void validateResults(String expected[], List<SVCompletionProposal> proposals) {
		for (String exp : expected) {
			boolean found = false;
			for (int i=0; i<proposals.size(); i++) {
				if (proposals.get(i).getReplacement().equals(exp)) {
					found = true;
					proposals.remove(i);
					break;
				}
			}
			
			assertTrue("Failed to find content proposal " + exp, found);
		}
		
		for (SVCompletionProposal p : proposals) {
			System.out.println("[ERROR] Unexpected proposal " + p.getReplacement());
		}
		assertEquals("Unexpected proposals", 0, proposals.size());
	}

}
