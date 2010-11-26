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


package net.sf.sveditor.core.tests.constraint_parser;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.StringInputStream;
import net.sf.sveditor.core.db.ISVDBFileFactory;
import net.sf.sveditor.core.db.ISVDBScopeItem;
import net.sf.sveditor.core.db.SVDBConstraint;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.expr.parser.SVExpr;
import net.sf.sveditor.core.expr.parser.SVExprDump;
import net.sf.sveditor.core.expr.parser.SVExprParseException;
import net.sf.sveditor.core.parser.SVExprParser;
import net.sf.sveditor.core.scanutils.InputStreamTextScanner;
import net.sf.sveditor.core.scanutils.StringTextScanner;

public class SmokeTest extends TestCase {
	
	public void testBasics() {
		SVExprParser p = new SVExprParser();
		String constraint = "if (a == 5) {b inside {6, 7, [8:10]}; c == 7;} else if (b == 6) { c == 8 ; d == 10;}";
		StringInputStream in = new StringInputStream(constraint);
		SVExprDump dump = new SVExprDump(System.out);
		List<SVExpr> expr_l = null;
		
		try {
			expr_l = p.parse_constraint(new InputStreamTextScanner(in, ""));
		} catch (SVExprParseException e) {
			e.printStackTrace();
		}
		
	}
	
	public void testReal() {
		ISVDBFileFactory factory = SVCorePlugin.getDefault().createFileFactory(null);
		SVDBFile file = null;
		InputStream in = null;
		List<SVDBConstraint>	constraints = new ArrayList<SVDBConstraint>();
		List<List<SVExpr>> constraint_expr = new ArrayList<List<SVExpr>>();
		SVExprParser p = new SVExprParser();
		
		try {
			in = new FileInputStream("/home/ballance/try.svh");
			file = factory.parse(in, "try.svh");
		} catch (Exception e) {
			e.printStackTrace();
		}

		find_constraints(file, constraints);
		
		System.out.println("There are " + constraints.size() + " constraints");
		
		for (SVDBConstraint c : constraints) {
			System.out.println("[CONSTRAINT] " + c.getConstraintExpr());
			try {
				constraint_expr.add(p.parse_constraint(
						new StringTextScanner(c.getConstraintExpr())));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
	public static void find_constraints(ISVDBScopeItem scope, List<SVDBConstraint> constraints) {
		for (SVDBItem it : scope.getItems()) {
			if (it.getType() == SVDBItemType.Constraint) {
				constraints.add((SVDBConstraint)it);
			} else if (it instanceof ISVDBScopeItem) {
				find_constraints((ISVDBScopeItem)it, constraints);
			}
		}
	}

}
