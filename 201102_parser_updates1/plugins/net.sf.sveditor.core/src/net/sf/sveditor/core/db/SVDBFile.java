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


package net.sf.sveditor.core.db;

import java.io.File;

import net.sf.sveditor.core.db.persistence.DBFormatException;
import net.sf.sveditor.core.db.persistence.IDBReader;

public class SVDBFile extends SVDBScopeItem {
	private String						fFile;
	private long						fLastModified;
	
	public SVDBFile() {
		super("", SVDBItemType.File);
	}
	
	public SVDBFile(String file) {
		super(new File(file).getName(), SVDBItemType.File);
		fFile               = file;
		setLocation(new SVDBLocation(-1, -1));
	}

	public SVDBFile(String file, long lastModified) {
		this(file);
		
		fLastModified = lastModified;
		setLocation(new SVDBLocation(-1, -1));
	}

	public long getLastModified() {
		return fLastModified;
	}
	
	public void setLastModified(long lastModified) {
		fLastModified = lastModified;
	}
	
	public String getFilePath() {
		return fFile;
	}
	
	public void setFilePath(String file) {
		fFile = file;
	}
	
	public SVDBItemBase duplicate() {
		SVDBFile ret = new SVDBFile(fFile);
		
		ret.init(this);
		
		return ret;
	}
	
	public void init(SVDBItemBase other) {
		super.init(other);

		fFile               = ((SVDBFile)other).fFile;
		fLastModified = ((SVDBFile)other).fLastModified;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SVDBFile) {
			SVDBFile o = (SVDBFile)obj;

			if (fLastModified != o.fLastModified) {
				return false;
			}
			
			return (fFile.equals(o.fFile) &&
					super.equals(obj));
		}
		return false;
	}
}
