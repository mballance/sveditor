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

import java.util.List;


public class SVDBTypeInfoUserDef extends SVDBTypeInfo implements ISVDBScopeItem {
	protected SVDBParamValueAssignList				fParamAssignList;
	protected SVDBLocation							fEndLocation;
	protected List<ISVDBItemBase>					fItems;
	
	public SVDBTypeInfoUserDef() {
		this("");
	}
	
	public SVDBTypeInfoUserDef(String typename) {
		this(typename, SVDBItemType.TypeInfoUserDef);
	}
	
	public SVDBTypeInfoUserDef(String typename, SVDBItemType type) {
		super(typename, type);
	}
	
	public SVDBLocation getEndLocation() {
		return fEndLocation;
	}

	public List<ISVDBItemBase> getItems() {
		return fItems;
	}

	public void setEndLocation(SVDBLocation loc) {
		// TODO Auto-generated method stub
		
	}

	public SVDBParamValueAssignList getParameters() {
		return fParamAssignList;
	}

	public void setParameters(SVDBParamValueAssignList params) {
		fParamAssignList = params;
	}
	
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append(getName());
		
		if (fParamAssignList != null && fParamAssignList.getParameters().size() > 0) {
			ret.append(" #(");
			
			for (SVDBParamValueAssign p : fParamAssignList.getParameters()) {
				if (fParamAssignList.getIsNamedMapping()) {
					ret.append("." + p.getName() + "(" + p.getValue() + "), ");
				} else {
					ret.append(p.getValue() + ", ");
				}
			}
			
			ret.setLength(ret.length()-2);
			ret.append(")");
		}
		
		return ret.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SVDBTypeInfoUserDef) {
			SVDBTypeInfoUserDef o = (SVDBTypeInfoUserDef)obj;
			
			if (fParamAssignList == null || o.fParamAssignList == null) {
				if (fParamAssignList != o.fParamAssignList) {
					return false;
				}
			} else if (fParamAssignList.equals(o.fParamAssignList)) {
				return false;
			}
			
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public SVDBTypeInfoUserDef duplicate() {
		return (SVDBTypeInfoUserDef)super.duplicate();
	}
	
}
