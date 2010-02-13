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


package net.sf.sveditor.core.expr.parser;

public class SVExprParseException extends Exception {
	
	private static final long serialVersionUID = 4403018861977065475L;

	public SVExprParseException(String msg) {
		super(msg);
	}
	
	public SVExprParseException(Exception e) {
		super(e);
	}

}
