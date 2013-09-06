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


package net.sf.sveditor.core.tests.index;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.Tuple;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBMarker;
import net.sf.sveditor.core.db.index.ISVDBIndex;
import net.sf.sveditor.core.db.index.ISVDBIndexChangeListener;
import net.sf.sveditor.core.db.index.argfile.SVDBArgFileIndexFactory;
import net.sf.sveditor.core.db.index.old.SVDBLibIndex;
import net.sf.sveditor.core.db.index.old.SVDBLibPathIndexFactory;
import net.sf.sveditor.core.log.ILogLevel;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;
import net.sf.sveditor.core.tests.CoreReleaseTests;
import net.sf.sveditor.core.tests.IndexTestUtils;
import net.sf.sveditor.core.tests.SVCoreTestCaseBase;
import net.sf.sveditor.core.tests.SVCoreTestsPlugin;
import net.sf.sveditor.core.tests.utils.BundleUtils;
import net.sf.sveditor.core.tests.utils.TestUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;

public class TestIndexPersistance extends SVCoreTestCaseBase implements ISVDBIndexChangeListener {
	private int				fRebuildCount;
	
	public void index_changed(int reason, SVDBFile file) {}

	public void index_rebuilt() {
		try {
			throw new Exception("index_rebuilt");
		} catch (Exception e) {
			fLog.debug("Index Rebuilt", e);
		}
		fRebuildCount++;
	}

	public void testWSArgFileIndex() {
		SVCorePlugin.getDefault().enableDebug(false);
// 		SVCorePlugin.getDefault().setDebugLevel(0);
		LogHandle log = LogFactory.getLogHandle("testWSArgFileIndex");
		CoreReleaseTests.clearErrors();
		BundleUtils utils = new BundleUtils(SVCoreTestsPlugin.getDefault().getBundle());
		
		File test_dir = new File(fTmpDir, "testArgFileIndex");
		if (test_dir.exists()) {
			assertTrue(test_dir.delete());
		}
		assertTrue(test_dir.mkdirs());
		
		utils.unpackBundleZipToFS("/ovm.zip", test_dir);		
		File xbus = new File(test_dir, "ovm/examples/xbus");

		IProject p = TestUtils.createProject("xbus", xbus);
		SVCorePlugin.getDefault().getProjMgr().getProjectData(p);
		addProject(p);
		
		ISVDBIndex index;
		SVDBFile   file;
		InputStream in;
		String path = "${workspace_loc}/xbus/examples/xbus_demo_tb.sv";

		log.debug(ILogLevel.LEVEL_MIN, ">==== PASS 1 ====");
		// Create the index
		index = fIndexRgy.findCreateIndex(new NullProgressMonitor(), "xbus",
				"${workspace_loc}/xbus/examples/compile_questa_sv.f",
				SVDBArgFileIndexFactory.TYPE, null);
		index.addChangeListener(this);
		fRebuildCount=0;
		
		index.loadIndex(new NullProgressMonitor());
		
		in = index.getFileSystemProvider().openStream(path);
		List<SVDBMarker> errors = new ArrayList<SVDBMarker>();
		
		file = IndexTestUtils.parse(index, in, path, errors).second();
		
		assertNotNull(file);
		assertEquals(1, fRebuildCount);
		
		for (SVDBMarker m : errors) {
			log.debug(ILogLevel.LEVEL_MIN, "[ERROR] " + m.getMessage());
		}
		assertEquals("No errors", 0, errors.size());

		log.debug(ILogLevel.LEVEL_MIN, "<==== PASS 1 ====");

		// Now, tear down everything
		log.debug(ILogLevel.LEVEL_MIN, ">==== PASS 2 ====");
		reinitializeIndexRegistry();
		
		index = fIndexRgy.findCreateIndex(new NullProgressMonitor(), "xbus",
				"${workspace_loc}/xbus/examples/compile_questa_sv.f",
				SVDBArgFileIndexFactory.TYPE, null);
		index.addChangeListener(this);
		fRebuildCount=0;

		in = index.getFileSystemProvider().openStream(path);
		
		Tuple<SVDBFile, SVDBFile> parse_r = index.parse(new NullProgressMonitor(), in, path, null);
		
		assertNotNull(parse_r);
		assertNotNull(parse_r.second());
		
		file = parse_r.second();
		
		assertNotNull(file);
		assertEquals(0, fRebuildCount);
		log.debug(ILogLevel.LEVEL_MIN, "<==== PASS 2 ====");
		
		// Ensure no errors were produced
		assertEquals(0, CoreReleaseTests.getErrors().size());
		LogFactory.removeLogHandle(log);
	}

	public void testWSLibIndex() {
		CoreReleaseTests.clearErrors();
		SVCorePlugin.getDefault().enableDebug(false);
		LogHandle log = LogFactory.getLogHandle("testWSLibIndex");
		BundleUtils utils = new BundleUtils(SVCoreTestsPlugin.getDefault().getBundle());
		
		File test_dir = new File(fTmpDir, "testLibIndex");
		assertTrue(test_dir.mkdirs());
		
		utils.unpackBundleZipToFS("/ovm.zip", test_dir);		
		File ovm = new File(test_dir, "ovm");
		
		IProject p = TestUtils.createProject("ovm", ovm);
		SVCorePlugin.getDefault().getProjMgr().getProjectData(p);
		addProject(p);
		
		ISVDBIndex index;
		SVDBFile   file;
		InputStream in;
		String path = "${workspace_loc}/ovm/src/base/ovm_component.svh";

		log.debug(">==== PASS 1 ====");
		// Create the index
		index = fIndexRgy.findCreateIndex(new NullProgressMonitor(), "ovm",
				"${workspace_loc}/ovm/src/ovm_pkg.sv",
				SVDBLibPathIndexFactory.TYPE, null);
		index.addChangeListener(this);
		fRebuildCount=0;
		
		in = ((SVDBLibIndex)index).getFileSystemProvider().openStream(path);
		
		List<SVDBMarker> errors = new ArrayList<SVDBMarker>();		
		file = index.parse(new NullProgressMonitor(), in, path, errors).second();
		
		assertNotNull(file);
		assertEquals(1, fRebuildCount);
		
		for (SVDBMarker m : errors) {
			log.debug("[ERROR] " + m.getMessage());
		}
		assertEquals("No errors", 0, errors.size());

		log.debug("<==== PASS 1 ====");

		// Now, tear down everything
		log.debug(">==== PASS 2 ====");
		reinitializeIndexRegistry();
		
		index = fIndexRgy.findCreateIndex(new NullProgressMonitor(), 
				"ovm", "${workspace_loc}/ovm/src/ovm_pkg.sv",
				SVDBLibPathIndexFactory.TYPE, null);
		index.addChangeListener(this);
		fRebuildCount=0;

		in = ((SVDBLibIndex)index).getFileSystemProvider().openStream(path); 
		file = index.parse(new NullProgressMonitor(), in, path, null).second();
		
		assertNotNull(file);
		assertEquals(0, fRebuildCount);
		log.debug("<==== PASS 2 ====");
		
		assertEquals(0, CoreReleaseTests.getErrors().size());
		LogFactory.removeLogHandle(log);
	}

}
