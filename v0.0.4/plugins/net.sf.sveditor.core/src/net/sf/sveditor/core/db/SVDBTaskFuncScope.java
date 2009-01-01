package net.sf.sveditor.core.db;

import java.util.ArrayList;
import java.util.List;

public class SVDBTaskFuncScope extends SVDBScopeItem implements IFieldItemAttr {
	private List<SVDBTaskFuncParam>			fParams;
	private int								fAttr;
	
	public SVDBTaskFuncScope(String name, SVDBItemType type) {
		super(name, type);
		fParams = new ArrayList<SVDBTaskFuncParam>();
	}
	
	public void setAttr(int attr) {
		fAttr = attr;
	}
	
	public int getAttr() {
		return fAttr;
	}
	
	public void addParam(SVDBTaskFuncParam p) {
		p.setParent(this);
		fParams.add(p);
	}
	
	public List<SVDBTaskFuncParam> getParams() {
		return fParams;
	}
	
	public SVDBItem duplicate() {
		SVDBTaskFuncScope ret = new SVDBTaskFuncScope(getName(), getType());
		
		ret.init(this);
		
		return ret;
	}
	
	public void init(SVDBItem other) {
		super.init(other);

		fParams.clear();
		for (SVDBTaskFuncParam p : ((SVDBTaskFuncScope)other).fParams) {
			fParams.add((SVDBTaskFuncParam)p.duplicate());
		}
	}
	
}
