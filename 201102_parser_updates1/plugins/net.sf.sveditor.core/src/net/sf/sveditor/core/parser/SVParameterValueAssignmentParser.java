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

import java.util.List;

import net.sf.sveditor.core.db.SVDBParamValueAssign;
import net.sf.sveditor.core.db.SVDBParamValueAssignList;
import net.sf.sveditor.core.db.SVDBTypeInfo;
import net.sf.sveditor.core.db.expr.SVDBExpr;
import net.sf.sveditor.core.scanner.SVKeywords;

public class SVParameterValueAssignmentParser extends SVParserBase {
	
	public SVParameterValueAssignmentParser(ISVParser parser) {
		super(parser);
	}
	
	public SVDBParamValueAssignList parse() throws SVParseException {
		SVDBParamValueAssignList ret = new SVDBParamValueAssignList();
		// StringBuilder v = new StringBuilder();
		
		fLexer.readOperator("#");
		fLexer.readOperator("(");
		while (true) {
			boolean is_mapped = false;
			String name = null;
			if (fLexer.peekOperator(".")) {
				fLexer.eatToken();
				name = fLexer.readId();
				fLexer.readOperator("(");
				is_mapped = true;
			}
			
			// TODO:
			// Skip forward to see if we have a scoped identifier
			List<SVToken> id_list = parsers().SVParser().peekScopedStaticIdentifier_l(false);
			
			if (fLexer.peekOperator("#") || fLexer.peekKeyword(SVKeywords.fBuiltinTypes)) {
				// This is actually a type reference
				fLexer.ungetToken(id_list);
				SVDBTypeInfo type = parsers().dataTypeParser().data_type(0);
				ret.addParameter(new SVDBParamValueAssign(name, type));
			} else {
				fLexer.ungetToken(id_list);
				SVDBExpr val = parsers().exprParser().expression();
				ret.addParameter(new SVDBParamValueAssign(name, val));
			}

			if (is_mapped) {
				// Read inside
				fLexer.readOperator(")");
			}

			//ret.addParameter(new SVDBParamValueAssign(name, v.toString()));
			ret.setIsNamedMapping(is_mapped);
			
			if (fLexer.peekOperator(",")) {
				fLexer.eatToken();
			} else {
				break;
			}
		}
		
		fLexer.readOperator(")");
		
		return ret;
	}
	
}
