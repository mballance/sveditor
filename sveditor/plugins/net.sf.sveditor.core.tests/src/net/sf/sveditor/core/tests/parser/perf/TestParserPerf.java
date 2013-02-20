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


package net.sf.sveditor.core.tests.parser.perf;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;
import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.db.ISVDBChildItem;
import net.sf.sveditor.core.db.ISVDBChildParent;
import net.sf.sveditor.core.db.ISVDBScopeItem;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.SVDBPackageDecl;
import net.sf.sveditor.core.db.index.ISVDBIndex;
import net.sf.sveditor.core.db.index.SVDBArgFileIndex;
import net.sf.sveditor.core.db.index.SVDBArgFileIndex2;
import net.sf.sveditor.core.db.index.SVDBArgFileIndexFactory;
import net.sf.sveditor.core.db.index.SVDBFSFileSystemProvider;
import net.sf.sveditor.core.db.index.SVDBIndexRegistry;
import net.sf.sveditor.core.db.index.SVDBLibPathIndexFactory;
import net.sf.sveditor.core.log.ILogLevel;
import net.sf.sveditor.core.tests.TestIndexCacheFactory;
import net.sf.sveditor.core.tests.utils.TestUtils;

import org.eclipse.core.runtime.NullProgressMonitor;

public class TestParserPerf extends TestCase {
	
	private File				fTmpDir;
	
	@Override
	protected void setUp() throws Exception {
		fTmpDir = TestUtils.createTempDir();
//		SVCorePlugin.testInit(); 
	}



	@Override
	protected void tearDown() throws Exception {
//		SVCorePlugin.getDefault().getSVDBIndexRegistry().save_state();
		TestUtils.delete(fTmpDir);
		// TODO Auto-generated method stub
		super.tearDown();
	}



	public void testXBusExample() {
		/*
		LogFactory.getDefault().addLogListener(new ILogListener() {
			
			public void message(ILogHandle handle, int type, int level, String message) {
				System.out.println("[MSG] " + message);
			}
		});
		 */
		
		String cls_path = "net/sf/sveditor/core/tests/CoreReleaseTests.class";
		URL plugin_class = getClass().getClassLoader().getResource(cls_path);
		System.out.println("plugin_class: " + plugin_class.toExternalForm());
		String path = plugin_class.toExternalForm();
		path = path.substring("file:".length());
		path = path.substring(0, path.length()-(cls_path.length()+"/class/".length()));
		
		String ovm_dir = path + "/ovm";

//		SVCorePlugin.getDefault().enableDebug(false);
//		BundleUtils utils = new BundleUtils(SVCoreTestsPlugin.getDefault().getBundle());
		
		String xbus = ovm_dir + "/examples/xbus";

		SVDBIndexRegistry rgy = new SVDBIndexRegistry(true);
		SVDBArgFileIndexFactory factory = new SVDBArgFileIndexFactory();
//		rgy.test_init(TestIndexCacheFactory.instance(null));
		rgy.test_init(TestIndexCacheFactory.instance(fTmpDir));
		
		String compile_questa_sv = xbus + "/examples/compile_questa_sv.f";
		System.out.println("compile_questa_sv=" + compile_questa_sv);
		
		ISVDBIndex index = rgy.findCreateIndex("GENERIC",
				compile_questa_sv, SVDBArgFileIndexFactory.TYPE, factory, null);
		
		// ISVDBItemIterator it = index.getItemIterator(new NullProgressMonitor());
		index.loadIndex(new NullProgressMonitor());
		/*
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
			System.out.println("[ERROR] " + m.getMessage());
		}
		assertEquals("No errors", 0, errors.size());
		 */
	}

