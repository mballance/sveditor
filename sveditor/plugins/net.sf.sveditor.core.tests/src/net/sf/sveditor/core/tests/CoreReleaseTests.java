/****************************************************************************
 * Copyright (c) 2008-2014 Matthew Ballance and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Ballance - initial implementation
 ****************************************************************************/


package net.sf.sveditor.core.tests;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.log.ILogHandle;
import net.sf.sveditor.core.log.ILogListener;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.tests.argfile.content_assist.ArgFileContentAssistTests;
import net.sf.sveditor.core.tests.argfile.creator.TestArgFileCreator;
import net.sf.sveditor.core.tests.argfile.open_decl.ArgFileOpenDeclTests;
import net.sf.sveditor.core.tests.argfile.parser.ArgFileParserTests;
import net.sf.sveditor.core.tests.content_assist.ContentAssistTests;
import net.sf.sveditor.core.tests.db.SVDBTests;
import net.sf.sveditor.core.tests.docs.DocsTests;
import net.sf.sveditor.core.tests.fileset.FileSetTests;
import net.sf.sveditor.core.tests.hierarchy.HierarchyTests;
import net.sf.sveditor.core.tests.indent.IndentTests;
import net.sf.sveditor.core.tests.index.IndexTests;
import net.sf.sveditor.core.tests.index.argfile2.ArgFileIndex2Tests;
import net.sf.sveditor.core.tests.index.cache.IndexCacheTests;
import net.sf.sveditor.core.tests.index.persistence.PersistenceTests;
import net.sf.sveditor.core.tests.index.refs.IndexRefTests;
import net.sf.sveditor.core.tests.job_mgr.JobMgrTests;
import net.sf.sveditor.core.tests.logscanner.LogScannerTests;
import net.sf.sveditor.core.tests.open_decl.OpenDeclTests;
import net.sf.sveditor.core.tests.parser.ParserTests;
import net.sf.sveditor.core.tests.parser.ams.ParserAMSTests;
import net.sf.sveditor.core.tests.parser.db.ParserDBTests;
import net.sf.sveditor.core.tests.preproc.PreProcTests;
import net.sf.sveditor.core.tests.primitives.PrimitivesTests;
import net.sf.sveditor.core.tests.project_settings.ProjectSettingsTests;
import net.sf.sveditor.core.tests.scanner.PreProcMacroTests;
import net.sf.sveditor.core.tests.searchutils.SearchUtilsTests;
import net.sf.sveditor.core.tests.srcgen.SrcGenTests;

public class CoreReleaseTests extends TestSuite {
	
	private static List<Exception>		fErrors = new ArrayList<Exception>();
	private static ILogListener		fErrorLogListener;
	
	static {
		fErrorLogListener = new ILogListener() {
			public void message(ILogHandle handle, int type, int level, String message) {
				if (type == ILogListener.Type_Error) {
//					System.out.println("MSG: " + message);
					try {
						throw new Exception("[" + handle.getName() + "] " + message);
					} catch (Exception e) {
						fErrors.add(e);
					}
				}
			}
		};
		LogFactory.getDefault().addLogListener(fErrorLogListener);
	}
	
	public CoreReleaseTests() {
		addTest(ArgFileContentAssistTests.suite());
		addTest(ArgFileIndex2Tests.suite());
		addTest(new TestSuite(TestArgFileCreator.class));
		addTest(ArgFileOpenDeclTests.suite());
		addTest(ArgFileParserTests.suite());
		addTest(new TestSuite(SVScannerTests.class));
		addTest(ParserTests.suite());
		addTest(ParserAMSTests.suite());
		addTest(ParserDBTests.suite());
		addTest(new TestSuite(PreProcMacroTests.class));
		addTest(PreProcTests.suite());
		addTest(IndentTests.suite());
		addTest(IndexRefTests.suite());
		addTest(JobMgrTests.suite());
		addTest(ContentAssistTests.suite());
		addTest(PersistenceTests.suite());
		addTest(ProjectSettingsTests.suite());
		addTest(IndexTests.suite());
		addTest(IndexCacheTests.suite());
		addTest(SrcGenTests.suite());
		addTest(LogScannerTests.suite());
		addTest(OpenDeclTests.suite());
		addTest(PrimitivesTests.suite());
		addTest(new TestSuite(FileSetTests.class));
		addTest(SearchUtilsTests.suite());
		addTest(SVDBTests.suite());
//		addTest(SearchTests.suite());
		addTest(HierarchyTests.suite());
		addTest(DocsTests.suite());
		
	}
	
	public static List<Exception> getErrors() {
		return fErrors;
	}
	
	public static void clearErrors() {
		fErrors.clear();
	}
	
	
	@Override
	public void run(TestResult result) {
		SVCorePlugin.getDefault().enableDebug(false);
		// TODO Auto-generated method stub
		super.run(result);
	}
	
	@Override
	public void runTest(Test test, TestResult result) {
		SVCorePlugin.getDefault().enableDebug(false);
		super.runTest(test, result);
	}



	public static Test suite() {
		
		/*
		TestSuite suite = new TestSuite();
		suite.addTest(new TestSuite(SVScannerTests.class));
		suite.addTest(IndentTests.suite());
		suite.addTest(ContentAssistTests.suite());
		suite.addTest(PersistenceTests.suite());
		suite.addTest(IndexTests.suite());
		
		return suite;
		 */
		return new CoreReleaseTests();
	}
	
}