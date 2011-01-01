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

public enum SVExprType {
	ArrayAccess,
	Assign,
	Binary,
	Cast,
	Cond,
	Constraint,
	FieldAccess,
	Identifier,
	IncDec,
	Literal,
	Paren,
	Inside,
	Range,
	QualifiedSuperFieldRef,
	QualifiedThisRef,
	TFCall,
	Unary,
	ConstraintIf,
	ConstraintSet,
	DistList,
	DistItem,
	Implication,
	SolveBefore,
	
	Coverpoint,
	CoverBins,
	Concatenation

}