	public void testUVMPreProc() {
		/*
		LogFactory.getDefault().addLogListener(new ILogListener() {
			
			public void message(ILogHandle handle, int type, int level, String message) {
				System.out.println("[MSG] " + message);
			}
		});
		 */
//		LogFactory.getDefault().setLogLevel(null, 10);
		
		String cls_path = "net/sf/sveditor/core/tests/CoreReleaseTests.class";
		URL plugin_class = getClass().getClassLoader().getResource(cls_path);
		System.out.println("plugin_class: " + plugin_class.toExternalForm());
		String path = plugin_class.toExternalForm();
		path = path.substring("file:".length());
		path = path.substring(0, path.length()-(cls_path.length()+"/class/".length()));
		
		File uvm_zip = new File(new File(path), "/uvm.zip");

//		SVCorePlugin.getDefault().enableDebug(false);
//		BundleUtils utils = new BundleUtils(SVCoreTestsPlugin.getDefault().getBundle());

		TestUtils.unpackZipToFS(uvm_zip, fTmpDir);

		SVDBIndexRegistry rgy = new SVDBIndexRegistry(true);
		SVDBLibPathIndexFactory factory = new SVDBLibPathIndexFactory();
//		rgy.test_init(TestIndexCacheFactory.instance(null));
		rgy.test_init(TestIndexCacheFactory.instance(fTmpDir));
	
		File uvm = new File(fTmpDir, "uvm");
		File uvm_pkg = new File(uvm, "src/uvm_pkg.sv");
		
		System.out.println("uvm_pkg: " + uvm_pkg.getAbsolutePath());

		ISVDBIndex index = rgy.findCreateIndex("GENERIC",
				uvm_pkg.getAbsolutePath(), 
				SVDBLibPathIndexFactory.TYPE, 
				factory, 
				null);
		
		// ISVDBItemIterator it = index.getItemIterator(new NullProgressMonitor());
		long fullparse_start = System.currentTimeMillis();
		index.loadIndex(new NullProgressMonitor());
		long fullparse_end = System.currentTimeMillis();
		
		System.out.println("Full parse: " + (fullparse_end-fullparse_start));
//		System.out.println("Files: " + index.getFileList(new NullProgressMonitor()).size());
		/*
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
			System.out.println("[ERROR] " + m.getMessage());
		}
		assertEquals("No errors", 0, errors.size());
		 */
	}	

	public void testManyIfdefs() {
	
		SVCorePlugin.testInit();
		SVCorePlugin.getDefault().setDebugLevel(ILogLevel.LEVEL_MID);
		
		String cls_path = "net/sf/sveditor/core/tests/CoreReleaseTests.class";
		URL plugin_class = getClass().getClassLoader().getResource(cls_path);
		System.out.println("plugin_class: " + plugin_class.toExternalForm());
		String path = plugin_class.toExternalForm();
		path = path.substring("file:".length());
		path = path.substring(0, path.length()-(cls_path.length()+"/class/".length()));
		
		File proj_zip = new File(new File(path), "/data/performance/many_ifdefs/ProjectIncdir.zip");

		TestUtils.unpackZipToFS(proj_zip, fTmpDir);

		SVDBIndexRegistry rgy = new SVDBIndexRegistry(true);
		SVDBArgFileIndexFactory factory = new SVDBArgFileIndexFactory();
		rgy.test_init(TestIndexCacheFactory.instance(fTmpDir));
	
		File project_incdir = new File(fTmpDir, "ProjectIncdir");
		File project_incdir_f = new File(project_incdir, "ProjectIncdir.f");
		
		ISVDBIndex index = rgy.findCreateIndex("GENERIC",
				project_incdir_f.getAbsolutePath(), 
				SVDBArgFileIndexFactory.TYPE,
				factory, 
				null);
		
		long fullparse_start = System.currentTimeMillis();
		index.loadIndex(new NullProgressMonitor());
		long fullparse_end = System.currentTimeMillis();
		
		System.out.println("Full parse: " + (fullparse_end-fullparse_start));
	}
	
	public void testOpenSparc() {
		File opensparc_design = new File("/home/ballance.1/Downloads/OpenSPARCT2/design/design.f");

		SVDBIndexRegistry rgy = new SVDBIndexRegistry(true);
		SVDBArgFileIndexFactory factory = new SVDBArgFileIndexFactory();
		rgy.test_init(TestIndexCacheFactory.instance(fTmpDir));
		
		ISVDBIndex index = rgy.findCreateIndex("GENERIC",
				opensparc_design.getAbsolutePath(), 
				SVDBArgFileIndexFactory.TYPE, factory, null);
		
		// ISVDBItemIterator it = index.getItemIterator(new NullProgressMonitor());
		long fullparse_start = System.currentTimeMillis();
		index.loadIndex(new NullProgressMonitor());
		long fullparse_end = System.currentTimeMillis();
		System.out.println("Full parse: " + (fullparse_end-fullparse_start));
//		System.out.println("Files: " + index.getFileList(new NullProgressMonitor()).size());
	}

