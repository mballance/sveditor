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


/**
 * @author ballance
 * The DB package contains classes that represent the SystemVerilog AST
 */
package net.sf.sveditor.core.db;

import net.sf.sveditor.core.db.expr.SVExpr;

public class SVDB {
	private static boolean			fInit;
	
	public static void init() {
		if (fInit) {
			return;
		}
		
		SVDBAlwaysBlock.init();
		SVDBAssign.init();
		SVDBConstraint.init();
		SVDBCoverGroup.init();
		SVDBCoverPoint.init();
		SVDBCoverpointCross.init();
		SVDBFile.init();
		SVDBInclude.init();
		SVDBInitialBlock.init();
		SVDBMacroDef.init();
		SVDBMarkerItem.init();
		SVDBModIfcClassDecl.init();
		SVDBModIfcClassParam.init();
		SVDBModIfcInstItem.init();
		SVDBPackageDecl.init();
		SVDBPreProcCond.init();
		SVDBScopeItem.init();
		SVDBParamPort.init();
		SVDBTaskFuncScope.init();
		SVDBTypedef.init();
		SVDBTypeInfo.init();
		SVDBVarDeclItem.init();
		SVDBParamValueAssignList.init();
		SVDBParamValueAssign.init();
		SVDBGenerateBlock.init();
		SVDBClockingBlock.init();
		SVDBImport.init();
		SVExpr.init();

		fInit = true;
	}

}
