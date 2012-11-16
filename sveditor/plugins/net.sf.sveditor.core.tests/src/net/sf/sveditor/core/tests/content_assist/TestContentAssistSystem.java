package net.sf.sveditor.core.tests.content_assist;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.Tuple;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.SVDBMarker;
import net.sf.sveditor.core.db.index.ISVDBIndex;
import net.sf.sveditor.core.db.index.ISVDBItemIterator;
import net.sf.sveditor.core.db.index.SVDBArgFileIndexFactory;
import net.sf.sveditor.core.db.index.SVDBFileOverrideIndex;
import net.sf.sveditor.core.db.index.SVDBIndexCollection;
import net.sf.sveditor.core.db.index.SVDBIndexRegistry;
import net.sf.sveditor.core.db.index.SVDBIndexUtil;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;
import net.sf.sveditor.core.tests.SVCoreTestsPlugin;
import net.sf.sveditor.core.tests.TestIndexCacheFactory;
import net.sf.sveditor.core.tests.utils.BundleUtils;
import net.sf.sveditor.core.tests.utils.TestUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.sun.org.apache.bcel.internal.generic.ARRAYLENGTH;

import junit.framework.TestCase;

public class TestContentAssistSystem extends TestCase {
	private File				fTmpDir;
	private IProject			fProject;
	private BundleUtils			fUtils;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		fTmpDir = TestUtils.createTempDir();
		fProject = null;
		fUtils = new BundleUtils(SVCoreTestsPlugin.getDefault().getBundle());
	}

	@Override
	protected void tearDown() throws Exception {
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		rgy.save_state();
		super.tearDown();
		
		if (fProject != null) {
			TestUtils.deleteProject(fProject);
			fProject = null;
		}
		
		if (fTmpDir != null && fTmpDir.exists()) {
			TestUtils.delete(fTmpDir);
			fTmpDir = null;
		}
	}
	
	public void testGlobalFieldRef() {
		String testname = "testGlobalFieldRef";
		LogHandle log = LogFactory.getLogHandle(testname);
		SVCorePlugin.getDefault().enableDebug(false);
		
		String doc = 
			"class c1;\n" +
			"	function void foo;\n" +
			"		field_c<<MARK>>\n" +
			"	endfunction\n" +
			"endclass\n"
			;
		
		
		fProject = TestUtils.createProject("project");
		
		fUtils.copyBundleDirToWS("/data/content_assist/global_field_ref/", fProject);
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		
		ISVDBIndex index = rgy.findCreateIndex(
				new NullProgressMonitor(), "GENERIC", 
				"${workspace_loc}/project/global_field_ref/global_field_ref.f",
				SVDBArgFileIndexFactory.TYPE, null);

		ContentAssistTests.runTest(testname, doc, index, 
				"field_cls");
		
		LogFactory.removeLogHandle(log);		
	}
	
	public void testShadowSVBuiltinProjectFile() {
		String testname = "testFindSVBuiltinProcessProject";
		LogHandle log = LogFactory.getLogHandle(testname);
		SVCorePlugin.getDefault().enableDebug(false);
		
		String doc =
			"class c1;\n" +
			"	int				my_q[$];\n" +
			"	function void foo;\n" +
			"		my_q.s<<MARK>>\n" +
			"	endfunction\n" +
			"endclass\n"
			;

		fProject = TestUtils.createProject("project");
		
		fUtils.copyBundleDirToWS("/data/content_assist/simple_proj/", fProject);
		
		TestUtils.copy(
				"class c1;\n" +
				"endclass\n",
				fProject.getFile("simple_proj/c1.svh"));

		Tuple<ISVDBIndex, SVDBIndexCollection> result = SVDBIndexUtil.findIndexFile(
				"${workspace_loc}/project/simple_proj/c1.svh", 
				"project", 
				true);
		
		assertNotNull(result);
		assertNotNull(result.first());
		assertNotNull(result.second());
		
		ContentAssistTests.runTest(testname, doc, result.second(), 
				"size");
		
		LogFactory.removeLogHandle(log);		
	}

	public void testShadowSVBuiltinNonProjectFile() {
		String testname = "testShadowSVBuiltinNonProjectFile";
		LogHandle log = LogFactory.getLogHandle(testname);
		SVCorePlugin.getDefault().enableDebug(false);
		
		String doc =
			"class c1;\n" +
			"	int				my_q[$];\n" +
			"	function void foo;\n" +
			"		my_q.s<<MARK>>\n" +
			"	endfunction\n" +
			"endclass\n"
			;

		fProject = TestUtils.createProject("project");
		
		fUtils.copyBundleDirToWS("/data/content_assist/simple_proj/", fProject);
	
		File c1 = new File(fTmpDir, "c1.svh");
		TestUtils.copy(
				"class c1;\n" +
				"endclass\n",
				c1);

		Tuple<ISVDBIndex, SVDBIndexCollection> result = SVDBIndexUtil.findIndexFile(
				c1.getAbsolutePath(),
				null,
				true);
		
		assertNotNull(result);
		assertNotNull(result.first());
		assertNotNull(result.second());
		
		List<SVDBMarker> markers = new ArrayList<SVDBMarker>();
		SVDBFile file = new SVDBFile(c1.getAbsolutePath());
		
		SVDBFileOverrideIndex index = new SVDBFileOverrideIndex(
				file, null, result.first(), result.second(), markers);
		
		ContentAssistTests.runTest(testname, doc, index, "size");
		
		LogFactory.removeLogHandle(log);		
	}
	
}
