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

import java.io.InputStream;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.SVFileUtils;
import net.sf.sveditor.core.Tuple;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.index.ISVDBIndex;
import net.sf.sveditor.core.db.index.SVDBIndexRegistry;
import net.sf.sveditor.core.db.index.argfile.SVDBArgFileIndexFactory;
import net.sf.sveditor.core.db.index.old.SVDBLibPathIndexFactory;
import net.sf.sveditor.core.db.index.old.SVDBSourceCollectionIndexFactory;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;
import net.sf.sveditor.core.tests.IndexTestUtils;
import net.sf.sveditor.core.tests.SVCoreTestCaseBase;
import net.sf.sveditor.core.tests.SVCoreTestsPlugin;
import net.sf.sveditor.core.tests.utils.BundleUtils;
import net.sf.sveditor.core.tests.utils.TestUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

/**
 * These tests exercise the ISVDBIndex.parse() method to ensure that
 * the index can parse a file that exists within the index
 * 
 * @author ballance
 *
 */
public class TestIndexParse extends SVCoreTestCaseBase {
	
	private BundleUtils				fUtils;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		fUtils = new BundleUtils(SVCoreTestsPlugin.getDefault().getBundle());
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		rgy.init(fCacheFactory);
	}
	
	@Override
	protected void tearDown() throws Exception {
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		rgy.close();
		
		super.tearDown();
	}
	
	public void testWSLibIndexParse() {
		IProject project_dir = TestUtils.createProject("project");
		
		fUtils.copyBundleDirToWS("/project_dir/", project_dir);
		
		SVCorePlugin.getDefault().getProjMgr().init();
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		ISVDBIndex index = rgy.findCreateIndex(new NullProgressMonitor(), "project", 
				"${workspace_loc}/project/project_dir/basic_lib_pkg.sv", 
				SVDBLibPathIndexFactory.TYPE, null);

		String path = "${workspace_loc}" +
			project_dir.getFile(new Path("project_dir/class1.svh")).getFullPath().toOSString();
		
		try {
			int_testIndexParse(index, path);
		} finally {
			TestUtils.deleteProject(project_dir);
		}
	}

	public void testWSArgFileIndexParse() {
		IProject project_dir = TestUtils.createProject("project");
		
		fUtils.copyBundleDirToWS("/project_dir/", project_dir);
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		SVCorePlugin.getDefault().getProjMgr().init();
		
		ISVDBIndex index = rgy.findCreateIndex(new NullProgressMonitor(), "project", 
				"${workspace_loc}/project/project_dir/testbench.f", 
				SVDBArgFileIndexFactory.TYPE, null);

		String path = "${workspace_loc}" +
			project_dir.getFile(new Path("project_dir/class1.svh")).getFullPath().toOSString();
		
		try {
			int_testIndexParse(index, path);
		} finally {
			TestUtils.deleteProject(project_dir);
		}
	}

	public void testWSSourceCollectionIndexParse() {
		IProject project_dir = TestUtils.createProject("project");
		
		fUtils.copyBundleDirToWS("/project_dir/", project_dir);
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		SVCorePlugin.getDefault().getProjMgr().init();
		
		ISVDBIndex index = rgy.findCreateIndex(new NullProgressMonitor(), "project", 
				"${workspace_loc}/project/project_dir", 
				SVDBSourceCollectionIndexFactory.TYPE, null);

		String path = "${workspace_loc}" +
			project_dir.getFile(new Path("project_dir/class1.svh")).getFullPath().toOSString();
		
		try {
			int_testIndexParse(index, path);
		} finally {
			TestUtils.deleteProject(project_dir);
		}
	}

	private void int_testIndexParse(ISVDBIndex index, String path) {
		String path_n = SVFileUtils.normalize(path);
		
		InputStream in = index.getFileSystemProvider().openStream(path);
		
		assertNotNull("Failed to open path \"" + path + "\"", in);
	
		Tuple<SVDBFile, SVDBFile> result = index.parse(new NullProgressMonitor(), in, path, null);
		
		assertNotNull(result);

		SVDBFile file = result.second();
		
		assertNotNull("Failed to parse path \"" + path + "\"", file);

		index.getFileSystemProvider().closeStream(in);

		// If the normalized path is different (ie Windows), then
		// run extra tests to ensure that either path works
		if (!path_n.equals(path)) {
			in = index.getFileSystemProvider().openStream(path_n);
			
			assertNotNull("Failed to open path \"" + path_n + "\"", in);
			
			file = index.parse(new NullProgressMonitor(), in, path_n, null).second();
			
			assertNotNull("Failed to parse path \"" + path_n + "\"", file);
			
			index.getFileSystemProvider().closeStream(in);
		}
	}

	/**
	 * This test appears to fail with the new index because
	 * index-iteration relies on findFile 
	 */
	public void testRecursiveIncludeParse() {
		String testname = getName();
		LogHandle log = LogFactory.getLogHandle(testname);
		
		SVCorePlugin.getDefault().enableDebug(false);
		
		IProject project = TestUtils.createProject("recursive_include", fTmpDir);
		addProject(project);
		
		fUtils.copyBundleDirToWS("/data/recursive_include", project);
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		rgy.init(fCacheFactory);
		
		ISVDBIndex index = rgy.findCreateIndex(new NullProgressMonitor(), 
				"recursive_include", 
				"${workspace_loc}/recursive_include/recursive_include/recursive_include.f",
				SVDBArgFileIndexFactory.TYPE, null);
		
		index.loadIndex(new NullProgressMonitor());
		
		IndexTestUtils.assertNoErrWarn(log, index);
		IndexTestUtils.assertFileHasElements(log, index, "t1", "t2");
	}
}
