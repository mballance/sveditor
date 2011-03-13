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


package net.sf.sveditor.core.db;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.db.stmt.SVDBVarDeclStmt;

public class SVDBTypeInfoStruct extends SVDBTypeInfo {
	private List<SVDBVarDeclStmt>			fFields;
	
	public SVDBTypeInfoStruct() {
		super("<<ANONYMOUS>>", SVDBItemType.TypeInfoStruct);
		fFields = new ArrayList<SVDBVarDeclStmt>();
	}
	
	public List<SVDBVarDeclStmt> getFields() {
		return fFields;
	}
	
	public void addField(SVDBVarDeclStmt f) {
		fFields.add(f);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SVDBTypeInfoStruct) {
			SVDBTypeInfoStruct o = (SVDBTypeInfoStruct)obj;
			
			if (fFields.size() == o.fFields.size()) {
				for (int i=0; i<fFields.size(); i++) {
					if (!fFields.get(i).equals(o.fFields.get(i))) {
						return false;
					}
				}
			} else {
				return false;
			}
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public SVDBTypeInfoStruct duplicate() {
		SVDBTypeInfoStruct ret = new SVDBTypeInfoStruct();
		ret.setName(getName());
		
		for (SVDBVarDeclStmt f : fFields) {
			ret.addField((SVDBVarDeclStmt)f.duplicate());
		}
		
		return ret;
	}
}
