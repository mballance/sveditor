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


package net.sf.sveditor.core.db.index;

import java.io.File;

public class SVDBPersistenceDescriptor {
	private File					fDBFile;
	private String					fBaseLocation;
	
	public SVDBPersistenceDescriptor(File file, String base_location) {
		fDBFile = file;
		fBaseLocation = base_location;
	}
	
	public File getDBFile() {
		return fDBFile;
	}
	
	public String getBaseLocation() {
		return fBaseLocation;
	}
}
