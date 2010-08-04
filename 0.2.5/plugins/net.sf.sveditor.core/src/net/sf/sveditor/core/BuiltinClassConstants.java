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


package net.sf.sveditor.core;

import java.util.HashMap;
import java.util.Map;

import net.sf.sveditor.core.db.SVDBItemType;


public class BuiltinClassConstants {
	
	public static final String Covergroup		= "__sv_builtin_covergroup";
	public static final String Coverpoint		= "__sv_builtin_coverpoint";
	public static final String CoverpointCross	= "__sv_builtin_coverpoint_cross";

	private static final Map<SVDBItemType, String>		fBaseClassMap;
	
	static {
		fBaseClassMap = new HashMap<SVDBItemType, String>();
		fBaseClassMap.put(SVDBItemType.Covergroup, Covergroup);
		fBaseClassMap.put(SVDBItemType.Coverpoint, Coverpoint);
	}
	
	public static boolean hasBuiltin(SVDBItemType type) {
		return fBaseClassMap.containsKey(type);
	}
	
	public static String getBuiltinClass(SVDBItemType type) {
		return fBaseClassMap.get(type);
	}

}
