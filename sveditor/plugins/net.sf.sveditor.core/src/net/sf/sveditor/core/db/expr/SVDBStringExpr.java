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


package net.sf.sveditor.core.db.expr;

import net.sf.sveditor.core.db.ISVDBVisitor;
import net.sf.sveditor.core.db.SVDBItemType;

public class SVDBStringExpr extends SVDBExpr {
	public String				fStr;
	
	public SVDBStringExpr() {
		this("");
	}
	
	public SVDBStringExpr(String str) {
		super(SVDBItemType.StringExpr);
		fStr = str;
	}
	
	public String getContent() {
		return fStr;
	}
	
	public SVDBStringExpr duplicate() {
		return (SVDBStringExpr)super.duplicate();
	}
	
	@Override
	public void accept(ISVDBVisitor v) {
		v.visit_string_expr(this);
	}

}
