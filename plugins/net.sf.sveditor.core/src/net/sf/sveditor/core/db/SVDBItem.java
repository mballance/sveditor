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

import net.sf.sveditor.core.db.attr.SVDBParentAttr;
import net.sf.sveditor.core.db.expr.SVDBIdentifierExpr;



public class SVDBItem extends SVDBItemBase implements ISVDBNamedItem, ISVDBChildItem {
	
	@SVDBParentAttr
	public ISVDBChildItem		fParent;
	
	public String				fName;
	
	public SVDBItem(String name, SVDBItemType type) {
		super(type);
		if (name == null) {
			fName = "";
		} else {
			fName = name;
		}
	}

	public SVDBItem(SVDBIdentifierExpr name, SVDBItemType type) {
		super(type);
		if (name == null) {
			fName = "";
		} else {
			fName = name.getId();
		}
	}

	public void setParent(ISVDBChildItem parent) {
		fParent = parent;
	}
	
	public ISVDBChildItem getParent() {
		return fParent;
	}
	
	/*
	public Iterable<ISVDBChildItem> getChildren() {
		return EmptySVDBChildItemIterable.iterable;
	}
	 */

	public String getName() {
		return fName;
	}
	
	public void setName(String name) {
		fName = name;
	}
	
	public void setType(SVDBItemType type) {
		fType = type;
	}
	
	public SVDBItemType getType() {
		return fType;
	}
	
	public void init(SVDBItemBase other) {
		SVDBItem o = (SVDBItem)other;
		fName     = o.fName;
		fParent   = o.fParent;
		super.init(o);
	}
	
	/*
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof SVDBItem) {
			return equals((SVDBItem)obj, true);
		} else {
			return super.equals(obj);
		}
	}
	 */

	@Override
	public boolean equals(ISVDBItemBase obj, boolean full) {
		boolean ret = false;
		
		if (obj instanceof SVDBItem) {
			ret = true;
			SVDBItem other = (SVDBItem)obj;

			if (other.fName == null || fName == null) {
				ret &= other.fName == fName;
			} else {
				ret &= other.fName.equals(fName);
			}

			ret &= super.equals(obj, full);
		}
		
		return ret;
	}
	
	public static String getName(ISVDBItemBase item) {
		if (item == null) {
			return "null";
		} else if (item instanceof ISVDBNamedItem) {
			return ((ISVDBNamedItem)item).getName();
		} else {
			return "UNKNOWN " + item.getType();
		}
	}
	
}
