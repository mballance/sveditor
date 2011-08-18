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
import net.sf.sveditor.core.db.SVDBClassDecl;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.SVDBScopeItem;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;
import net.sf.sveditor.core.parser.ParserSVDBFileFactory;
import net.sf.sveditor.core.parser.SVParseException;
import net.sf.sveditor.core.tests.SVDBTestUtils;

public class TestParseDataTypes extends TestCase {
	
	public void testTypedefVirtual() throws SVParseException {
		LogHandle log = LogFactory.getLogHandle("testTypedefVirtual");
		String content =
			"class foobar;\n" +
			"    typedef virtual my_if #(FOO, BAR, BAZ) my_if_t;\n" +
			"\n" +
			"endclass\n";
		ParserSVDBFileFactory parser = new ParserSVDBFileFactory(null);
		parser.init(new StringInputStream(content), "test");
		SVDBScopeItem scope = new SVDBScopeItem();
		
		parser.parsers().classParser().parse(scope, 0);
		
		assertTrue(scope.getChildren().iterator().hasNext());
		SVDBClassDecl cls = (SVDBClassDecl)scope.getChildren().iterator().next();
		
		for (ISVDBItemBase it : cls.getChildren()) {
			log.debug("it " + it.getType() + " " + SVDBItem.getName(it));
		}
		LogFactory.removeLogHandle(log);
	}
	
	public void testTypedefEnumFwdDecl() throws SVParseException {
		String content =
			"class foo;\n" +
			"    typedef enum foo_enum_t;\n" +
			"    foo_enum_t        my_var;\n" +
			"endclass\n"
			;
		
		runTest("testTypedefEnumFwdDecl", content,
				new String [] {"foo", "my_var"});
	}

	public void testEnumVarTFScope() throws SVParseException {
		SVCorePlugin.getDefault().enableDebug(false);
		String content =
			"class foo;\n" +
			"	function void foobar;\n" +
			"   	enum { A, B, C } enum_var;\n" +
			"	endfunction\n" +
			"endclass\n"
			;
		
		runTest("testTypedefEnumFwdDecl", content,
				new String [] {"foo", "enum_var"});
	}

	public void testStructVarTFScope() throws SVParseException {
		SVCorePlugin.getDefault().enableDebug(false);
		String content =
			"class foo;\n" +
			"	function void foobar;\n" +
			"   	struct { int A; int B; int C; } struct_var;\n" +
			"	endfunction\n" +
			"endclass\n"
			;
		
		runTest("testTypedefEnumFwdDecl", content,
				new String [] {"foo", "struct_var"});
	}

	public void testMultiDimArrayDecl() throws SVParseException {
		String content =
			"class foo;\n" +
			"    string				my_var[][2];\n" +
			"endclass\n"
			;
		
		runTest("testMultiDimArrayDecl", content,
				new String [] {"foo", "my_var"});
	}

	public void testVirtualInterfaceParameterizedClass() throws SVParseException {
		String content =
			"class my_class\n" + 
			"	#(\n" +
			"	type vif = virtual my_inteface, // causes parse error\n" +
			"	type data = pkg_mypackage::my_datatype\n" +
			"	) extends uvm_object;\n" +
			"		// class internals\n" +
			"	endclass\n"
			;
		
		runTest("testVirtualInterfaceParameterizedClass", content,
				new String[] {"my_class"});
	}
	
	public void testVirtualInterfaceClassParam() throws SVParseException {
		SVCorePlugin.getDefault().enableDebug(false);
		String content =
			"class my_class extends my_base_class #(virtual my_interface);\n" + 
			"		// class internals\n" +
			"	endclass\n"
			;
		
		runTest("testVirtualInterfaceClassParam", content,
				new String[] {"my_class"});
	}

	public void testTimeUnits() throws SVParseException {
		SVCorePlugin.getDefault().enableDebug(false);
		String content =
			"class my_class extends my_base_class #(virtual my_interface);\n" +
			"\n" +
			"	function do_something;\n" +
			"		time t_s = 0.5s;\n" +
			"		time t_ms = 0.5ms;\n" +
			"		time t_us = 0.5us;\n" +
			"		time t_ns = 0.5ns;\n" +
			"		time t_ps = 0.5ps;\n" +
			"		time t_fs = 0.5fs;\n" +
			"		time t_1s = 1s;\n" +
			"	endfunction\n" +
			"endclass\n"
			;
		
		runTest("testTimeUnits", content,
				new String[] {"my_class", "do_something", "t_s", "t_ms", "t_us",
					"t_ns", "t_ps", "t_fs", "t_1s"});
	}

	private void runTest(
			String			testname,
			String			doc,
			String			exp_items[]) {
		SVDBFile file = SVDBTestUtils.parse(doc, testname);
		
		SVDBTestUtils.assertNoErrWarn(file);
		SVDBTestUtils.assertFileHasElements(file, exp_items);
	}

}
