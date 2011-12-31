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


package net.sf.sveditor.core.srcgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.ISVDBNamedItem;
import net.sf.sveditor.core.db.SVDBClassDecl;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBTask;
import net.sf.sveditor.core.db.index.ISVDBIndexIterator;
import net.sf.sveditor.core.db.search.SVDBFindDefaultNameMatcher;
import net.sf.sveditor.core.db.search.SVDBFindSuperClass;

public class OverrideMethodsFinder {
	
	private SVDBClassDecl									fLeafClass;
	private Map<SVDBClassDecl, List<SVDBTask>>		fClassMap;
	private ISVDBIndexIterator								fIndexIt;

	/*
	private class ClassComparator implements Comparator<SVDBModIfcClassDecl> {
		public int compare(SVDBModIfcClassDecl c1, SVDBModIfcClassDecl c2) {
			
			if (c1.getSuperClass() != null && 
					c1.getSuperClass().equals(c2.getSuperClass())) {
				return 1;
			} else {
				return -1;
			}
		}
	}
	 */
	
	public OverrideMethodsFinder(SVDBClassDecl leaf_class, ISVDBIndexIterator index_it) {
		fLeafClass = leaf_class;
		fClassMap = new HashMap<SVDBClassDecl, List<SVDBTask>>();
		fIndexIt = index_it;
		
		findClasses();
	}
	
	public Set<SVDBClassDecl> getClassSet() {
		return fClassMap.keySet();
	}
	
	public List<SVDBTask> getMethods(SVDBClassDecl cls) {
		return fClassMap.get(cls);
	}
	
	private void findClasses() {
		fClassMap.clear();
		SVDBClassDecl cl = fLeafClass;
		SVDBFindSuperClass  finder_super = new SVDBFindSuperClass(
				fIndexIt, SVDBFindDefaultNameMatcher.getDefault());

		while (cl != null) {
			
			cl = finder_super.find(cl);
			
			if (cl != null) {
				List<SVDBTask> overrides = getClassOverrideTargets(cl);
				if (overrides.size() > 0) {
					fClassMap.put(cl, getClassOverrideTargets(cl));
				}
			}
		}
	}

	private List<SVDBTask> getClassOverrideTargets(SVDBClassDecl cls) {
		List<SVDBTask> ret = new ArrayList<SVDBTask>();
		
		for (ISVDBItemBase it : cls.getChildren()) {
			if (it.getType() == SVDBItemType.Function ||
					it.getType() == SVDBItemType.Task) {
				SVDBTask tf = (SVDBTask)it;
				if ((tf.getAttr() & SVDBTask.FieldAttr_Local) == 0) {
					if (!existsInClass(it, fLeafClass)) {
						ret.add(tf);
					}
				}
			}
		}
		
		return ret;
	}

	private boolean existsInClass(ISVDBItemBase it, SVDBClassDecl cls) {
		
		for (ISVDBItemBase it_t : cls.getChildren()) {
			if (it instanceof ISVDBNamedItem && it_t instanceof ISVDBNamedItem && 
					((ISVDBNamedItem)it_t).getName().equals(
							((ISVDBNamedItem)it).getName())) {
				return true;
			}
		}
		
		return false;
	}

}
