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


package net.sf.sveditor.core.argfile.parser;

public class SVArgFileExprContext {
	public enum ContextType {
		OptionComplete,
		PathComplete,
	};
	
	
	// Document offset where 'leaf' begins
	public int 						fStart;
	
	public ContextType				fType;
	
	// Root of the expression
	public String					fRoot;
	
	// Leaf of the expression 
	public String					fLeaf;
	
	// Trigger character
	public String					fTrigger;

}
