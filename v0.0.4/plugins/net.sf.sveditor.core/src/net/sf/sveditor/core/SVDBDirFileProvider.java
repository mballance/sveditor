package net.sf.sveditor.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBFileFactory;

public class SVDBDirFileProvider implements ISVDBFileProvider {
	private List<String>			fPaths = new ArrayList<String>();
	private Map<File, SVDBFile>		fFileMap = new HashMap<File, SVDBFile>();
	
	public SVDBDirFileProvider() {
		
	}

	public void add_path(String path) {
		fPaths.add(path);
	}

	public SVDBFile getFile(String path) {
		for (String p : fPaths) {
			File f = new File(p, path);
	
			if (f.isFile()) {
				if (!fFileMap.containsKey(f)) {
					try {
						InputStream in = new FileInputStream(f); 
						SVDBFile s_f = SVDBFileFactory.createFile(
								in, f.getAbsolutePath(), this);
						in.close();
						
						fFileMap.put(f, s_f);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				return fFileMap.get(f);
			}
		}
		
		return null;
	}

}
