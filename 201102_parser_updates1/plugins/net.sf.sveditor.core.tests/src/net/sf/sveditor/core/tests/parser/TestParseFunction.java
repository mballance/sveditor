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


package net.sf.sveditor.core.tests.parser;

import junit.framework.TestCase;
import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.StringInputStream;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBTask;
import net.sf.sveditor.core.db.stmt.SVDBParamPortDecl;
import net.sf.sveditor.core.db.stmt.SVDBVarDeclItem;
import net.sf.sveditor.core.db.stmt.SVDBVarDeclStmt;
import net.sf.sveditor.core.parser.ParserSVDBFileFactory;
import net.sf.sveditor.core.parser.SVParseException;

public class TestParseFunction extends TestCase {
	
	public void testBasicFunction() throws SVParseException {
		String content =
			"function void foobar();\n" +
			"    a = 5;\n" +
			"endfunction\n";
		
		ParserSVDBFileFactory parser = new ParserSVDBFileFactory(null);
		parser.init(new StringInputStream(content), "test");
		
		parser.parsers().taskFuncParser().parse(null, 0);
	}

	public void testReturnOnlyFunction() throws SVParseException {
		String content =
			"class foobar;\n" +
			"local virtual function string foobar();\n" +
			"    return \"foobar\";\n" +
			"endfunction\n" +
			"endclass\n" 
			;
		
		
		ParserSVDBFileFactory parser = new ParserSVDBFileFactory(null);
		parser.init(new StringInputStream(content), "test");
		
		try {
			parser.parsers().classParser().parse(0);
		} catch (SVParseException e) {
			e.printStackTrace();
			throw e;
		}
	}

	// Tests that local variables are correctly recognized and that 
	// cast expressions are skipped appropriately
	public void testLocalVarsWithCast() throws SVParseException {
		String content =
			"function void foobar();\n" +
			"    int a = integer'(5);\n" +
			"    int b = longint'(6);\n" +
			"    a = 5;\n" +
			"endfunction\n";
		
		SVCorePlugin.getDefault().enableDebug(false);
		ParserSVDBFileFactory parser = new ParserSVDBFileFactory(null);
		parser.init(new StringInputStream(content), "test");
		
		SVDBTask func = parser.parsers().taskFuncParser().parse(null, 0);

		assertEquals(3, func.getItems().size());
		SVDBVarDeclItem a=null, b=null;
		for (ISVDBItemBase it_t : func.getItems()) {
			if (it_t.getType() == SVDBItemType.VarDeclStmt) {
				SVDBVarDeclStmt v = (SVDBVarDeclStmt)it_t;
				for (SVDBVarDeclItem vi : v.getVarList()) {
					if (vi.getName().equals("a")) {
						a = vi;
					} else if (vi.getName().equals("b")) {
						b = vi;
					}
				}
			}
		}
		assertNotNull(a);
		assertNotNull(b);
	}

	// Tests that local variables are correctly recognized and that 
	// cast expressions are skipped appropriately
	public void testLocalTimeVar() throws SVParseException {
		String content =
			"function void foobar();\n" +
			"    time t = 10ns;\n" +
			"    t = 20ns;\n" +
			"endfunction\n";
		
		SVCorePlugin.getDefault().enableDebug(true);
		ParserSVDBFileFactory parser = new ParserSVDBFileFactory(null);
		parser.init(new StringInputStream(content), "test");
		
		SVDBTask func = parser.parsers().taskFuncParser().parse(null, 0);

		assertEquals(2, func.getItems().size());
		assertTrue(func.getItems().get(0).getType() == SVDBItemType.VarDeclStmt);
		SVDBVarDeclStmt stmt = (SVDBVarDeclStmt)func.getItems().get(0);
		assertEquals("t", stmt.getVarList().get(0).getName());
	}

