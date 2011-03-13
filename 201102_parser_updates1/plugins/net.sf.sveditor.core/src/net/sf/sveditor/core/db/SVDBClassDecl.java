package net.sf.sveditor.core.db;

import java.util.ArrayList;
import java.util.List;


public class SVDBClassDecl extends SVDBScopeItem {
	
	private List<SVDBModIfcClassParam>			fParams;
	private SVDBTypeInfoClassType				fClassType;
	private SVDBTypeInfoClassType				fSuperClass;

	public SVDBClassDecl() {
		this("");
	}
	
	public SVDBClassDecl(String name) {
		super(name, SVDBItemType.ClassDecl);
	}

	public List<SVDBModIfcClassParam> getParameters() {
		return fParams;
	}
	
	public void addParameters(List<SVDBModIfcClassParam> params) {
		if (fParams == null) {
			fParams = new ArrayList<SVDBModIfcClassParam>();
		}
		fParams.addAll(params);
	}
	
	public SVDBTypeInfoClassType getClassType() {
		return fClassType;
	}
	
	public void setClassType(SVDBTypeInfoClassType cls_type) {
		fClassType = cls_type;
	}

	public SVDBTypeInfoClassType getSuperClass() {
		return fSuperClass;
	}
	
	public void setSuperClass(SVDBTypeInfoClassType super_class) {
		fSuperClass = super_class;
	}
	
	public SVDBClassDecl duplicate() {
		SVDBClassDecl ret = new SVDBClassDecl(getName());
		
		ret.init(this);
		
		return ret;
	}

	public void init(SVDBItemBase other) {
		super.init(other);
		SVDBClassDecl o = (SVDBClassDecl)other;

		if (o.fParams != null) {
			fParams.clear();
			for (SVDBModIfcClassParam p : o.fParams) {
				fParams.add((SVDBModIfcClassParam)p.duplicate());
			}
		} else {
			fParams = null;
		}
		
		setSuperClass(o.getSuperClass());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SVDBClassDecl) {
			SVDBClassDecl o = (SVDBClassDecl)obj;

			if (fParams == null || o.fParams == null) {
				if (fParams != o.fParams) {
					return false;
				}
			} else {
				if (fParams.size() == o.fParams.size()) {
					for (int i=0; i<fParams.size(); i++) {
						SVDBModIfcClassParam p1 = fParams.get(i);
						SVDBModIfcClassParam p2 = o.fParams.get(i);

						if (!p1.equals(p2)) {
							return false;
						}
					}
				} else {
					return false;
				}
			}
			
			if (fSuperClass == null || o.fSuperClass == null) {
				if (fSuperClass != o.fSuperClass) {
					return false;
				}
			} else if (!fSuperClass.equals(o.fSuperClass)) {
				return false;
			}
			
			return super.equals(obj);
		}
		return false;
	}

}
