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


package net.sf.sveditor.core.db.search;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.db.ISVDBChildItem;
import net.sf.sveditor.core.db.ISVDBChildParent;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.ISVDBNamedItem;
import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBModIfcDecl;
import net.sf.sveditor.core.db.SVDBTask;
import net.sf.sveditor.core.db.index.ISVDBIndexIterator;
import net.sf.sveditor.core.db.stmt.SVDBParamPortDecl;
import net.sf.sveditor.core.db.stmt.SVDBVarDeclItem;
import net.sf.sveditor.core.db.stmt.SVDBVarDeclStmt;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;

public class SVDBFindByNameInScopes {
	private ISVDBFindNameMatcher			fMatcher;
	private LogHandle						fLog;
	
	public SVDBFindByNameInScopes(ISVDBIndexIterator index_it) {
		fMatcher = SVDBFindDefaultNameMatcher.getDefault();
		fLog = LogFactory.getLogHandle("SVDBFindByNameInScopes");
	}
	
	public SVDBFindByNameInScopes(ISVDBIndexIterator index_it, ISVDBFindNameMatcher matcher) {
		fMatcher = matcher;
		fLog = LogFactory.getLogHandle("SVDBFindByNameInScopes");
	}
	
	public List<ISVDBItemBase> find(
			ISVDBChildItem			context,
			String					name,
			boolean					stop_on_first_match,
			SVDBItemType	...		types) {
		List<ISVDBItemBase> ret = new ArrayList<ISVDBItemBase>();
		fLog.debug("--> find: context=" + ((context!=null)?SVDBItem.getName(context):"null") + " name=" + name);

		// Search up the scope
		while (context != null && context instanceof ISVDBChildParent) {
			
			// First, search the local variables
			for (ISVDBItemBase it : ((ISVDBChildParent)context).getChildren()) {
				if (it instanceof SVDBVarDeclStmt) {
					for (ISVDBItemBase it_t : ((SVDBVarDeclStmt)it).getChildren()) {
						if (it_t instanceof ISVDBNamedItem && fMatcher.match((ISVDBNamedItem)it_t, name)) {
							boolean match = (types.length == 0);

							for (SVDBItemType t : types) {
								if (it_t.getType() == t) {
									match = true;
									break;
								}
							}
							
							if (match) {
								ret.add(it_t);
								
								if (stop_on_first_match) {
									break;
								}
							}
						}
					}
				} else {
					if (it instanceof ISVDBNamedItem && fMatcher.match((ISVDBNamedItem)it, name)) {
						boolean match = (types.length == 0);

						for (SVDBItemType t : types) {
							if (it.getType() == t) {
								match = true;
								break;
							}
						}
						
						if (match) {
							ret.add(it);
							
							if (stop_on_first_match) {
								break;
							}
						}
					}
				}
			}
			
			if (ret.size() > 0 && stop_on_first_match) {
				break;
			}
			
			// Next, search the parameters, if we're in a function/task scope
			if (context.getType().isElemOf(SVDBItemType.Function, SVDBItemType.Task)) {
				for (SVDBParamPortDecl p : ((SVDBTask)context).getParams()) {
					for (ISVDBChildItem pi : p.getChildren()) {
						fLog.debug("check param \"" + SVDBItem.getName(pi) + "\"");
						if (SVDBItem.getName(pi).equals(name)) {
							ret.add(pi);
						
							if (stop_on_first_match) {
								break;
							}
						}
					}
					if (ret.size() > 0 && stop_on_first_match) {
						break;
					}
				}
			}

			if (ret.size() > 0 && stop_on_first_match) {
				break;
			}
			
			// Next, if we check the class parameters if we're in a class scope,
			// or the module ports/parameters if we're in an interface/module scope
			if (context.getType() == SVDBItemType.ClassDecl) {
//				SVDBClassDecl cls = (SVDBClassDecl)context;
				
			} else if (context.getType() == SVDBItemType.ModuleDecl ||
					context.getType() == SVDBItemType.InterfaceDecl) {
				List<SVDBParamPortDecl> p_list = ((SVDBModIfcDecl)context).getPorts();

				// Check ports
				for (SVDBParamPortDecl p : p_list) {
					for (ISVDBChildItem c : p.getChildren()) {
						SVDBVarDeclItem pi = (SVDBVarDeclItem)c;
						if (pi.getName().equals(name)) {
							ret.add(pi);
							if (ret.size() > 0 && stop_on_first_match) {
								break;
							}
						}
						if (ret.size() > 0 && stop_on_first_match) {
							break;
						}
					}
				}
			}

			if (ret.size() > 0 && stop_on_first_match) {
				break;
			}

			context = context.getParent();
		}

		fLog.debug("<-- find: context=" + ((context!=null)?SVDBItem.getName(context):"null") + " name=" + name);

		return ret;
	}

}
