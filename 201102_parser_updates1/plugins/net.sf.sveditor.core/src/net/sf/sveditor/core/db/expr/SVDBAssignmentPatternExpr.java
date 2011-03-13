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


package net.sf.sveditor.core.db.expr;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.db.SVDBItemBase;
import net.sf.sveditor.core.db.SVDBItemType;

public class SVDBAssignmentPatternExpr extends SVDBExpr {
	private List<SVDBExpr>			fPatternList;
	
	public SVDBAssignmentPatternExpr() {
		this(SVDBItemType.AssignmentPatternExpr);
	}

	public SVDBAssignmentPatternExpr(SVDBItemType type) {
		super(type);
		fPatternList = new ArrayList<SVDBExpr>();
	}

	public List<SVDBExpr> getPatternList() {
		return fPatternList;
	}
	
	public SVDBAssignmentPatternExpr duplicate() {
		SVDBAssignmentPatternExpr ret = new SVDBAssignmentPatternExpr();
		for (SVDBExpr e : fPatternList) {
			ret.fPatternList.add((SVDBExpr)e.duplicate());
		}
		
		ret.init(this);
		
		return ret;
	}
	
	public void init(SVDBItemBase other) {
		
	}

}
