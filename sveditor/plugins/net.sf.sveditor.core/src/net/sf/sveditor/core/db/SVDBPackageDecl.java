/****************************************************************************
 * Copyright (c) 2008-2014 Matthew Ballance and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Ballance - initial implementation
 ****************************************************************************/


package net.sf.sveditor.core.db;


public class SVDBPackageDecl extends SVDBScopeItem {

	public SVDBPackageDecl() {
		super("", SVDBItemType.PackageDecl);
	}
	
	public SVDBPackageDecl(String name) {
		super(name, SVDBItemType.PackageDecl);
	}

	@Override
	public void accept(ISVDBVisitor v) {
		v.visit_package_decl(this);
	}
	
	
	
}
