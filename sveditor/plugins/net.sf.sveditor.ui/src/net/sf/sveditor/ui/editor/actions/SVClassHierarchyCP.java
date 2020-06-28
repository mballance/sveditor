/****************************************************************************
 * Copyright (c) 2008-2014 Matthew Ballance and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *
 * Contributors:
 *     Matthew Ballance - initial implementation
 ****************************************************************************/


package net.sf.sveditor.ui.editor.actions;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.db.ISVDBChildItem;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.SVDBClassDecl;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBModIfcDecl;
import net.sf.sveditor.core.db.utils.SVDBIndexSearcher;
import net.sf.sveditor.core.db.utils.SVDBSearchUtils;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class SVClassHierarchyCP implements ITreeContentProvider {
	
	private SVDBIndexSearcher				fIndexSearcher;
	private SVDBClassDecl					fLeafClass;
	private Object							fEmptyList[] = new Object[0];
	
	public SVClassHierarchyCP(
			SVDBClassDecl			leaf_class,
			SVDBIndexSearcher		index_searcher) {
		fLeafClass = leaf_class;
		fIndexSearcher = index_searcher;
	}

	public Object[] getElements(Object inputElement) {
		List<SVDBClassDecl> ret = new ArrayList<SVDBClassDecl>();

		SVDBClassDecl cl = fLeafClass;

		while (cl != null) {
			cl = fIndexSearcher.findSuperClass(cl);
			
			if (cl != null) {
				ret.add(cl);
			}
		}

		return ret.toArray();
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof SVDBClassDecl) {
			List<ISVDBItemBase> ret = SVDBSearchUtils.findItemsByType(
					(SVDBModIfcDecl)parentElement,
					SVDBItemType.Function, SVDBItemType.Task);
			
			return ret.toArray();
		} else {
			return fEmptyList;
		}
	}
	

	public Object getParent(Object element) {
		return ((ISVDBChildItem)element).getParent();
	}

	public boolean hasChildren(Object element) {
		return (getChildren(element).length > 0);
	}


	public void dispose() {}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

}
