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


package net.sf.sveditor.ui.tests.explorer;

import java.io.File;
import java.util.List;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.index.ISVDBIndex;
import net.sf.sveditor.core.db.index.SVDBIndexCollectionMgr;
import net.sf.sveditor.core.db.index.SVDBIndexRegistry;
import net.sf.sveditor.core.db.index.plugin_lib.SVDBPluginLibIndexFactory;
import net.sf.sveditor.core.tests.SVCoreTestsPlugin;
import net.sf.sveditor.core.tests.content_assist.ContentAssistIndex;
import net.sf.sveditor.core.tests.utils.BundleUtils;
import net.sf.sveditor.core.tests.utils.TestUtils;
import net.sf.sveditor.ui.explorer.PathTreeNode;
import net.sf.sveditor.ui.explorer.PathTreeNodeFactory;
import junit.framework.TestCase;

public class TestExplorer extends TestCase {
	private File 						fTmpDir;
	private SVDBIndexCollectionMgr		fIndexCollectionOVMMgr;
	
	@Override
	public void setUp() {
		fTmpDir = TestUtils.createTempDir();
		BundleUtils utils = new BundleUtils(SVCoreTestsPlugin.getDefault().getBundle());
		
		utils.copyBundleDirToFS("/data/basic_content_assist_project", fTmpDir);

		String pname = "basic_content_assist_project";
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		fIndexCollectionOVMMgr = new SVDBIndexCollectionMgr(pname);
		fIndexCollectionOVMMgr.addPluginLibrary(
				rgy.findCreateIndex(pname, SVCoreTestsPlugin.OVM_LIBRARY_ID, 
						SVDBPluginLibIndexFactory.TYPE, null));

		// Force database loading
		fIndexCollectionOVMMgr.getItemIterator();
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		
		fTmpDir.delete();
	}
	
	public void testPathTreeNodeFactory() {
		PathTreeNodeFactory f = new PathTreeNodeFactory();
		
		List<ISVDBIndex> l = fIndexCollectionOVMMgr.getPluginPathList();
		
		for (ISVDBIndex i : l) {
			List<PathTreeNode> roots = f.build(i.getPreProcFileMap().values());
			
			for (PathTreeNode n : roots) {
				System.out.println("root: " + n.getName());
				printChildren("    ", n);
			}
		}
	}
	
	private void printChildren(String indent, PathTreeNode n) {
		System.out.println(indent + "Node: " + n.getName());
		for (SVDBFile f : n.getFileList()) {
			System.out.println(indent + "    File: " + f.getName());
		}
		for (PathTreeNode n_t : n.getChildNodes()) {
			printChildren(indent + "    ", n_t);
		}
	}

}
