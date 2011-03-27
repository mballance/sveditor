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


package net.sf.sveditor.core.db;

import net.sf.sveditor.core.db.expr.SVDBExpr;

public class SVDBCoverpoint extends SVDBScopeItem {
	private SVDBExpr				fTarget;
	private SVDBExpr				fIFF;

	public SVDBCoverpoint() {
		super("", SVDBItemType.Coverpoint);
	}
	
	public SVDBCoverpoint(String name) {
		super(name, SVDBItemType.Coverpoint);
	}
	
	public SVDBExpr getTarget() {
		return fTarget;
	}
	
	public void setTarget(SVDBExpr expr) {
		fTarget = expr;
	}
	
	public SVDBExpr getIFF() {
		return fIFF;
	}
	
	public void setIFF(SVDBExpr expr) {
		fIFF = expr;
	}
	
	@Override
	public SVDBItemBase duplicate() {
		SVDBCoverpoint ret = new SVDBCoverpoint(getName());
		
		ret.init(this);
		
		return ret;
	}

	@Override
	public void init(SVDBItemBase other) {
		SVDBCoverpoint other_i = (SVDBCoverpoint)other;
		
		super.init(other);
		
		fTarget = other_i.fTarget;
	}

/*
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SVDBCoverpoint) {
			// TODO:
			return true;
		} else {
			return false;
		}
	}
 */
	
}
