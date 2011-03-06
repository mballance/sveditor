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

import net.sf.sveditor.core.db.ISVDBChildItem;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.SVDBFieldItem;
import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBLocation;
import net.sf.sveditor.core.db.SVDBModIfcClassDecl;
import net.sf.sveditor.core.db.stmt.SVDBParamPort;

public class SVModIfcProgDeclParser extends SVParserBase {
	
	public SVModIfcProgDeclParser(ISVParser parser) {
		super(parser);
	}
	
	public SVDBModIfcClassDecl parse(int qualifiers) throws SVParseException {
		String id;
		String module_type_name = null;
		SVDBModIfcClassDecl module = null;

		debug("--> process_mod_ifc_prog()");
		
		SVDBLocation start = fLexer.getStartLocation();
		String type_name = fLexer.readKeyword("module", "macromodule",
				"interface", "program");
		SVDBItemType type = null;
		if (type_name.equals("module") || type_name.equals("macromodule")) {
			type = SVDBItemType.Module;
		} else if (type_name.equals("interface")) {
			type = SVDBItemType.Interface;
		} else if (type_name.equals("program")) {
			type = SVDBItemType.Program;
		} else {
			error("Unsupported module/interface/program type-name " + type_name);
		}
		
		if (fLexer.peekKeyword("static", "automatic")) {
			// TODO: tag with lifetime
			fLexer.eatToken();
		}
		
		if (type == SVDBItemType.Program && fLexer.peekOperator(";")) {
			// anonymous program block
			module_type_name = "";
		} else {
			module_type_name = fLexer.readId();
		}
		

		module = new SVDBModIfcClassDecl(module_type_name, type);
		module.setLocation(start);
		
		// TODO: Should remove this later
		parsers().SVParser().enter_scope(type_name, module);

		if (type != SVDBItemType.Program) {
			// May have imports prior to the port declaration
			while (fLexer.peekKeyword("import")) {
				// Import statement
				ISVDBChildItem imp = parsers().impExpParser().parse_import();
				module.addItem(imp);
			}
		}

		// Handle modules with parameters
		if (fLexer.peekOperator("#")) {
			// Handle in-line parameter declarations
			module.getParameters().addAll(parsers().paramPortListParser().parse());
		}

		if (fLexer.peekOperator("(")) {
			// port-list
			List<SVDBParamPort> ports = parsers().portListParser().parse();
			module.getPorts().addAll(ports);
		}
		fLexer.readOperator(";");
		
		// TODO: should be cleaned up
		parsers().SVParser().setNewStatement();

		// Extern module/programs don't have bodies
		if ((qualifiers & SVDBFieldItem.FieldAttr_Extern) == 0) {
			while (fLexer.peek() != null && !fLexer.peekKeyword("end" + type_name)) {
				try {
					ISVDBChildItem item = fParsers.modIfcBodyItemParser().parse(type_name);
					
					// Check whether we aborted parsing the body because
					// we found a 1st-level scope keyword
					if (item == null) {
						break;
					}

					module.addItem(item);

				} catch (SVParseException e) {
					// TODO: How to adapt?
					debug("Module body item parse failed", e);
				}

				// TODO: Should have already been added ?
				// fScopeStack.peek().addItem(item);
			}
			
			SVDBLocation end = fLexer.getStartLocation();
			module.setEndLocation(end);
			fLexer.readKeyword("end" + type_name);
		} else {
			SVDBLocation end = fLexer.getStartLocation();
			module.setEndLocation(end);
		}

		// Pop the first-level scope
		parsers().SVParser().handle_leave_scope();

		debug("<-- process_mod_ifc_prog()");
		return module;
	}

}
