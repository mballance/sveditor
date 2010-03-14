package net.sf.sveditor.core.tests.index;

import java.io.File;

import org.eclipse.core.resources.IProject;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.Tuple;
import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.index.ISVDBIndex;
import net.sf.sveditor.core.db.index.ISVDBItemIterator;
import net.sf.sveditor.core.db.index.SVDBArgFileIndexFactory;
import net.sf.sveditor.core.db.index.SVDBIndexRegistry;
import net.sf.sveditor.core.db.index.SVDBLibPathIndexFactory;
import net.sf.sveditor.core.db.index.SVDBSourceCollectionIndexFactory;
import net.sf.sveditor.core.db.project.SVDBProjectData;
import net.sf.sveditor.core.db.project.SVDBProjectManager;
import net.sf.sveditor.core.db.project.SVProjectFileWrapper;
import net.sf.sveditor.core.tests.Activator;
import net.sf.sveditor.core.tests.utils.BundleUtils;
import net.sf.sveditor.core.tests.utils.TestUtils;
import junit.framework.TestCase;

public class TestGlobalDefine extends TestCase {

	private File					fTmpDir;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		fTmpDir = TestUtils.createTempDir();
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		
		if (fTmpDir != null) {
			fTmpDir.delete();
			fTmpDir = null;
		}
	}

	public void testLibIndexGlobalDefine() {
		BundleUtils utils = new BundleUtils(Activator.getDefault().getBundle());
		
		IProject project_dir = TestUtils.createProject("project");
		
		utils.copyBundleDirToWS("/data/basic_lib_global_defs/", project_dir);
		
		File db = new File(fTmpDir, "db");
		if (db.exists()) {
			db.delete();
		}
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		rgy.init(db);
		SVCorePlugin.getDefault().getProjMgr().init();
		
		ISVDBIndex index = rgy.findCreateIndex("project", 
				"${workspace_loc}/project/basic_lib_global_defs/basic_lib_pkg.sv", 
				SVDBLibPathIndexFactory.TYPE, null);
		SVDBProjectManager p_mgr = SVCorePlugin.getDefault().getProjMgr();
		SVDBProjectData p_data = p_mgr.getProjectData(project_dir);
	
		try {
			int_testGlobalDefine(p_data, index);
		} finally {
			TestUtils.deleteProject(project_dir);
		}
	}

	public void testArgFileIndexGlobalDefine() {
		SVCorePlugin.getDefault().enableDebug(true);
		BundleUtils utils = new BundleUtils(Activator.getDefault().getBundle());
		
		IProject project_dir = TestUtils.createProject("project");
		
		utils.copyBundleDirToWS("/data/basic_lib_global_defs/", project_dir);
		
		File db = new File(fTmpDir, "db");
		if (db.exists()) {
			db.delete();
		}
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		rgy.init(db);
		SVCorePlugin.getDefault().getProjMgr().init();
		
		ISVDBIndex index = rgy.findCreateIndex("project", 
				"${workspace_loc}/project/basic_lib_global_defs/proj.f", 
				SVDBArgFileIndexFactory.TYPE, null);
		SVDBProjectManager p_mgr = SVCorePlugin.getDefault().getProjMgr();
		SVDBProjectData p_data = p_mgr.getProjectData(project_dir);
		
		try {
			int_testGlobalDefine(p_data, index);
		} finally {
			TestUtils.deleteProject(project_dir);
		}
	}

	public void testSourceCollectionIndexGlobalDefine() {
		SVCorePlugin.getDefault().enableDebug(true);
		BundleUtils utils = new BundleUtils(Activator.getDefault().getBundle());
		
		IProject project_dir = TestUtils.createProject("project");
		
		utils.copyBundleDirToWS("/data/basic_lib_global_defs/", project_dir);
		
		File db = new File(fTmpDir, "db");
		if (db.exists()) {
			db.delete();
		}
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		rgy.init(db);
		SVCorePlugin.getDefault().getProjMgr().init();
		
		ISVDBIndex index = rgy.findCreateIndex("project", 
				"${workspace_loc}/project/basic_lib_global_defs",
				SVDBSourceCollectionIndexFactory.TYPE, null);
		SVDBProjectManager p_mgr = SVCorePlugin.getDefault().getProjMgr();
		SVDBProjectData p_data = p_mgr.getProjectData(project_dir);
		
		try {
			int_testGlobalDefine(p_data, index);
		} finally {
			TestUtils.deleteProject(project_dir);
		}
	}

	private void int_testGlobalDefine(
			SVDBProjectData				project_data,
			ISVDBIndex					index) {
		// Add the definition that we'll check for
		SVProjectFileWrapper p_wrap = project_data.getProjectFileWrapper().duplicate();
		p_wrap.getGlobalDefines().add(new Tuple<String, String>("ENABLE_CLASS1", ""));
		project_data.setProjectFileWrapper(p_wrap);
		
		p_wrap = project_data.getProjectFileWrapper();
		assertEquals("Check that define not forgotten", 1, p_wrap.getGlobalDefines().size());
		
		ISVDBItemIterator<SVDBItem> index_it = index.getItemIterator();
		
		SVDBItem class1_it = null;
		while (index_it.hasNext()) {
			SVDBItem it_tmp = index_it.nextItem();
			if (it_tmp.getName().equals("class1")) {
				class1_it = it_tmp;
			}
		}
		
		assertNotNull("Expect to find \"class1\"", class1_it);
	}
			
}
