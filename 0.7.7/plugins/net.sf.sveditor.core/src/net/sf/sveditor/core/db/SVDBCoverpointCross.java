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

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.db.expr.SVDBExpr;
import net.sf.sveditor.core.db.expr.SVDBIdentifierExpr;

public class SVDBCoverpointCross extends SVDBScopeItem {
	List<SVDBIdentifierExpr>	fCoverpointList;
	SVDBExpr					fIFF;

	public SVDBCoverpointCross() {
		super("", SVDBItemType.CoverpointCross);
	}

	public SVDBCoverpointCross(String name) {
		super(name, SVDBItemType.CoverpointCross);
		fCoverpointList = new ArrayList<SVDBIdentifierExpr>();
	}
	
	public SVDBExpr getIFF() {
		return fIFF;
	}
	
	public void setIFF(SVDBExpr expr) {
		fIFF = expr;
	}

	public List<SVDBIdentifierExpr> getCoverpointList() {
		return fCoverpointList;
	}
	
	@Override
	public SVDBItemBase duplicate() {
		return (SVDBCoverpointCross)SVDBItemUtils.duplicate(this);
	}

	@Override
	public void init(SVDBItemBase other) {
		SVDBCoverpointCross other_i = (SVDBCoverpointCross)other;
		
		super.init(other);
		
		fCoverpointList.clear();
		fCoverpointList.addAll(other_i.fCoverpointList);
	}


	/*
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SVDBCoverpointCross) {
			SVDBCoverpointCross o = (SVDBCoverpointCross)obj;
			if (o.fCoverpointList.size() == fCoverpointList.size()) {
				for (int i=0; i<fCoverpointList.size(); i++) {
					if (!o.fCoverpointList.get(i).equals(fCoverpointList.get(i))) {
						return false;
					}
				}
			}
			return super.equals(obj);
		}
		
		return false;
	}
	 */

}
