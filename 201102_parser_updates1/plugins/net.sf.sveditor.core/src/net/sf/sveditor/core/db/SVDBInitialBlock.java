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
import net.sf.sveditor.core.db.persistence.ISVDBPersistenceFactory;
import net.sf.sveditor.core.db.persistence.SVDBPersistenceReader;

public class SVDBInitialBlock extends SVDBScopeItem {
	
	public static void init() {
		ISVDBPersistenceFactory f = new ISVDBPersistenceFactory() {
			public SVDBItemBase readSVDBItem(ISVDBChildItem parent, SVDBItemType type, IDBReader reader) throws DBFormatException {
				return new SVDBInitialBlock(parent, type, reader);
			}
		};
		
		SVDBPersistenceReader.registerPersistenceFactory(f, SVDBItemType.InitialBlock); 
	}
	
	public SVDBInitialBlock() {
		super("", SVDBItemType.InitialBlock);
	}

	public SVDBInitialBlock(ISVDBChildItem parent, SVDBItemType type, IDBReader reader) throws DBFormatException {
		super(parent, type, reader);
	}

	@Override
	public SVDBItemBase duplicate() {
		SVDBInitialBlock ret = new SVDBInitialBlock();
		
		ret.init(this);
		
		return ret;
	}

	@Override
	public void init(SVDBItemBase other) {
		super.init(other);
	}

}
