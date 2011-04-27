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

import net.sf.sveditor.core.db.stmt.SVDBVarDimItem;

public class SVDBTypeInfo extends SVDBItem implements ISVDBNamedItem {
	public static final int				TypeAttr_Vectored			= (1 << 6);
	protected SVDBVarDimItem			fArrayDim;
	
	public SVDBTypeInfo(String typename, SVDBItemType data_type) {
		super(typename, data_type);
		fLocation = null;
	}

	@Deprecated
	public SVDBItemType getDataType() {
		return getType();
	}
	
	@Deprecated
	public void setDataType(SVDBItemType type) {
		setType(type);
	}
	
	public SVDBVarDimItem getArrayDim() {
		return fArrayDim;
	}
	
	public void setArrayDim(SVDBVarDimItem dim) {
		fArrayDim = dim;
	}
	
	public void init(SVDBItemBase other) {
		super.init(other);
		
		SVDBTypeInfo other_t = (SVDBTypeInfo)other;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SVDBTypeInfo) {
			SVDBTypeInfo o = (SVDBTypeInfo)obj;
			
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public SVDBTypeInfo duplicate() {
		return (SVDBTypeInfo)super.duplicate();
	}
	
	public static boolean isDataType(SVDBItemType type) {
		return false;
	}
	
}
