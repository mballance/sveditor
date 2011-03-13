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
import net.sf.sveditor.core.db.ISVDBScopeItem;
import net.sf.sveditor.core.db.SVDBClassDecl;
import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBMarker;
import net.sf.sveditor.core.db.SVDBModIfcDecl;
import net.sf.sveditor.core.db.index.ISVDBItemIterator;
import net.sf.sveditor.core.db.index.SVDBIndexCollectionMgr;
import net.sf.sveditor.core.db.index.SVDBIndexRegistry;
import net.sf.sveditor.core.db.index.plugin_lib.SVDBPluginLibIndexFactory;
import net.sf.sveditor.core.db.stmt.SVDBStmt;
import net.sf.sveditor.core.db.stmt.SVDBVarDeclStmt;
import net.sf.sveditor.core.tests.utils.TestUtils;

import org.eclipse.core.runtime.NullProgressMonitor;

public class TestBuiltinIndex extends TestCase {
	File					fTmpDir;
	
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
		}
	}

	public void testBuiltinIndexNoErrors() {
		File tmpdir = new File(fTmpDir, "no_errors");
		
		if (tmpdir.exists()) {
			tmpdir.delete();
		}
		tmpdir.mkdirs();
		
		SVDBIndexRegistry rgy = SVCorePlugin.getDefault().getSVDBIndexRegistry();
		rgy.init(tmpdir);
	
		SVDBIndexCollectionMgr index_mgr = new SVDBIndexCollectionMgr("GLOBAL");
		index_mgr.addPluginLibrary(
				rgy.findCreateIndex("GLOBAL", SVCorePlugin.SV_BUILTIN_LIBRARY, 
						SVDBPluginLibIndexFactory.TYPE, null));
		
		ISVDBItemIterator index_it = index_mgr.getItemIterator(new NullProgressMonitor());
		List<SVDBMarker> markers = new ArrayList<SVDBMarker>();
		ISVDBItemBase string_cls=null, process_cls=null, covergrp_cls=null;
		ISVDBItemBase finish_task=null;
		
		while (index_it.hasNext()) {
			ISVDBItemBase it = index_it.nextItem();
			
			if (it.getType() != SVDBItemType.File) {
				assertNotNull("Item " + SVDBItem.getName(it) + " has null location",
						it.getLocation());
				if (it instanceof ISVDBScopeItem) {
					assertNotNull("Item " + SVDBItem.getName(it) + " has null end location",
							((ISVDBScopeItem)it).getEndLocation());
				}
			}
			
			if (SVDBStmt.isType(it, SVDBItemType.VarDeclStmt)) {
				assertNotNull("Item " + SVDBItem.getName(it) + " w/parent " + 
						SVDBItem.getName(((SVDBVarDeclStmt)it).getParent()) + " has null type",
					((SVDBVarDeclStmt)it).getTypeInfo());
			}
			
			if (it.getType() == SVDBItemType.Marker) {
				markers.add((SVDBMarker)it);
			} else if (it.getType() == SVDBItemType.ClassDecl) {
				String name = ((SVDBClassDecl)it).getName();
				if (name.equals("string")) {
					string_cls = it;
				} else if (name.equals("process")) {
					process_cls = it;
				} else if (name.equals("__sv_builtin_covergroup")) {
					covergrp_cls = it;
				}
			} else if (it.getType() == SVDBItemType.Task) {
				if (SVDBItem.getName(it).equals("$finish")) {
					finish_task = it;
				}
			}
		}
		
		assertEquals("Check that no errors were found", 0, markers.size());
		assertNotNull("Check found string", string_cls);
		assertNotNull("Check found process", process_cls);
		assertNotNull("Check found covergroup", covergrp_cls);
		assertNotNull("Check found $finish", finish_task);
	}

}
