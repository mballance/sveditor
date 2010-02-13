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


package net.sf.sveditor.core.db.persistence;

import java.util.List;

import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBScopeItem;

public interface IDBReader {
	
	int readInt() throws DBFormatException;
	
	long readLong() throws DBFormatException;
	
	byte [] readByteArray() throws DBFormatException;
	
	String readString() throws DBFormatException;
	
	SVDBItemType readItemType() throws DBFormatException;
	
	@SuppressWarnings("unchecked")
	List readItemList(SVDBFile file, SVDBScopeItem parent) throws DBFormatException;
	
	List<String> readStringList() throws DBFormatException;
	
	List<Integer> readIntList() throws DBFormatException;

}
