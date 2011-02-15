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


package net.sf.sveditor.core.tests.content_assist;

import java.util.List;

import junit.framework.TestCase;
import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.StringInputStream;
import net.sf.sveditor.core.Tuple;
import net.sf.sveditor.core.content_assist.SVCompletionProposal;
import net.sf.sveditor.core.db.ISVDBFileFactory;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.SVDBModIfcClassDecl;
import net.sf.sveditor.core.db.index.ISVDBIndexIterator;
import net.sf.sveditor.core.db.index.ISVDBItemIterator;
import net.sf.sveditor.core.scanutils.StringBIDITextScanner;
import net.sf.sveditor.core.tests.SVDBIndexValidator;
import net.sf.sveditor.core.tests.TextTagPosUtils;

import org.eclipse.core.runtime.NullProgressMonitor;

public class TestParamClassContentAssist extends TestCase {
	private ContentAssistIndex			fIndex;
	
	@Override
	protected void setUp() throws Exception {
		fIndex = new ContentAssistIndex();
	}

	public void testParameterizedField() {
		String doc =
			"class elem_t;\n" +
			"    int my_field;\n" +
			"endclass\n" +
			"\n" +
			"class container_t #(T=int);\n" +
			"    T            m_field;\n" +
			"endclass\n" +
			"\n" +
			"class my_class1;\n" +							// 1
			"       container_t #(elem_t)  cont;\n" +
			"\n" +
			"    function void my_func();\n" +
			"        cont.m_field.<<MARK>>\n" +
			"    endfunction\n" +
			"\n" +
			"endclass\n"
			;
		SVCorePlugin.getDefault().enableDebug(false);
		Tuple<SVDBFile, TextTagPosUtils> ini = contentAssistSetup(doc);
		
		StringBIDITextScanner scanner = new StringBIDITextScanner(ini.second().getStrippedData());
		TestCompletionProcessor cp = new TestCompletionProcessor(ini.first(), fIndex);
		
		scanner.seek(ini.second().getPosMap().get("MARK"));
		
		ISVDBIndexIterator index_it = cp.getIndexIterator();
		ISVDBItemIterator it = index_it.getItemIterator(new NullProgressMonitor());
		SVDBIndexValidator v = new SVDBIndexValidator();
		
		v.validateIndex(index_it.getItemIterator(new NullProgressMonitor()), SVDBIndexValidator.ExpectErrors);
		
		SVDBModIfcClassDecl my_class1 = null;
		
		while (it.hasNext()) {
			ISVDBItemBase it_t = it.nextItem();
			//System.out.println("    " + it_t.getType() + " " + it_t.getName());
			if (SVDBItem.getName(it_t).equals("my_class1")) {
				my_class1 = (SVDBModIfcClassDecl)it_t;
			}
		}
		
		assertNotNull(my_class1);
		
		System.out.println("[my_class1] " + my_class1.getItems().size() + " items");
		for (ISVDBItemBase it_t : my_class1.getItems()) {
			System.out.println("    [my_class1] " + it_t.getType() + " " + SVDBItem.getName(it_t));
		}
		
		cp.computeProposals(scanner, ini.first(), 
				ini.second().getLineMap().get("MARK"));
		List<SVCompletionProposal> proposals = cp.getCompletionProposals();
		
		// TODO: at some point, my_class1 and my_class2 will not be proposals,
		// since they are types not variables 
		validateResults(new String[] {"my_field"}, proposals);
	}

	public void testParameterizedFunction() {
		String doc =
			"class elem_t;\n" +
			"    int my_field;\n" +
			"endclass\n" +
			"\n" +
			"class container_t #(T=int);\n" +
			"    T            m_field;\n" +
			"    function T get_element();\n" +
			"        return m_field;\n" +
			"    endfunction\n" +
			"endclass\n" +
			"\n" +
			"class my_class1;\n" +							// 1
			"       container_t #(elem_t)  cont;\n" +
			"\n" +
			"    function void my_func();\n" +
			"        cont.get_element().<<MARK>>\n" +
			"    endfunction\n" +
			"\n" +
			"endclass\n"
			;
		SVCorePlugin.getDefault().enableDebug(false);
		Tuple<SVDBFile, TextTagPosUtils> ini = contentAssistSetup(doc);
		
		StringBIDITextScanner scanner = new StringBIDITextScanner(ini.second().getStrippedData());
		TestCompletionProcessor cp = new TestCompletionProcessor(ini.first(), fIndex);
		
		scanner.seek(ini.second().getPosMap().get("MARK"));
		
		ISVDBIndexIterator index_it = cp.getIndexIterator();
		ISVDBItemIterator it = index_it.getItemIterator(new NullProgressMonitor());
		SVDBIndexValidator v = new SVDBIndexValidator();
		
		v.validateIndex(index_it.getItemIterator(new NullProgressMonitor()), SVDBIndexValidator.ExpectErrors);
		
		SVDBModIfcClassDecl my_class1 = null;
		
		while (it.hasNext()) {
			ISVDBItemBase it_t = it.nextItem();
			//System.out.println("    " + it_t.getType() + " " + it_t.getName());
			if (SVDBItem.getName(it_t).equals("my_class1")) {
				my_class1 = (SVDBModIfcClassDecl)it_t;
			}
		}
		
		assertNotNull(my_class1);
		
		System.out.println("[my_class1] " + my_class1.getItems().size() + " items");
		for (ISVDBItemBase it_t : my_class1.getItems()) {
			System.out.println("    [my_class1] " + it_t.getType() + " " + SVDBItem.getName(it_t));
		}
		
		cp.computeProposals(scanner, ini.first(), 
				ini.second().getLineMap().get("MARK"));
		List<SVCompletionProposal> proposals = cp.getCompletionProposals();
		
		// TODO: at some point, my_class1 and my_class2 will not be proposals,
		// since they are types not variables 
		validateResults(new String[] {"my_field"}, proposals);
	}

	/*************** Utility Methods ********************/
	private Tuple<SVDBFile, TextTagPosUtils> contentAssistSetup(String doc) {
		TextTagPosUtils tt_utils = new TextTagPosUtils(new StringInputStream(doc));
		ISVDBFileFactory factory = SVCorePlugin.createFileFactory(null);
		
		SVDBFile file = factory.parse(tt_utils.openStream(), "doc");
		fIndex.setFile(file);

		return new Tuple<SVDBFile, TextTagPosUtils>(file, tt_utils);
	}
	
	private void validateResults(String expected[], List<SVCompletionProposal> proposals) {
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
