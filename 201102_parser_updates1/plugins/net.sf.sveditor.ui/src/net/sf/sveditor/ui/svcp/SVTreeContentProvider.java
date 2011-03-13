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


package net.sf.sveditor.ui.svcp;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.db.ISVDBChildItem;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.ISVDBScopeItem;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBTask;
import net.sf.sveditor.core.db.stmt.SVDBVarDeclStmt;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class SVTreeContentProvider implements ITreeContentProvider {
	
	public Object[] getChildren(Object elem) {
		if (elem instanceof ISVDBScopeItem &&
				!(elem instanceof SVDBTask)) {
			List<ISVDBItemBase> c = new ArrayList<ISVDBItemBase>();
			for (ISVDBItemBase it : ((ISVDBScopeItem)elem).getItems()) {
				if (it.getType() == SVDBItemType.VarDeclStmt) {
					c.addAll(((SVDBVarDeclStmt)it).getVarList());
				} else {
					c.add(it);
				}
			}
			return c.toArray();
		} else {
			return new Object[0];
		}
	}
	
	public Object getParent(Object element) {
		if (element instanceof ISVDBChildItem) {
			return ((ISVDBChildItem)element).getParent();
		} else {
			return null;
		}
	}
	
	public boolean hasChildren(Object element) {
		return (element instanceof ISVDBScopeItem && 
				!(element instanceof SVDBTask) &&
				((ISVDBScopeItem)element).getItems().size() > 0);
	}

	public Object[] getElements(Object element) {
		if (element instanceof ISVDBScopeItem) {
			return ((ISVDBScopeItem)element).getItems().toArray();
		} else {
			return new Object[0];
		}
	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
