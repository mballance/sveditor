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

public class SVDBModIfcClassDecl extends SVDBScopeItem {
	
	private List<SVDBModIfcClassParam>			fParams;
	private String								fSuperClass;
	private List<SVDBModIfcClassParam>			fSuperParams;
	
	public static void init() {
		ISVDBPersistenceFactory f = new ISVDBPersistenceFactory() {
			public SVDBItem readSVDBItem(IDBReader reader, SVDBItemType type, 
					SVDBFile file, SVDBScopeItem parent) throws DBFormatException {
				return new SVDBModIfcClassDecl(file, parent, type, reader);
			}
		};
		
		SVDBPersistenceReader.registerPersistenceFactory(f,
				SVDBItemType.Class, SVDBItemType.Module, SVDBItemType.Interface,
				SVDBItemType.Struct); 
	}
	
	public SVDBModIfcClassDecl(String name, SVDBItemType type) {
		super(name, type);
		
		fParams = new ArrayList<SVDBModIfcClassParam>();
		fSuperParams = new ArrayList<SVDBModIfcClassParam>();
	}
	
	@SuppressWarnings("unchecked")
	public SVDBModIfcClassDecl(SVDBFile file, SVDBScopeItem parent, SVDBItemType type, IDBReader reader) throws DBFormatException {
		super(file, parent, type, reader);
		fParams     = (List<SVDBModIfcClassParam>)reader.readItemList(file, this);
		fSuperClass = reader.readString();
		fSuperParams = (List<SVDBModIfcClassParam>)reader.readItemList(file, this);
	}
	
	public void dump(IDBWriter writer) {
		super.dump(writer);
		writer.writeItemList(fParams);
		writer.writeString(fSuperClass);
		writer.writeItemList(fSuperParams);
	}
	
	
	public List<SVDBModIfcClassParam> getParameters() {
		return fParams;
	}
	
	public List<SVDBModIfcClassParam> getSuperParameters() {
		return fSuperParams;
	}
	
	public boolean isParameterized() {
		return ((fParams != null && fParams.size() > 0) ||
				(fSuperParams != null && fSuperParams.size() > 0));
	}
	
	public String getSuperClass() {
		return fSuperClass;
	}
	
	public void setSuperClass(String super_class) {
		fSuperClass = super_class;
	}
	
	public SVDBItem duplicate() {
		SVDBModIfcClassDecl ret = new SVDBModIfcClassDecl(getName(), getType());
		
		ret.init(this);
		
		return ret;
	}
	
	public void init(SVDBItem other) {
		super.init(other);

		if (((SVDBModIfcClassDecl)other).fParams != null) {
			fParams.clear();
			for (SVDBModIfcClassParam p : ((SVDBModIfcClassDecl)other).fParams) {
				fParams.add((SVDBModIfcClassParam)p.duplicate());
			}
		} else {
			fParams = null;
		}
	}
}
