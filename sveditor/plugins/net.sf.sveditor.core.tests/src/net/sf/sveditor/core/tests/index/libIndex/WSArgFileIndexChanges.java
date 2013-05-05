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


package net.sf.sveditor.core.tests.index.libIndex;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.List;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.index.ISVDBIndex;
import net.sf.sveditor.core.db.index.SVDBDeclCacheItem;
import net.sf.sveditor.core.db.index.argfile.SVDBArgFileIndexFactory;
import net.sf.sveditor.core.db.search.SVDBFindDefaultNameMatcher;
import net.sf.sveditor.core.tests.IndexTestUtils;
import net.sf.sveditor.core.tests.SVCoreTestCaseBase;
import net.sf.sveditor.core.tests.SVCoreTestsPlugin;
import net.sf.sveditor.core.tests.utils.BundleUtils;
import net.sf.sveditor.core.tests.utils.TestUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

public class WSArgFileIndexChanges extends SVCoreTestCaseBase {
	
	public void testArgFileChange() {
		SVCorePlugin.getDefault().enableDebug(false);
		
		int_testArgFileChange(fTmpDir);
	}
	
	private void int_testArgFileChange(File tmpdir) {
		System.out.println("Begin int_testArgFileChange");
		BundleUtils utils = new BundleUtils(SVCoreTestsPlugin.getDefault().getBundle());
		
		IProject p = TestUtils.createProject("project");
		addProject(p);
		
		utils.copyBundleDirToWS("/data/basic_lib_project/", p);
		
		reinitializeIndexRegistry();
		
		ISVDBIndex index = fIndexRgy.findCreateIndex(new NullProgressMonitor(), "GENERIC", 
				"${workspace_loc}/project/basic_lib_project/basic_lib.f", 
				SVDBArgFileIndexFactory.TYPE, null);
		
		IndexTestUtils.assertFileHasElements(fLog, index, "class1");
		IndexTestUtils.assertDoesNotContain(index, "class1_2");
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(out);
		
		ps.println("\n\n");
		ps.println("class class1_2;\n");
		ps.println("\n\n");
		ps.println("endclass\n\n");
		ps.flush();
		
		// Now, write back the file
		TestUtils.copy(out, p.getFile(new Path("basic_lib_project/class1_2.svh")));

		out = new ByteArrayOutputStream();
		ps = new PrintStream(out);
		
		ps.println("\n\n");
		ps.println("basic_lib_pkg.sv");
		ps.println("class1_2.svh");
		ps.flush();
		
		// Now, write back the file
		TestUtils.copy(out, p.getFile(new Path("basic_lib_project/basic_lib.f")));
	
		System.out.println("--> Sleep");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {}
		System.out.println("<-- Sleep");

		IndexTestUtils.assertFileHasElements(fLog, index, "class1");
		IndexTestUtils.assertFileHasElements(fLog, index, "class1_2");

		index.dispose();
	}

}
