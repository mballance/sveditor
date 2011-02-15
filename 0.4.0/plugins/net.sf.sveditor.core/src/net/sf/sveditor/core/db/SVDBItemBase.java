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

import net.sf.sveditor.core.db.persistence.IDBWriter;


public class SVDBItemBase implements ISVDBItemBase {
	protected SVDBItemType			fType;
	protected SVDBLocation			fLocation;
	
	public SVDBItemBase(SVDBItemType type) {
		fType = type;
		fLocation = null;
	}

	public SVDBItemType getType() {
		return fType;
	}

	public SVDBLocation getLocation() {
		return fLocation;
	}

	public void setLocation(SVDBLocation location) {
		fLocation = location;
	}

	public void dump(IDBWriter writer) {
		writer.writeItemType(fType);
	}

	public SVDBItemBase duplicate() {
		SVDBItemBase ret = new SVDBItemBase(getType());
		ret.init(this);
		
		return ret;
	}

	public void init(ISVDBItemBase other) {
		fType = other.getType();
		if (other.getLocation() != null) {
			fLocation = other.getLocation().duplicate();
		} else {
			fLocation = null;
		}
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof SVDBItemBase) {
			return equals((ISVDBItemBase)obj, true);
		} else {
			return false;
		}
	}

	public boolean equals(ISVDBItemBase obj, boolean full) {
		boolean ret = false;
		if (obj instanceof SVDBItemBase) {
			ret = true;
			SVDBItemBase other = (SVDBItemBase)obj;

			if (other.fType == null || fType == null) {
				ret &= other.fType == fType;
			} else {
				ret &= other.fType.equals(fType);
			}

			if (full) {
				if (fLocation == null || other.fLocation == null) {
					ret &= (fLocation == other.fLocation);
				} else {
					ret &= other.fLocation.equals(fLocation);
				}
			}
		}
		
		return ret;
	}
	
}
