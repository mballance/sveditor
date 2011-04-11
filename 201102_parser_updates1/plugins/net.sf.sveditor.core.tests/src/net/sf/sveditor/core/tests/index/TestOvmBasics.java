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
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.SVDBClassDecl;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBMarker;
import net.sf.sveditor.core.db.SVDBMarker.MarkerType;
import net.sf.sveditor.core.db.index.ISVDBIndex;
import net.sf.sveditor.core.db.index.ISVDBItemIterator;
import net.sf.sveditor.core.db.index.SVDBArgFileIndexFactory;
import net.sf.sveditor.core.db.index.SVDBIndexCollectionMgr;
import net.sf.sveditor.core.db.index.SVDBIndexRegistry;
import net.sf.sveditor.core.db.index.plugin_lib.SVDBPluginLibIndexFactory;
import net.sf.sveditor.core.db.stmt.SVDBStmt;
import net.sf.sveditor.core.db.stmt.SVDBVarDeclStmt;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;
import net.sf.sveditor.core.tests.SVCoreTestsPlugin;
import net.sf.sveditor.core.tests.TestIndexCacheFactory;
import net.sf.sveditor.core.tests.utils.BundleUtils;
import net.sf.sveditor.core.tests.utils.TestUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;

public class TestOvmBasics extends TestCase {
	
