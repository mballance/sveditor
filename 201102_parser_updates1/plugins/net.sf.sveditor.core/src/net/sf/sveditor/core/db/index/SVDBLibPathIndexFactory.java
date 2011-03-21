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


package net.sf.sveditor.core.db.index;

import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;

import net.sf.sveditor.core.db.index.cache.ISVDBIndexCache;

public class SVDBLibPathIndexFactory implements ISVDBIndexFactory {
	
	public static final String			TYPE = "net.sf.sveditor.libIndex";

	public ISVDBIndex createSVDBIndex(
			String 					project_name, 
			String 					base_location,
			ISVDBIndexCache			cache,
			Map<String, Object>		config) {
		ISVDBFileSystemProvider fs_provider;
		
		if (base_location.startsWith("${workspace_loc}")) {
			fs_provider = new SVDBWSFileSystemProvider();
		} else {
			fs_provider = new SVDBFSFileSystemProvider();
		}
		
		ISVDBIndex index = new SVDBLibIndex(
				project_name, base_location, fs_provider, cache);
		
		SVDBIndexFactoryUtils.setBaseProperties(config, index);
		
		return index;
	}

}
