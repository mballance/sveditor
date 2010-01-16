package net.sf.sveditor.core.fileset;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SVFilesystemFileMatcher extends AbstractSVFileMatcher {
	
	public SVFilesystemFileMatcher() {
	}

	@Override
	public List<String> findIncludedPaths() {
		List<String> ret = new ArrayList<String>();
		
		for (SVFileSet fs : fFileSets) {
			File base = new File(fs.getBase());
			
			findIncludedPaths(fs.getBase(), ret, base);
		}
		
		return ret;
	}
	
	private void findIncludedPaths(String base, List<String> paths, File parent) {
		if (parent.isFile()) {
			if (include_file(parent.getAbsolutePath())) {
				paths.add(parent.getAbsolutePath());
			}
		} else {
			if (parent == null) {
				System.out.println("parent is null");
			} else if (parent.listFiles() == null) {
				System.out.println("parent \"" + parent.getPath() + "\" returns null");
			}
			for (File file : parent.listFiles()) {
				if (file.isDirectory()) {
					findIncludedPaths(base, paths, file);
				} else {
					if (include_file(file.getAbsolutePath())) {
						paths.add(file.getAbsolutePath());
					}
				}
			}
		}
	}
}
