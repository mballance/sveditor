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


package net.sf.sveditor.core.tests.scanner;

import java.io.InputStream;

import junit.framework.TestCase;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBPreProcObserver;
import net.sf.sveditor.core.db.index.SVDBFileTree;
import net.sf.sveditor.core.db.index.SVDBFileTreeUtils;
import net.sf.sveditor.core.scanner.FileContextSearchMacroProvider;
import net.sf.sveditor.core.scanner.SVPreProcDefineProvider;
import net.sf.sveditor.core.scanner.SVPreProcScanner;

import org.apache.tools.ant.filters.StringInputStream;

public class PreProcMacroTests extends TestCase {
	
	public void testMultiTokenGlue() {
		String text = 
			"`define analysis_closure_imp(data_type, target, func) \\\n" +
			"typedef class target; \\\n" +
			"class analysis_closure_``data_type``_``target``_``func \\\n" +
			"	extends ovm_component; \\\n" +
			"	ovm_analysis_imp #(data_type, \\\n" + 
			"		analysis_closure_``data_type``_``target``_``func) exp; \\\n" +
			"	\\\n" +
			"	target							m_target_ptr; \\\n" +
			"	\\\n" +
			"	function new(string name, target t); \\\n" +
			"		super.new(name, t); \\\n" +
			"		m_target_ptr = t; \\\n" +
			"		exp = new(\"exp\", this); \\\n" +
			"	endfunction \\\n" +
			"	\\\n" +
			"	virtual function void write(data_type t); \\\n" +
			"		m_target_ptr. func (t); \\\n" +
			"	endfunction \\\n" +
			"endclass\n" +
			"\n" +
			"\n" +
			"`analysis_closure_imp(foo, bar, write_func)\n" +
			"\n";
			
		InputStream in = new StringInputStream(text);
		SVPreProcScanner 	sc = new SVPreProcScanner();
		SVDBPreProcObserver ob = new SVDBPreProcObserver();

		sc.init(in, "text");
		sc.setObserver(ob);
		sc.scan();

		SVDBFile pp_file = ob.getFiles().get(0);
		SVDBFileTree ft_root = new SVDBFileTree((SVDBFile)pp_file.duplicate());

		SVDBFileTreeUtils	ft_utils = new SVDBFileTreeUtils();
		FileContextSearchMacroProvider mp = new FileContextSearchMacroProvider();
		SVPreProcDefineProvider		dp = new SVPreProcDefineProvider(mp);
		mp.setFileContext(ft_root);
		
		ft_utils.resolveConditionals(ft_root, dp);
		
		
		String result = dp.expandMacro("`analysis_closure_imp(foo, bar, write_func)", "text", 1);
		
		System.out.println("result: \"" + result + "\"");
	}

}
