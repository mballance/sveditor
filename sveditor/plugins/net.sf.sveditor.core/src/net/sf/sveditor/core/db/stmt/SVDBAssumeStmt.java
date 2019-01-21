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

import net.sf.sveditor.core.db.ISVDBVisitor;
import net.sf.sveditor.core.db.SVDBItemType;

public class SVDBAssumeStmt extends SVDBAssertStmt {
	
	public SVDBAssumeStmt() {
		super(SVDBItemType.AssumeStmt);
	}

	@Override
	public void accept(ISVDBVisitor v) {
		v.visit_assume_stmt(this);
	}
}
