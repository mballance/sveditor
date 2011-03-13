package net.sf.sveditor.core.db.stmt;

import net.sf.sveditor.core.db.ISVDBChildItem;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.ISVDBNamedItem;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.expr.SVDBExpr;
import net.sf.sveditor.core.db.persistence.SVDBParentAttr;

public class SVDBVarDeclItem extends SVDBStmt implements ISVDBNamedItem {
	@SVDBParentAttr
	protected SVDBVarDeclStmt			fParent;
	protected String					fName;
	protected int						fAttr;
	protected int						fVarAttr;
	protected SVDBVarDimItem			fArrayDim;
	protected SVDBExpr					fInitExpr;
	
	public SVDBVarDeclItem() {
		super(SVDBItemType.VarDeclItem);
	}
	
	public SVDBVarDeclItem(String name) {
		super(SVDBItemType.VarDeclItem);
		fName = name;
	}
	
	public String getName() {
		return fName;
	}
	
	public void setInitExpr(SVDBExpr expr) {
		fInitExpr = expr;
	}
	
	public SVDBExpr getInitExpr() {
		return fInitExpr;
	}
	
	public int getAttr() {
		return fAttr;
	}
	
	public void setAttr(int attr) {
		fAttr |= attr;
	}
	
	public void resetAttr(int attr) {
		fAttr = attr;
	}

	public SVDBVarDimItem getArrayDim() {
		return fArrayDim;
	}
	
	public void setArrayDim(SVDBVarDimItem dim) {
		fArrayDim = dim;
	}
	
	public SVDBVarDeclStmt getParent() {
		return fParent;
	}

	public void setParent(ISVDBChildItem parent) {
		fParent = (SVDBVarDeclStmt)parent;
	}

	public Iterable<ISVDBItemBase> getChildren() {
		// TODO Auto-generated method stub
		return null;
	}

	public SVDBVarDeclItem duplicate() {
		// TODO Auto-generated method stub
		return null;
	}

	public void init(ISVDBItemBase other) {
		// TODO Auto-generated method stub

	}

	public boolean equals(ISVDBItemBase other, boolean recurse) {
		// TODO Auto-generated method stub
		return false;
	}

}
