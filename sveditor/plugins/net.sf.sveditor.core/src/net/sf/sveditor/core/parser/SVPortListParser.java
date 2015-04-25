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


package net.sf.sveditor.core.parser;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.db.SVDBTypeInfo;
import net.sf.sveditor.core.db.SVDBTypeInfoBuiltin;
import net.sf.sveditor.core.db.stmt.SVDBParamPortDecl;
import net.sf.sveditor.core.db.stmt.SVDBVarDeclItem;

public class SVPortListParser extends SVParserBase {
	
	public SVPortListParser(ISVParser parser) {
		super(parser);
	}
	
	public List<SVDBParamPortDecl> parse() throws SVParseException {
		List<SVDBParamPortDecl> ports = new ArrayList<SVDBParamPortDecl>();
		int dir = SVDBParamPortDecl.Direction_Input;
		SVDBTypeInfo last_type = null;
		boolean is_ansi = false;
		
		fLexer.readOperator(OP.LPAREN);

		
		if (fLexer.peekOperator(".*")) {
			fLexer.eatToken();
			fLexer.readOperator(OP.RPAREN);
			return ports;
		}
		
		if (fLexer.peekOperator(OP.RPAREN)) {
			// empty port list
			fLexer.eatToken();
			return ports;
		}
		
		while (true) {
			long it_start = fLexer.getStartLocation();
			
			// Catch per-port attributes
			try {
				if (fLexer.peekOperator("(*")) {
					fParsers.attrParser().parse(null);
				}
			} catch (SVParseException e) {}
			
			if (fLexer.peekKeyword("input", "output", "inout", "ref")) {
				it_start = fLexer.getStartLocation();
				String dir_s = fLexer.eatToken();
				is_ansi = true;
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
				it_start = fLexer.getStartLocation();
				fLexer.eatToken();
				fLexer.readKeyword("ref");
				dir = (SVDBParamPortDecl.Direction_Ref | SVDBParamPortDecl.Direction_Const);
			}
			
			// This may be an untyped vectored parameter
			SVDBTypeInfo type = null; 
			String id = null;
			if (fLexer.peekOperator(OP.LBRACKET)) {
				SVDBTypeInfoBuiltin bi_type = new SVDBTypeInfoBuiltin("untyped");
				bi_type.setVectorDim(fParsers.dataTypeParser().vector_dim());
				type = bi_type;

				id = fLexer.readId();
			} else {
				type = parsers().dataTypeParser().data_type(0);

				// This could be a continuation of the same type: int a, b, c


				// Handle the case where a single type and a 
				// list of parameters is declared
				if (fLexer.peekOperator(",", ")", "=", "[")) {
					// use previous type
					id = type.getName();
					if (last_type == null) {
						// this is an untyped parameter. 
					}
					type = last_type;
				} else {
					// Relax to allow use of SV keywords
					id = fLexer.readIdOrKeyword();

					/* 
					if (fLexer.peekOperator(OP.LBRACKET)) {
						fLexer.startCapture();
						fLexer.skipPastMatch("[", "]");
						fLexer.endCapture();
					}
					 */

					last_type = type;
				}
			}
			

			SVDBParamPortDecl param_r = new SVDBParamPortDecl(type);
			param_r.setDir(dir);
			param_r.setLocation(it_start);
			SVDBVarDeclItem param = new SVDBVarDeclItem(id);
			param.setLocation(it_start);
			param_r.addChildItem(param);

			if (fLexer.peekOperator(OP.LBRACKET)) {
				// This port is an array port
				param.setArrayDim(parsers().dataTypeParser().var_dim());
			}

			// Read in default value
			if (fLexer.peekOperator(OP.EQ)) {
				fLexer.eatToken();
				param.setInitExpr(parsers().exprParser().expression());
				if (fDebugEn) {
					debug("parameter default: " + param.getInitExpr());
				}
			}
			 
			ports.add(param_r);
			
			if (fLexer.peekOperator(OP.COMMA)) {
				fLexer.eatToken();
				if (!is_ansi && fLexer.peekOperator(OP.RPAREN)) {
					// We're done
					break;
				}
			} else {
				break;
			}
		}
		
		fLexer.readOperator(OP.RPAREN);
		
		return ports;
	}

}
