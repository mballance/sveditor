package net.sf.sveditor.core.preproc;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import net.sf.sveditor.core.db.SVDBFileTree;
import net.sf.sveditor.core.db.SVDBLocation;
import net.sf.sveditor.core.db.SVDBMacroDef;
import net.sf.sveditor.core.db.SVDBUnprocessedRegion;
import net.sf.sveditor.core.scanutils.ScanLocation;

public class SVPreProc2InputData {

	private SVPreProcessor2		fPreProc;
	private InputStream 		fInput;
	private String 				fFilename;
	private int 				fFileId;
	private int 				fLineno;
	private int 				fLinepos;
	private int 				fLineCount;
	private int 				fLastCh;
	private int 				fUngetCh1;
	private int 				fUngetCh2;
	private boolean 			fEof;
	private boolean 			fIncPos;
	private Map<String, String> fRefMacros;
	private SVDBFileTree 		fFileTree;
	private SVDBUnprocessedRegion fUnprocessedRegion;

	SVPreProc2InputData(
			SVPreProcessor2		preproc,
			InputStream 		in, 
			String 				filename, 
			int 				file_id) {
		this(preproc, in, filename, file_id, true);
	}
	
	SVPreProc2InputData(
			SVPreProcessor2		preproc,
			InputStream 		in, 
			String 				filename, 
			int 				file_id, 
			boolean 			inc_pos) {
		fPreProc = preproc;
		fLineno = 1;
		fInput = in;
		fFilename = filename;
		fFileId   = file_id;
		fLastCh = -1;
		fEof = false;
		fIncPos = inc_pos;
		fRefMacros = new HashMap<String, String>();
		fUngetCh1 = -1;
		fUngetCh2 = -1;
	}
	
	int get_ch() {
		int ch = -1;
		
		if (fUngetCh1 != -1) {
			ch = fUngetCh1;
			fUngetCh1 = fUngetCh2;
			fUngetCh2 = -1;
		} else {
			try {
				ch = fInput.read();
			} catch (IOException e) {}
		}
	
		if (ch != -1) {
			if (fLastCh == '\n') {
				if (fIncPos) {
					// Save a marker for the line in the line map
					fLineno++;
					if (fPreProc != null) {
						fPreProc.add_file_change_info(fFileId, fLineno);
					}
				}
				fLineCount++;
			}
			fLastCh = ch;
		}
		
		return ch;
	}
	
	void unget_ch(int ch) {
		if (fUngetCh1 == -1) {
			fUngetCh1 = ch;
		} else {
			fUngetCh2 = fUngetCh1;
			fUngetCh1 = ch;
		}
	}
	
	SVDBFileTree getFileTree() {
		return fFileTree;
	}
	
	void setFileTree(SVDBFileTree ft) {
		fFileTree = ft;
	}
	
	InputStream getInput() {
		return fInput;
	}
	
	int getFileId() {
		return fFileId;
	}
	
	int getLineNo() {
		return fLineno;
	}
	
	String getFileName() {
		return fFilename;
	}
	
	int getLineCount() {
		return fLineCount;
	}
	
	void close() {
		try {
			if (fInput != null) {
				fInput.close();
			}
		} catch (IOException e) {}
	}
	
	ScanLocation getLocation() {
		return new ScanLocation(fFileId, fLineno, fLinepos);
	}
	
	void addRefMacro(String name, SVDBMacroDef m) {
		fRefMacros.remove(name);
		if (m == null) {
			fRefMacros.put(name, null);
		} else {
			fRefMacros.put(name, m.getDef());
		}		
	}
	
	void addReferencedMacro(String macro, SVDBMacroDef def) {
		fFileTree.fReferencedMacros.remove(macro);
		if (def == null) {
			fFileTree.fReferencedMacros.put(macro, null);
		} else {
			fFileTree.fReferencedMacros.put(macro, def.getDef());
		}		
	}
	
	void update_unprocessed_region(ScanLocation scan_loc, boolean enabled_pre, boolean enabled_post) {
		if (enabled_pre && !enabled_post) {
			// Entering an unprocessed region
			SVDBLocation loc = new SVDBLocation(scan_loc.getFileId(), 
					scan_loc.getLineNo(), scan_loc.getLinePos());
		
			fUnprocessedRegion = new SVDBUnprocessedRegion();
			fUnprocessedRegion.setLocation(loc);
		} else if (!enabled_pre && enabled_post) {
			// Leaving an unprocessed region
			SVDBLocation loc = new SVDBLocation(scan_loc.getFileId(), 
					scan_loc.getLineNo(), scan_loc.getLinePos());
		
			SVDBUnprocessedRegion r = fUnprocessedRegion;
			fUnprocessedRegion = null;
			
			r.setEndLocation(loc);
			fFileTree.getSVDBFile().addChildItem(r);
		}		
	}
	
	void leave_file() {
		if (fUnprocessedRegion != null) {
			// TODO: mark error
			// we fell off the end of the file with an ifdef active
			ScanLocation scan_loc = getLocation();
			SVDBLocation loc = new SVDBLocation(scan_loc.getFileId(), 
					scan_loc.getLineNo(), scan_loc.getLinePos());
			fUnprocessedRegion.setEndLocation(loc);
			fFileTree.getSVDBFile().addChildItem(fUnprocessedRegion);
		}
		
		close();
	}
}
