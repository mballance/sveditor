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


package net.sf.sveditor.core.db.expr;

import net.sf.sveditor.core.db.SVDBItemType;


public class SVDBLiteralExpr extends SVDBExpr {
	
	String					fLiteral;
	
	public SVDBLiteralExpr() {
		this(null);
	}
	
	public SVDBLiteralExpr(String literal) {
		super(SVDBItemType.LiteralExpr);
		
		fLiteral = literal;
	}
	
	public String getValue() {
		return fLiteral;
	}
	
	public SVDBLiteralExpr duplicate() {
		return (SVDBLiteralExpr)super.duplicate();
	}

}
