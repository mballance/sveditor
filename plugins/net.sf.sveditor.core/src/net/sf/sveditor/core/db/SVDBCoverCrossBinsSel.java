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


package net.sf.sveditor.core.db;

import net.sf.sveditor.core.db.expr.SVDBExpr;
import net.sf.sveditor.core.db.expr.SVDBIdentifierExpr;

public class SVDBCoverCrossBinsSel extends SVDBItem {
	
	public SVDBExpr				fSelectExpr;
	
	public SVDBCoverCrossBinsSel() {
		super("", SVDBItemType.CoverCrossBinsSel);
	}
	
	public SVDBCoverCrossBinsSel(SVDBIdentifierExpr id) {
		super(id, SVDBItemType.CoverCrossBinsSel);
	}
	
	public void setSelectExpr(SVDBExpr expr) {
		fSelectExpr = expr;
	}
	
	public SVDBExpr getSelectExpr() {
		return fSelectExpr;
	}

}
