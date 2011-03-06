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
import net.sf.sveditor.core.db.stmt.SVDBParamPort;
import net.sf.sveditor.core.db.stmt.SVDBVarDeclItem;

public class SVPortListParser extends SVParserBase {
	
	public SVPortListParser(ISVParser parser) {
		super(parser);
	}
	
	public List<SVDBParamPort> parse() throws SVParseException {
		List<SVDBParamPort> ports = new ArrayList<SVDBParamPort>();
		int dir = SVDBParamPort.Direction_Input;
		SVDBTypeInfo last_type = null;
		
		fLexer.readOperator("(");
		
		if (fLexer.peekOperator(".*")) {
			fLexer.eatToken();
			fLexer.readOperator(")");
			return ports;
		}
		
		if (fLexer.peekOperator(")")) {
			// empty port list
			fLexer.eatToken();
			return ports;
		}
		
		while (true) {
			SVDBLocation it_start = fLexer.getStartLocation();
			if (fLexer.peekKeyword("input", "output", "inout", "ref")) {
				String dir_s = fLexer.eatToken();
				if (dir_s.equals("input")) {
					dir = SVDBParamPort.Direction_Input;
				} else if (dir_s.equals("output")) {
					dir = SVDBParamPort.Direction_Output;
				} else if (dir_s.equals("inout")) {
					dir = SVDBParamPort.Direction_Inout;
				} else if (dir_s.equals("ref")) {
					dir = SVDBParamPort.Direction_Ref;
				}
			} else if (fLexer.peekKeyword("const")) {
				fLexer.eatToken();
				fLexer.readKeyword("ref");
				dir = (SVDBParamPort.Direction_Ref | SVDBParamPort.Direction_Const);
			}
			
			// This may be an untyped vectored parameter
			SVDBTypeInfo type = null; 
			String id = null;
			if (fLexer.peekOperator("[")) {
				fLexer.startCapture();
				// TODO: handle multi-dimensional vectors
				while (fLexer.peekOperator("[")) {
					fLexer.skipPastMatch("[", "]");
				}
				String vector_dim = fLexer.endCapture();
				SVDBTypeInfoBuiltin bi_type = new SVDBTypeInfoBuiltin("untyped");
				bi_type.setVectorDim(vector_dim);
				type = bi_type;

				// Relax to allow use of SV keywords for Verilog ports
				id = fLexer.readIdOrKeyword();
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
					if (fLexer.peekOperator("[")) {
						fLexer.startCapture();
						fLexer.skipPastMatch("[", "]");
						fLexer.endCapture();
					}
					 */

					last_type = type;
				}
			}
			

			SVDBParamPort param_r = new SVDBParamPort(type);
			param_r.setDir(dir);
			param_r.setLocation(it_start);
			SVDBVarDeclItem param = new SVDBVarDeclItem(id);
			param_r.addVar(param);

			if (fLexer.peekOperator("[")) {
				// This port is an array port
				param.setArrayDim(parsers().dataTypeParser().var_dim());
			}

			ports.add(param_r);

			/*
			if (fLexer.peekOperator("=")) {
				fLexer.eatToken();
				// TODO: read expression
				parsers().SVParser().readExpression();
			}
			 */
			
			if (fLexer.peekOperator(",")) {
				fLexer.eatToken();
			} else {
				break;
			}
		}
		
		fLexer.readOperator(")");
		
		return ports;
	}

}
