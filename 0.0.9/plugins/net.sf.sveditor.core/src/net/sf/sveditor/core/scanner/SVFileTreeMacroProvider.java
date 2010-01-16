package net.sf.sveditor.core.scanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBMacroDef;
import net.sf.sveditor.core.db.SVDBScopeItem;
import net.sf.sveditor.core.db.index.SVDBFileTree;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;

public class SVFileTreeMacroProvider implements IPreProcMacroProvider {
	private Map<String, SVDBMacroDef>		fMacroCache;
	private SVDBFileTree					fContext;
	private boolean							fFirstSearch;
	private int								fLastLineno;
	private LogHandle						fLog;
	
	public SVFileTreeMacroProvider(SVDBFileTree context) {
		fLog = LogFactory.getDefault().getLogHandle("SVFileTreeMacroProvider");
		
		fContext = context;
		fMacroCache = new HashMap<String, SVDBMacroDef>();
		fFirstSearch = true;
		fLastLineno  = 0;
	}

	public void addMacro(SVDBMacroDef macro) {
		if (fMacroCache.containsKey(macro.getName())) {
			fMacroCache.remove(macro.getName());
		}
		fMacroCache.put(macro.getName(), macro);
	}

	public void setMacro(String key, String value) {
		if (fMacroCache.containsKey(key)) {
			fMacroCache.get(key).setDef(value);
		} else {
			fMacroCache.put(key, new SVDBMacroDef(key, new ArrayList<String>(), value));
		}
	}

	public SVDBMacroDef findMacro(String name, int lineno) {
		if (fFirstSearch) {
			collectParentFileMacros();
			fFirstSearch = false;
		}
		if (fLastLineno < lineno) {
			collectThisFileMacros(lineno);
		}
		
		SVDBMacroDef m = fMacroCache.get(name);

		/*
		fLog.debug("findMacro(\"" + name + "\") => " + ((m != null)?"Defined":"Undefined") +
				" (" + fContext.getFilePath() + ")");
		 */
		
		return m; 
	}
	
	private void collectParentFileMacros() {
		List<SVDBFileTree> file_list = new ArrayList<SVDBFileTree>();
		
		SVDBFileTree ib = fContext;
		file_list.add(ib);
		while (ib.getIncludedByFiles().size() > 0) {
			ib = ib.getIncludedByFiles().get(0);
			file_list.add(ib);
		}
		
		for (int i=file_list.size()-1; i>0; i--) {
			SVDBFile this_file = file_list.get(i).getSVDBFile();
			SVDBFile next_file = file_list.get(i-1).getSVDBFile();
			fLog.enter("--> Processing file \"" + this_file.getName() + 
					"\" (next " + next_file.getName() + ")");
			
			collectMacroDefs(file_list.get(i), this_file, next_file);
			
			fLog.leave("<-- Processing file \"" + this_file.getName() + 
					"\" (next " + next_file.getName() + ")");
		}
	}
	
	private boolean collectMacroDefs(
			SVDBFileTree		file,
			SVDBScopeItem 		scope, 
			SVDBFile 			stop_pt) {
		for (SVDBItem it : scope.getItems()) {
			if (it.getType() == SVDBItemType.Macro) {
				/*
				fLog.debug("Adding macro \"" + it.getName() + "\"" +
						" (" + fContext.getFilePath() + ")");
				 */
				addMacro((SVDBMacroDef)it);
			} else if (it.getType() == SVDBItemType.Include) {
				if (stop_pt != null && stop_pt.getName().endsWith(it.getName())) {
					/*
					fLog.debug("Found stop point");
					 */
					return true;
				} else {
					// Look for the included file
					SVDBFileTree inc = null;
					for (SVDBFileTree inc_t : file.getIncludedFiles()) {
						if (inc_t.getFilePath().endsWith(it.getName())) {
							inc = inc_t;
							break;
						}
					}
					
					if (inc != null) {
						collectMacroDefs(inc, inc.getSVDBFile(), null);
					} else {
						fLog.error("Failed to find \"" + it.getName() + "\" in file-tree");
					}
				}
			} else if (it instanceof SVDBScopeItem) {
				if (collectMacroDefs(file, (SVDBScopeItem)it, stop_pt)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	private void collectThisFileMacros(int lineno) {
		
	}


}
