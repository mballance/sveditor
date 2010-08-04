package net.sf.sveditor.core.tests;

import java.io.InputStream;

import net.sf.sveditor.core.StringInputStream;
import net.sf.sveditor.core.db.index.ISVDBFileSystemChangeListener;
import net.sf.sveditor.core.db.index.ISVDBFileSystemProvider;
import net.sf.sveditor.core.db.index.SVDBLibIndex;

public class SVDBStringDocumentIndex extends SVDBLibIndex {
	
	public SVDBStringDocumentIndex(final String input) {
		super("STUB", "ROOT", new ISVDBFileSystemProvider() {
			public InputStream openStream(String path) {
				if (path.equals("ROOT")) {
					return new StringInputStream(input);
				} else {
					return null;
				}
			}
			public boolean fileExists(String path) {
				return path.equals("ROOT");
			}
			
			public void init(String root) {}
			public long getLastModifiedTime(String path) {return 0;}
			public String resolvePath(String path) {return path;}
			public void removeFileSystemChangeListener(ISVDBFileSystemChangeListener l) {}
			public void dispose() {}
			public void closeStream(InputStream in) {}
			public void clearMarkers(String path) {}
			public void addMarker(String path, String type, int lineno, String msg) {}
			public void addFileSystemChangeListener(ISVDBFileSystemChangeListener l) {}
		});
	}
}
