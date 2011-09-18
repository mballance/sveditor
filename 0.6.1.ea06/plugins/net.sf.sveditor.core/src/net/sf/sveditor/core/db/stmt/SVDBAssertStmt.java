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


package net.sf.sveditor.core.db.stmt;

import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.expr.SVDBExpr;

public class SVDBAssertStmt extends SVDBStmt {
	private SVDBExpr				fExpr;
	private SVDBActionBlockStmt		fActionBlock;
	
	public SVDBAssertStmt() {
		this(SVDBItemType.AssertStmt);
	}
	
	protected SVDBAssertStmt(SVDBItemType type) {
		super(type);
	}
	
	public void setExpr(SVDBExpr expr) {
		fExpr = expr;
	}
	
	public SVDBExpr getExpr() {
		return fExpr;
	}
	
	public void setActionBlock(SVDBActionBlockStmt stmt) {
		fActionBlock = stmt;
	}
	
	public SVDBActionBlockStmt getActionBlock() {
		return fActionBlock;
	}

}
