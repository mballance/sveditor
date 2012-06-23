package net.sf.sveditor.core.db.expr;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.db.SVDBItemType;

public class SVDBPropertyCaseStmt extends SVDBExpr {
	
	public SVDBExpr						fExpr;
	public List<SVDBPropertyCaseItem>	fItemList;
	
	public SVDBPropertyCaseStmt() {
		super(SVDBItemType.PropertyCaseStmt);
		fItemList = new ArrayList<SVDBPropertyCaseItem>();
	}
	
	public void setExpr(SVDBExpr expr) {
		fExpr = expr;
	}
	
	public SVDBExpr getExpr() {
		return fExpr;
	}
	
	public void addItem(SVDBPropertyCaseItem item) {
		fItemList.add(item);
	}

}
