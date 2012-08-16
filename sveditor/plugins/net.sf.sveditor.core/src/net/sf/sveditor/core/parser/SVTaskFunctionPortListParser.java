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


package net.sf.sveditor.core.parser;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.db.SVDBLocation;
import net.sf.sveditor.core.db.SVDBTypeInfo;
import net.sf.sveditor.core.db.SVDBTypeInfoBuiltin;
import net.sf.sveditor.core.db.SVDBTypeInfoBuiltinNet;
import net.sf.sveditor.core.db.SVDBTypeInfoUserDef;
import net.sf.sveditor.core.db.stmt.SVDBParamPortDecl;
import net.sf.sveditor.core.db.stmt.SVDBVarDeclItem;
import net.sf.sveditor.core.db.stmt.SVDBVarDimItem;

public class SVTaskFunctionPortListParser extends SVParserBase {
	
	public SVTaskFunctionPortListParser(ISVParser parser) {
		super(parser);
	}
	
	public List<SVDBParamPortDecl> parse() throws SVParseException {
		List<SVDBParamPortDecl> params = new ArrayList<SVDBParamPortDecl>();
		int dir = SVDBParamPortDecl.Direction_Input;
		SVDBTypeInfo last_type = null;
		
		fLexer.readOperator("(");
		
		// Empty parameter list
		if (fLexer.peekOperator(")")) {
			fLexer.eatToken();
			return params;
		}
		
		while (true) {
			SVDBLocation it_start = fLexer.getStartLocation();
			if (fLexer.peekKeyword("input", "output", "inout", "ref")) {
				String dir_s = fLexer.eatToken();
				if (dir_s.equals("input")) {
					dir = SVDBParamPortDecl.Direction_Input;
				} else if (dir_s.equals("output")) {
					dir = SVDBParamPortDecl.Direction_Output;
				} else if (dir_s.equals("inout")) {
					dir = SVDBParamPortDecl.Direction_Inout;
				} else if (dir_s.equals("ref")) {
					dir = SVDBParamPortDecl.Direction_Ref;
				}
			} else if (fLexer.peekKeyword("const")) {
				fLexer.eatToken();
				fLexer.readKeyword("ref");
				dir = (SVDBParamPortDecl.Direction_Ref | SVDBParamPortDecl.Direction_Const);
			}
			
			if (fLexer.peekKeyword("var")) {
				fLexer.eatToken();
				dir |= SVDBParamPortDecl.Direction_Var;
			}
			
			SVDBTypeInfo type = parsers().dataTypeParser().data_type(0);

			// This could be a continuation of the same type: int a, b, c
			if (fLexer.peekOperator("[")) {
				List<SVDBVarDimItem> dim = fParsers.dataTypeParser().vector_dim();
				if (type instanceof SVDBTypeInfoBuiltin) {
					((SVDBTypeInfoBuiltin)type).setVectorDim(dim);
				} else {
					// TODO:
				}
			}

			String id;

			// Handle the case where a single type and a 
			// list of parameters is declared
			if (fLexer.peekOperator(",", ")", "=", "[")) {
				// use previous type
				id = type.getName();
				type = last_type;
			} else {

				id = fLexer.readId();

				/**
				if (fLexer.peekOperator("[")) {
					fLexer.startCapture();
					fLexer.skipPastMatch("[", "]");
					fLexer.endCapture();
				}
				 */
				
				last_type = type;
			}

			
			SVDBParamPortDecl param_r = new SVDBParamPortDecl(type);
			param_r.setDir(dir);
			param_r.setLocation(it_start);
			
			SVDBVarDeclItem param = new SVDBVarDeclItem(id);
			param_r.addChildItem(param);
			
			if (fLexer.peekOperator("[")) {
				// This port is an array port
				param.setArrayDim(parsers().dataTypeParser().var_dim());
			}

			params.add(param_r);
			
			if (fLexer.peekOperator("=")) {
				fLexer.eatToken();
				param.setInitExpr(parsers().exprParser().expression());
			}
			
			if (fLexer.peekOperator(",")) {
				fLexer.eatToken();
			} else {
				break;
			}
		}
		
		fLexer.readOperator(")");
		
		return params;
	}

}