	public void testOpenSparc2() {
		File opensparc_design = new File("/home/ballance.1/Downloads/OpenSPARCT2/design/design.f");

		TestIndexCacheFactory cache_f = TestIndexCacheFactory.instance(fTmpDir);
		
		ISVDBIndex index = new SVDBArgFileIndex2("GENERIC", 
				opensparc_design.getAbsolutePath(),
				new SVDBFSFileSystemProvider(),
				cache_f.createIndexCache("GENERIC", opensparc_design.getAbsolutePath()),
				null);
		index.init(new NullProgressMonitor());
				
		long fullparse_start = System.currentTimeMillis();
		index.loadIndex(new NullProgressMonitor());
		long fullparse_end = System.currentTimeMillis();
		System.out.println("Full parse: " + (fullparse_end-fullparse_start));
		
		Iterable<String> files = index.getFileList(new NullProgressMonitor());
	
		/*
		for (String f : files) {
			System.out.println("File: " + f);
			SVDBFile p = index.findFile(f);
			traverse_files(p, -1);
//			break;
		}
		 */
	}

	public void testUVM2() {
		File opensparc_design = new File("/tools/uvm/uvm-1.1a/uvm.f");

		TestIndexCacheFactory cache_f = TestIndexCacheFactory.instance(fTmpDir);
		
		ISVDBIndex index = new SVDBArgFileIndex2("GENERIC", 
				opensparc_design.getAbsolutePath(),
				new SVDBFSFileSystemProvider(),
				cache_f.createIndexCache("GENERIC", opensparc_design.getAbsolutePath()),
				null);
		index.init(new NullProgressMonitor());
				
		long fullparse_start = System.currentTimeMillis();
		index.loadIndex(new NullProgressMonitor());
		long fullparse_end = System.currentTimeMillis();
		System.out.println("Full parse: " + (fullparse_end-fullparse_start));
		
		Iterable<String> files = index.getFileList(new NullProgressMonitor());
		
		for (String f : files) {
			System.out.println("File: " + f);
			SVDBFile p = index.findFile(f);
			traverse_files(p, -1);
//			break;
		}
	}
	private void traverse_files(ISVDBChildParent p, int file_id) {
		for (ISVDBChildItem c : p.getChildren()) {
			System.out.println("Item: " + SVDBItem.getName(c));
			if (c.getLocation() != null && c.getLocation().getFileId() != file_id) {
				System.out.println("Switch to file: " + c.getLocation().getFileId());
				file_id = c.getLocation().getFileId();
			}

			if (c instanceof ISVDBChildParent) {
				traverse_files((ISVDBChildParent)c, file_id);
			} else if (c instanceof ISVDBScopeItem) {
				
			}
		}
		
	}

	public void testLargeParam() {
		File opensparc_design = new File("/home/ballance/Downloads/sz/Project_complicated_include/top_dir/files.f");

		TestIndexCacheFactory cache_f = TestIndexCacheFactory.instance(fTmpDir);
		
		ISVDBIndex index = new SVDBArgFileIndex("GENERIC", 
				opensparc_design.getAbsolutePath(),
				new SVDBFSFileSystemProvider(),
				cache_f.createIndexCache("GENERIC", opensparc_design.getAbsolutePath()),
				null);
		index.init(new NullProgressMonitor());
				
		long fullparse_start = System.currentTimeMillis();
		index.loadIndex(new NullProgressMonitor());
		long fullparse_end = System.currentTimeMillis();
		System.out.println("Full parse: " + (fullparse_end-fullparse_start));
	}
	
	public void testLargeParam2() {
		File opensparc_design = new File("/home/ballance/Downloads/sz/Project_complicated_include/top_dir/files.f");

		TestIndexCacheFactory cache_f = TestIndexCacheFactory.instance(fTmpDir);
		
		ISVDBIndex index = new SVDBArgFileIndex2("GENERIC", 
				opensparc_design.getAbsolutePath(),
				new SVDBFSFileSystemProvider(),
				cache_f.createIndexCache("GENERIC", opensparc_design.getAbsolutePath()),
				null);
		index.init(new NullProgressMonitor());
				
		long fullparse_start = System.currentTimeMillis();
		index.loadIndex(new NullProgressMonitor());
		long fullparse_end = System.currentTimeMillis();
		System.out.println("Full parse: " + (fullparse_end-fullparse_start));
	}	
}