	private File			fTmpDir;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		fTmpDir = TestUtils.createTempDir();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		
		if (fTmpDir != null) {
			TestUtils.delete(fTmpDir);
		}
	}

	public void testBasicProcessing() {
		File tmpdir = new File(fTmpDir, "no_errors");
		SVCorePlugin.getDefault().enableDebug(false);
		LogHandle log = LogFactory.getLogHandle("testBasicProcessing");
		
		if (tmpdir.exists()) {
			tmpdir.delete();
		}
		tmpdir.mkdirs();
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		rgy.init(TestIndexCacheFactory.instance(tmpdir));
	
		SVDBIndexCollectionMgr index_mgr = new SVDBIndexCollectionMgr("GLOBAL");
		index_mgr.addPluginLibrary(
				rgy.findCreateIndex(new NullProgressMonitor(), "GLOBAL", "org.ovmworld.ovm", 
						SVDBPluginLibIndexFactory.TYPE, null));
		
		ISVDBItemIterator index_it = index_mgr.getItemIterator(new NullProgressMonitor());
		List<SVDBMarker> markers = new ArrayList<SVDBMarker>();
		ISVDBItemBase ovm_component=null, ovm_sequence=null;
		SVDBFile current_file = null;
		
		while (index_it.hasNext()) {
			ISVDBItemBase it = index_it.nextItem();
			String name = SVDBItem.getName(it);
			log.debug("" + it.getType() + " " + name);
			
			if (it.getType() == SVDBItemType.File) {
				current_file = (SVDBFile)it;
			} else if (it.getType() == SVDBItemType.Marker) {
				markers.add((SVDBMarker)it);
			} else if (it.getType() == SVDBItemType.ClassDecl) {
				if (name.equals("ovm_component")) {
					ovm_component = it;
				} else if (name.equals("ovm_sequence")) {
					ovm_sequence = it;
				}
			} else if (it.getType() == SVDBItemType.MacroDef) {
			} else if (SVDBStmt.isType(it, SVDBItemType.VarDeclStmt)) {
				SVDBVarDeclStmt v = (SVDBVarDeclStmt)it;
				if (v.getParent() == null) {
					log.debug("Current file is: " + current_file.getFilePath());
					log.debug("    Lineno: " + v.getLocation().getLine());
				}
				assertNotNull("Variable " + SVDBItem.getName(v.getVarList().get(0)) + " has null parent", v.getParent());
				assertNotNull("Variable " + SVDBItem.getName(v.getParent()) + "." +
						name + " has a null TypeInfo", v.getTypeInfo());
			}
		}
		
		for (SVDBMarker m : markers) {
			log.debug("[ERROR] " + m.getMessage());
		}
		
		assertEquals("Check that no errors were found", 0, markers.size());
		assertNotNull("Check found ovm_sequence", ovm_sequence);
		assertNotNull("Check found ovm_component", ovm_component);
		LogFactory.removeLogHandle(log);
	}
	
	public void testXbusExample() {
		SVCorePlugin.getDefault().enableDebug(false);
		LogHandle log = LogFactory.getLogHandle("testXbusExample");
		BundleUtils utils = new BundleUtils(SVCoreTestsPlugin.getDefault().getBundle());
		
		File test_dir = new File(fTmpDir, "testXbusExample");
		if (test_dir.exists()) {
			test_dir.delete();
		}
		test_dir.mkdirs();
		
		utils.unpackBundleZipToFS("/ovm.zip", test_dir);		
		File xbus = new File(test_dir, "ovm/examples/xbus");
		
		IProject project_dir = TestUtils.createProject("xbus", xbus);
		
		File db = new File(fTmpDir, "db");
		if (db.exists()) {
			TestUtils.delete(db);
		}
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		rgy.init(TestIndexCacheFactory.instance(db));
		
		ISVDBIndex index = rgy.findCreateIndex(new NullProgressMonitor(), "GENERIC",
				"${workspace_loc}/xbus/examples/compile_questa_sv.f",
				SVDBArgFileIndexFactory.TYPE, null);
		
		ISVDBItemIterator it = index.getItemIterator(new NullProgressMonitor());
		List<SVDBMarker> errors = new ArrayList<SVDBMarker>();
		
		while (it.hasNext()) {
			ISVDBItemBase tmp_it = it.nextItem();
			
			if (tmp_it.getType() == SVDBItemType.Marker) {
				SVDBMarker m = (SVDBMarker)tmp_it;
				if (m.getMarkerType() == MarkerType.Error) {
					errors.add(m);
				}
			}
			
			//System.out.println("tmp_it=" + tmp_it.getName());
		}
		
		for (SVDBMarker m : errors) {
			log.debug("[ERROR] " + m.getMessage());
		}
		assertEquals("No errors", 0, errors.size());
		
		TestUtils.deleteProject(project_dir);
		LogFactory.removeLogHandle(log);
	}

	public void testTrivialExample() {
		LogHandle log = LogFactory.getLogHandle("testTrivialExample");
		BundleUtils utils = new BundleUtils(SVCoreTestsPlugin.getDefault().getBundle());
		
		File test_dir = new File(fTmpDir, "testTrivialExample");
		if (test_dir.exists()) {
			test_dir.delete();
		}
		test_dir.mkdirs();
		
		utils.unpackBundleZipToFS("/ovm.zip", test_dir);		
		File trivial = new File(test_dir, "ovm/examples/trivial");
		
		/* IProject project_dir = */ TestUtils.createProject("trivial", trivial);
		
		File db = new File(fTmpDir, "db");
		if (db.exists()) {
			TestUtils.delete(db);
		}
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		rgy.init(TestIndexCacheFactory.instance(db));
		
		ISVDBIndex index = rgy.findCreateIndex(new NullProgressMonitor(), "GENERIC",
				"${workspace_loc}/trivial/compile_questa_sv.f",
				SVDBArgFileIndexFactory.TYPE, null);
		
		ISVDBItemIterator it = index.getItemIterator(new NullProgressMonitor());
		List<SVDBMarker> errors = new ArrayList<SVDBMarker>();
		
		while (it.hasNext()) {
			ISVDBItemBase tmp_it = it.nextItem();
			
			if (tmp_it.getType() == SVDBItemType.Marker) {
				SVDBMarker m = (SVDBMarker)tmp_it;
				if (m.getMarkerType() == MarkerType.Error) {
					errors.add(m);
				}
			}
			
			//System.out.println("tmp_it=" + tmp_it.getName());
		}
		
		for (SVDBMarker m : errors) {
			log.debug("[ERROR] " + m.getMessage());
		}
		assertEquals("No errors", 0, errors.size());
		LogFactory.removeLogHandle(log);
	}
	
	public void testSequenceBasicReadWriteExample() {
		BundleUtils utils = new BundleUtils(SVCoreTestsPlugin.getDefault().getBundle());
		
		File test_dir = new File(fTmpDir, "testSequenceBasicReadWriteExample");
		if (test_dir.exists()) {
			test_dir.delete();
		}
		test_dir.mkdirs();
		
		utils.unpackBundleZipToFS("/ovm.zip", test_dir);		
		File basic_read_write_sequence = new File(test_dir, "ovm/examples/sequence/basic_read_write_sequence");
		
		/* IProject project_dir = */ TestUtils.createProject("basic_read_write_sequence", basic_read_write_sequence);
		
		File db = new File(fTmpDir, "db");
		if (db.exists()) {
			db.delete();
		}
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		rgy.init(TestIndexCacheFactory.instance(db));
		
		ISVDBIndex index = rgy.findCreateIndex("GENERIC",
				"${workspace_loc}/basic_read_write_sequence/compile_questa_sv.f",
				SVDBArgFileIndexFactory.TYPE, null);
		
		ISVDBItemIterator it = index.getItemIterator(new NullProgressMonitor());
		List<SVDBMarker> errors = new ArrayList<SVDBMarker>();
		
		SVDBClassDecl my_driver = null;
		
		while (it.hasNext()) {
			ISVDBItemBase tmp_it = it.nextItem();
			
			if (tmp_it.getType() == SVDBItemType.Marker) {
				SVDBMarker m = (SVDBMarker)tmp_it;
				if (m.getMarkerType() == MarkerType.Error) {
					errors.add(m);
				}
			} else if (tmp_it.getType() == SVDBItemType.ClassDecl &&
					SVDBItem.getName(tmp_it).equals("my_driver")) {
				my_driver = (SVDBClassDecl)tmp_it;
			}
			
			//System.out.println("tmp_it=" + tmp_it.getName());
		}
		
		for (SVDBMarker m : errors) {
			System.out.println("[ERROR] " + m.getMessage());
		}
		assertEquals("No errors", 0, errors.size());
		
		assertNotNull(my_driver);
	}

	public void testSequenceSimpleExample() {
		BundleUtils utils = new BundleUtils(SVCoreTestsPlugin.getDefault().getBundle());
		
		File test_dir = new File(fTmpDir, "testSequenceSimpleExample");
		if (test_dir.exists()) {
			test_dir.delete();
		}
		test_dir.mkdirs();
		
		utils.unpackBundleZipToFS("/ovm.zip", test_dir);		
		File simple = new File(test_dir, "ovm/examples/sequence/simple");
		
		/* IProject project_dir = */ TestUtils.createProject("simple", simple);
		
		File db = new File(fTmpDir, "db");
		if (db.exists()) {
			db.delete();
		}
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		rgy.init(TestIndexCacheFactory.instance(db));
		
		ISVDBIndex index = rgy.findCreateIndex("GENERIC",
				"${workspace_loc}/simple/compile_questa_sv.f",
				SVDBArgFileIndexFactory.TYPE, null);
		
		ISVDBItemIterator it = index.getItemIterator(new NullProgressMonitor());
		List<SVDBMarker> errors = new ArrayList<SVDBMarker>();
		
		SVDBClassDecl simple_driver = null;
		
		while (it.hasNext()) {
			ISVDBItemBase tmp_it = it.nextItem();
			
			if (tmp_it.getType() == SVDBItemType.Marker) {
				SVDBMarker m = (SVDBMarker)tmp_it;
				if (m.getMarkerType() == MarkerType.Error) {
					errors.add(m);
				}
			} else if (tmp_it.getType() == SVDBItemType.ClassDecl &&
					SVDBItem.getName(tmp_it).equals("simple_driver")) {
				simple_driver = (SVDBClassDecl)tmp_it;
			}
			
			//System.out.println("tmp_it=" + tmp_it.getName());
		}
		
		for (SVDBMarker m : errors) {
			System.out.println("[ERROR] " + m.getMessage());
		}
		assertEquals("No errors", 0, errors.size());
		
		assertNotNull(simple_driver);
	}


}