	// Tests that local variables are correctly recognized and that 
	// cast expressions are skipped appropriately
	public void testLocalTypedef() throws SVParseException {
		String content =
			"function void foobar();\n" +
			"    typedef foo #(BAR) foo_t;\n" +
			"    foo_t              a;\n" +
			"    a = 5;\n" +
			"endfunction\n";
		
		ParserSVDBFileFactory parser = new ParserSVDBFileFactory(null);
		parser.init(new StringInputStream(content), "test");
		
		SVDBTask func = parser.parsers().taskFuncParser().parse(null, 0);
		
		SVDBVarDeclItem a=null;
		for (ISVDBItemBase it : func.getItems()) {
			if (it.getType() == SVDBItemType.VarDeclStmt) {
				for (SVDBVarDeclItem vi : ((SVDBVarDeclStmt)it).getVarList()) {
					if (vi.getName().equals("a")) {
						a = vi;
					}
				}
			}
		}

		assertEquals(3, func.getItems().size());
		assertEquals("foo_t", SVDBItem.getName(func.getItems().get(0)));
		assertNotNull(a);
	}

	public void testStaticFunction() throws SVParseException {
		String content =
			"function static void foobar();\n" +
			"    int a;\n" +
			// "    a = 5;\n" +
			"endfunction\n";
		
		ParserSVDBFileFactory parser = new ParserSVDBFileFactory(null);
		parser.init(new StringInputStream(content), "test");
		
//		SVDBTaskFuncScope func = 
			parser.parsers().taskFuncParser().parse(null, 0);
	}
	
	public void testIfElseBody() throws SVParseException {
		String content =
			"function new();\n" +
			"    string ini_file;\n" +
			"\n" +
		    "    if ($value$plusargs(\"INFACT_AXI_FULL_PROTOCOL_INI=%s\", ini_file)) begin\n" +
			"        ovm_report_info(\"INFACT_AXI_FULL_PROTOCOL\",\n" + 
			"            {\"Loading parameters from .ini file \", ini_file});\n" +
			"        load_ini(ini_file);\n" +
			"    end else begin\n" +
			"        ovm_report_info(\"INFACT_AXI_FULL_PROTOCOL\",\n" + 
			"            {\"No .ini file specified\"});\n" +
			"    end\n" +
			"endfunction\n"
			;

		SVCorePlugin.getDefault().enableDebug(false);
		ParserSVDBFileFactory parser = new ParserSVDBFileFactory(null);
		parser.init(new StringInputStream(content), "test");
		
//		SVDBTaskFuncScope func = 
			parser.parsers().taskFuncParser().parse(null, 0);
	}

	public void testAutomaticFunction() throws SVParseException {
		String content =
			"function automatic void foobar();\n" +
			"    a = 5;\n" +
			"endfunction\n";
		
		ParserSVDBFileFactory parser = new ParserSVDBFileFactory(null);
		parser.init(new StringInputStream(content), "test");
		
		parser.parsers().taskFuncParser().parse(null, 0);
	}

	public void testParamListFunction() throws SVParseException {
		String content =
			"function automatic void foobar(\n" +			// 1
			"        input int foobar,\n" +					// 2
			"        ref object bar,\n" +					// 3
			"        int foo);\n" +							// 4
			"    a_type foo, bar;\n" +						// 5
			"    b_type foo_q[$];\n" +						// 6
			"    b_cls #(foobar, bar) elem;\n" +
			"    int i, j, k;\n" +
			"    for (int i=0; i<5; i++) begin\n" +
			"        a = 5;\n" +
			"    end\n" +
			"endfunction\n";
		
		SVCorePlugin.getDefault().enableDebug(false);
		ParserSVDBFileFactory parser = new ParserSVDBFileFactory(null);
		parser.init(new StringInputStream(content), "test");
		
		SVDBTask func = parser.parsers().taskFuncParser().parse(null, 0);
		
		assertEquals("bar", func.getParams().get(1).getVarList().get(0).getName());
		assertEquals(SVDBParamPortDecl.Direction_Ref,
				func.getParams().get(1).getDir());
	}

}
