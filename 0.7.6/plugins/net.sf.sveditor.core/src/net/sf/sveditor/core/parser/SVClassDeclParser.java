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

import net.sf.sveditor.core.db.IFieldItemAttr;
import net.sf.sveditor.core.db.ISVDBAddChildItem;
import net.sf.sveditor.core.db.SVDBClassDecl;
import net.sf.sveditor.core.db.SVDBLocation;
import net.sf.sveditor.core.db.SVDBTypeInfoClassType;

public class SVClassDeclParser extends SVParserBase {
	
	public SVClassDeclParser(ISVParser parser) {
		super(parser);
	}
	
	/**
	 * 
	 * [ virtual ] class [ lifetime ] class_identifier [ parameter_port_list ]
	 * [ extends class_type [ ( list_of_arguments ) ] ];
	 * @param qualifiers
	 * @return
	 * @throws SVParseException
	 */
	public void parse(ISVDBAddChildItem parent, int qualifiers) throws SVParseException {
		SVDBClassDecl cls = null;
		SVDBTypeInfoClassType cls_type;
		String cls_type_name = null;
		
		if (fDebugEn) {
			debug("--> process_class()");
		}
		
		// Expect to enter on 'class'
		SVDBLocation start_loc = fLexer.getStartLocation();
		fLexer.readKeyword("class");
		
		if (fLexer.peekKeyword("automatic", "static")) {
			// TODO: set lifetime on class declaration
			fLexer.eatToken();
		}

		//
		// Class ID
		//
		cls_type_name = parsers().SVParser().scopedIdentifier(
				((qualifiers & IFieldItemAttr.FieldAttr_SvBuiltin) != 0));
		
		cls = new SVDBClassDecl(cls_type_name);
		cls.setLocation(start_loc);
		
		cls_type = new SVDBTypeInfoClassType(cls_type_name);
		cls.setClassType(cls_type);
		
		if (fLexer.peekOperator("#")) {
			// Handle classes with parameters
			cls.addParameters(parsers().paramPortListParser().parse());
		}
		
		if (fLexer.peekKeyword("extends")) {
			fLexer.eatToken();
			cls.setSuperClass(parsers().dataTypeParser().class_type());

			if (fLexer.peekOperator("#")) {
				// scanner().unget_ch('#');
				// TODO: List<SVDBModIfcClassParam> params = fParamDeclParser.parse();
				// cls.getSuperParameters().addAll(params);
				fLexer.eatToken();
				if (fLexer.peekOperator("(")) {
					fLexer.skipPastMatch("(", ")");
				} else {
					fLexer.eatToken();
				}
			}
		}
		
		fLexer.readOperator(";");
		
		parent.addChildItem(cls);
		
		// TODO: need a better system here...
		while (fLexer.peek() != null && !fLexer.peekKeyword("endclass")) {
			
			try {
				fParsers.modIfcBodyItemParser().parse(cls, "class");
			} catch (SVParseException e) {
				// Catch error
				// TODO: recover from errors
				while (fLexer.peek() != null && 
						!fLexer.peekOperator(";") && !fLexer.peekKeyword("endclass")) {
					fLexer.eatToken();
				}
			}
		}

		cls.setEndLocation(fLexer.getStartLocation());
		fLexer.readKeyword("endclass");

		// endclass : classname
		if (fLexer.peekOperator(":")) { 
			fLexer.eatToken();
			fLexer.readId();
		}
	}

}
