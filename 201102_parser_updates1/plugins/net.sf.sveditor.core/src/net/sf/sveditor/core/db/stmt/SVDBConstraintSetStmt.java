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


package net.sf.sveditor.core.db.stmt;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.expr.SVExpr;
import net.sf.sveditor.core.db.expr.SVExprType;

public class SVDBConstraintSetStmt extends SVDBStmt {
	private List<SVDBStmt>				fConstraintList;
	
	public SVDBConstraintSetStmt() {
		super(SVDBItemType.ConstraintSetStmt);
		fConstraintList = new ArrayList<SVDBStmt>();
	}
	
	public List<SVDBStmt> getConstraintList() {
		return fConstraintList;
	}
	
	public void addConstraintStmt(SVDBStmt stmt) {
		fConstraintList.add(stmt);
	}
	
	public SVDBConstraintSetStmt duplicate() {
		SVDBConstraintSetStmt ret = new SVDBConstraintSetStmt();
		
		for (SVDBStmt e : fConstraintList) {
			ret.fConstraintList.add(e.duplicate());
		}
		
		return ret;
	}

}
