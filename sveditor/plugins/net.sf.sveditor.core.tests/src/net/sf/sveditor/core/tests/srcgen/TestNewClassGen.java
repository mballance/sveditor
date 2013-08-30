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


package net.sf.sveditor.core.tests.srcgen;

import java.io.IOException;
import java.io.InputStream;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.db.index.SVDBIndexCollection;
import net.sf.sveditor.core.db.index.plugin_lib.SVDBPluginLibIndexFactory;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;
import net.sf.sveditor.core.srcgen.NewClassGenerator;
import net.sf.sveditor.core.tests.SVCoreTestCaseBase;
import net.sf.sveditor.core.tests.SVCoreTestsPlugin;
import net.sf.sveditor.core.tests.indent.IndentComparator;
import net.sf.sveditor.core.tests.utils.TestUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

public class TestNewClassGen extends SVCoreTestCaseBase {

	public void testNewClassBasics() {
		String expected =
			"/****************************************************************************\n" +
			" * test.svh\n" +
			" ****************************************************************************/\n" +
			"\n" +
			"/**\n" +
			" * Class: new_class\n" +
			" * \n" +
			" * TODO: Add class documentation\n" +
			" */\n" +
			"class new_class;\n" +
			"	\n" +
			"	function new();\n" +
			"		\n" +
			"	endfunction\n" +
			"\n" +
			"\n" +
			"endclass\n" +
			"\n"
			;
		NewClassGenerator gen = new NewClassGenerator();
		LogHandle log = LogFactory.getLogHandle("testNewClassBasics");
		
		try {
			IProject project_dir = TestUtils.createProject("project");

			IFile file = project_dir.getFile("test.svh");
			assertEquals("Ensure file doesn't exist", false, file.exists());

			SVDBIndexCollection index_mgr = new SVDBIndexCollection("GLOBAL");
			index_mgr.addPluginLibrary(
					fIndexRgy.findCreateIndex(new NullProgressMonitor(),
							"GLOBAL", SVCorePlugin.SV_BUILTIN_LIBRARY, 
							SVDBPluginLibIndexFactory.TYPE, null));

			gen.generate(index_mgr, file, "new_class", null, true, new NullProgressMonitor());

			try {
				InputStream in = file.getContents();
				String content = SVCoreTestsPlugin.readStream(in);
				log.debug("content:\n" + content);
				
				IndentComparator.compare(log, "testNewClassBasics", 
						expected.trim(), content.trim());
				in.close();
			} catch (CoreException e) {
				fail("Caught exception: " + e.getMessage());
			} catch (IOException e) {
				fail("Caught exception: " + e.getMessage());
			}
		} finally {
		}
		LogFactory.removeLogHandle(log);
	}

	public void testNewClassSuperCtor() {
		String doc =
			"class base;\n" +
			"\n" +
			"    function new(int a, int b);\n" +
			"    endfunction\n" +
			"\n" +
			"endclass\n"
			;
		
		String expected =
			"/****************************************************************************\n" +
			" * test.svh\n" +
			" ****************************************************************************/\n" +
			"\n" +
			"/**\n" +
			" * Class: new_class\n" +
			" * \n" +
			" * TODO: Add class documentation\n" +
			" */\n" +
			"class new_class extends base;\n" +
			"	\n" +
			"	function new(int a, int b);\n" +
			"		super.new(a, b);\n" +
			"\n" +
			"	endfunction\n" +
			"\n" +
			"\n" +
			"endclass\n" +
			"\n"
			;
		NewClassGenerator gen = new NewClassGenerator();
		LogHandle log = LogFactory.getLogHandle("testNewClassSuperCtor");
		
		try {
			IProject project_dir = TestUtils.createProject("project");

			IFile file = project_dir.getFile("test.svh");
			assertEquals("Ensure file doesn't exist", false, file.exists());

			SVDBIndexCollection index_it = SrcGenTests.createIndex(doc);

			gen.generate(index_it, file, "new_class", "base", true, new NullProgressMonitor());

			try {
				InputStream in = file.getContents();
				String content = SVCoreTestsPlugin.readStream(in);
				log.debug("content:\n" + content);
				
				IndentComparator.compare(log, "testNewClassSuperCtor", 
						expected.trim(), content.trim());
				in.close();
			} catch (CoreException e) {
				fail("Caught exception: " + e.getMessage());
			} catch (IOException e) {
				fail("Caught exception: " + e.getMessage());
			}
		} finally {
		}
		LogFactory.removeLogHandle(log);
	}

	// TODO: not sure if just filling in the default parameter values is the best option
	public void testNewClassTemplateSuper() {
		String doc =
			"class base #(int foo=5, int bar=6);\n" +
			"\n" +
			"    function new(int a, int b);\n" +
			"    endfunction\n" +
			"\n" +
			"endclass\n"
			;
		
		String expected =
			"/****************************************************************************\n" +
			" * test.svh\n" +
			" ****************************************************************************/\n" +
			"\n" +
			"/**\n" +
			" * Class: new_class\n" +
			" * \n" +
			" * TODO: Add class documentation\n" +
			" */\n" +
			"class new_class extends base #(foo, bar);\n" +
			"	\n" +
			"	function new(int a, int b);\n" +
			"		super.new(a, b);\n" +
			"\n" +
			"	endfunction\n" +
			"\n" +
			"\n" +
			"endclass\n" +
			"\n"
			;
		NewClassGenerator gen = new NewClassGenerator();
		LogHandle log = LogFactory.getLogHandle("testNewClassTemplateSuper");
		
		try {
			IProject project_dir = TestUtils.createProject("project");

			IFile file = project_dir.getFile("test.svh");
			assertEquals("Ensure file doesn't exist", false, file.exists());

			SVDBIndexCollection index_it = SrcGenTests.createIndex(doc);

			gen.generate(index_it, file, "new_class", "base", true, new NullProgressMonitor());

			try {
				InputStream in = file.getContents();
				String content = SVCoreTestsPlugin.readStream(in);
				log.debug("content:\n" + content);
				
				IndentComparator.compare(log, "testNewClassTemplateSuper", 
						expected.trim(), content.trim());
				in.close();
			} catch (CoreException e) {
				fail("Caught exception: " + e.getMessage());
			} catch (IOException e) {
				fail("Caught exception: " + e.getMessage());
			}
		} finally {
		}
		LogFactory.removeLogHandle(log);
	}

}
