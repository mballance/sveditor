/****************************************************************************
 * Copyright (c) 2008-2011 Matthew Ballance and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Ballance - initial implementation
 ****************************************************************************/


package net.sf.sveditor.core.db.stmt;

import net.sf.sveditor.core.db.SVDBItemType;

public class SVDBForkStmt extends SVDBBlockStmt {
	public enum JoinType {
		Join,
		JoinNone,
		JoinAny
	};
	
	public JoinType					fJoinType;
	
	public SVDBForkStmt() {
		super(SVDBItemType.ForkStmt);
	}
	
	public JoinType getJoinType() {
		return fJoinType;
	}
	
	public void setJoinType(JoinType join_type) {
		fJoinType = join_type;
	}

}
