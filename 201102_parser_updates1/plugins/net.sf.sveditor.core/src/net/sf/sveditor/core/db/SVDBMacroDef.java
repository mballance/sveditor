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

import net.sf.sveditor.core.db.persistence.DBFormatException;
import net.sf.sveditor.core.db.persistence.IDBReader;
import net.sf.sveditor.core.db.persistence.IDBWriter;
import net.sf.sveditor.core.db.persistence.ISVDBPersistenceFactory;
import net.sf.sveditor.core.db.persistence.SVDBPersistenceReader;

public class SVDBMacroDef extends SVDBItem {
	private List<String>			fParams;
	private String					fDef;
	
	public static void init() {
		ISVDBPersistenceFactory f = new ISVDBPersistenceFactory() {
			public SVDBItemBase readSVDBItem(ISVDBChildItem parent, SVDBItemType type, IDBReader reader) throws DBFormatException {
				return new SVDBMacroDef(parent, type, reader);
			}
		};
		
		SVDBPersistenceReader.registerPersistenceFactory(f, SVDBItemType.Macro); 
	}
	
	
	public SVDBMacroDef(
			String 				name, 
			List<String>		params,
			String				def) {
		super(name, SVDBItemType.Macro);
		fParams = new ArrayList<String>();
		fParams.addAll(params);
		fDef = def;
	}
	
	public SVDBMacroDef(ISVDBChildItem parent, SVDBItemType type, IDBReader reader) throws DBFormatException {
		super(parent, type, reader);
		fParams = reader.readStringList();
		fDef    = reader.readString();
	}
	
	public void dump(IDBWriter writer) {
		super.dump(writer);
		writer.writeStringList(fParams);
		writer.writeString(fDef);
	}
	
	
	public String getDef() {
		return fDef;
	}
	
	public void setDef(String def) {
		fDef = def;
	}
	
	public List<String> getParameters() {
		return fParams;
	}

	@Override
	public SVDBItemBase duplicate() {
		SVDBMacroDef ret = new SVDBMacroDef(
				getName(), fParams, fDef);
		
		ret.init(this);
		
		return ret;
	}

	@Override
	public void init(SVDBItemBase other) {
		super.init(other);
		
		SVDBMacroDef m = (SVDBMacroDef)other;
		fParams.clear();
		fParams.addAll(m.fParams);
		fDef = m.fDef;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SVDBMacroDef) {
			SVDBMacroDef o = (SVDBMacroDef)obj;
			
			if (fParams.size() == o.fParams.size()) {
				 if (!o.fName.equals(fName)) {
					 return false;
				 }
				 
				 for (int i=0; i<fParams.size(); i++) {
					 if (!fParams.get(i).equals(o.fParams.get(i))) {
						 return false;
					 }
				 }

				 return super.equals(obj);
			} else {
				return false;
			}
		}
		
		return false;
	}
	
}
