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

import net.sf.sveditor.core.db.persistence.DBFormatException;
import net.sf.sveditor.core.db.persistence.IDBReader;
import net.sf.sveditor.core.db.persistence.IDBWriter;
import net.sf.sveditor.core.db.persistence.ISVDBPersistenceFactory;
import net.sf.sveditor.core.db.persistence.SVDBPersistenceReader;

public class SVDBParamValueAssign extends SVDBItem {
	private String						fValue;
	
	public SVDBParamValueAssign(String name, String value) {
		super(name, SVDBItemType.ParamValue);
		fValue = value;
	}
	
	public SVDBParamValueAssign(SVDBFile file, SVDBScopeItem parent, SVDBItemType type, IDBReader reader) throws DBFormatException {
		super(file, parent, type, reader);
		fValue = reader.readString();
	}
	
	public static void init() {
		ISVDBPersistenceFactory f = new ISVDBPersistenceFactory() {
			public SVDBItemBase readSVDBItem(IDBReader reader, SVDBItemType type, 
					SVDBFile file, SVDBScopeItem parent) throws DBFormatException {
				return new SVDBParamValueAssign(file, parent, type, reader);
			}
		};
		
		SVDBPersistenceReader.registerPersistenceFactory(
				f, SVDBItemType.ParamValue); 
	}

	@Override
	public void dump(IDBWriter writer) {
		super.dump(writer);
		writer.writeString(fValue);
	}
	
	public String getValue() {
		return fValue;
	}

	@Override
	public SVDBItemBase duplicate() {
		SVDBParamValueAssign ret = new SVDBParamValueAssign(getName(), fValue);
		ret.init(this);
		
		return ret;
	}

	@Override
	public void init(SVDBItemBase other) {
		super.init(other);
		
		fValue = ((SVDBParamValueAssign)other).fValue;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SVDBParamValueAssign) {
			return (fValue.equals(((SVDBParamValueAssign)obj).fValue) &&
					super.equals(obj));
		}
		
		return false;
	}
	
}
