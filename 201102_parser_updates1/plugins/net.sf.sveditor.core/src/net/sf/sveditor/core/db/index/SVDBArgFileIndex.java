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

import java.io.InputStream;
import java.util.Map.Entry;

import net.sf.sveditor.core.db.index.cache.ISVDBIndexCache;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.scanutils.ITextScanner;
import net.sf.sveditor.core.scanutils.InputStreamTextScanner;
import net.sf.sveditor.core.svf_scanner.SVFScanner;

import org.eclipse.core.runtime.IProgressMonitor;

public class SVDBArgFileIndex extends AbstractSVDBIndex {
	private long						fArgFileLastModified;
	
	public SVDBArgFileIndex(
			String						project,
			String						root,
			ISVDBFileSystemProvider		fs_provider,
			ISVDBIndexCache				cache) {
		super(project, root, fs_provider, cache);
		fLog = LogFactory.getLogHandle("SVDBArgFileIndex");
		
//		fIncludePaths 		= new ArrayList<String>();
	}
	
	public String getTypeID() {
		return SVDBArgFileIndexFactory.TYPE;
	}

	/*
	@Override
	public void dump(IDBWriter index_data) {
		try {
			// Save the last-modified time for the arg file
			long last_modified = fFileSystemProvider.getLastModifiedTime(getResolvedBaseLocation());
			index_data.writeLong(last_modified);
		} catch (DBWriteException e) {
			fLog.error("Problem while writing", e);
		}
		
		super.dump(index_data);
	}
	 */

	/*
	@Override
	public void load(
			IDBReader 		index_data, 
			List<SVDBFile> 	pp_files,
			List<SVDBFile> 	db_files) throws DBFormatException {
		fArgFileLastModified = index_data.readLong();
		fLoadUpToDate = true;
		
		fLog.debug("load - pp_files.size=" + pp_files.size() + " db_files.size=" + db_files.size());
		
		// Read back the Global Defines. Project settings will already
		// be set.  
		int n_defines = index_data.readInt();
		for (int i=0; i<n_defines; i++) {
			String key = index_data.readString();
			String val = index_data.readString();
			
			if (fGlobalDefines.containsKey(key) ||
					!fGlobalDefines.get(key).equals(val)) {
				fGlobalDefines.remove(key);
				fGlobalDefines.put(key, val);
				fLog.debug("Invalidating load, since key " + key + " changed value");
				fLoadUpToDate = false;
			}
		}

		// Load up file paths from the .f target 
		initPaths();

		load_base(index_data, pp_files, db_files);
		
		if (isLoaded()) {
			fLog.debug("Index is loaded... Loading markers and FileTreeMap");

			// re-build the FileTree structure
			for (String file : fFilePaths) {
				file = resolvePath(file);
				SVDBFile pp_file = findPreProcFile(file);
				
				fLog.debug("    Building FileTree for \"" + file + "\"");
				
				if (pp_file == null) {
					fLog.error("Failed to find pre-proc file \"" + file + "\"");
					continue;
				}
				
				SVDBFileTree ft_root = new SVDBFileTree((SVDBFile)pp_file.duplicate());
				buildPreProcFileMap(null, ft_root);
			}
			
			loadMarkers();
		} else {
			fLog.debug("Index is not loaded...");
		}
	}

	@Override
	protected boolean isLoadUpToDate() {
		fLog.debug("BaseLocation Exists: " + 
				fFileSystemProvider.fileExists(getResolvedBaseLocation()) + 
				" ArgFileLastModified (saved): " + fArgFileLastModified + 
				" ArgFileLastModified (current): " + fFileSystemProvider.getLastModifiedTime(getResolvedBaseLocation()));
		if (fFileSystemProvider.fileExists(getResolvedBaseLocation()) &&
				fArgFileLastModified >= fFileSystemProvider.getLastModifiedTime(getResolvedBaseLocation())) {
			return super.isLoadUpToDate();
		}
		return false;
	}
	
	@Override
	public void fileChanged(String path) {
		if (path.equals(getResolvedBaseLocation())) {
			rebuildIndex();
		} else {
			super.fileChanged(path);
		}
	}
	 */

/*
	@Override
	protected void buildPreProcFileMap() {
		initPaths();
		
		// Say the index is already valid
		fPreProcFileMapValid = true;
		
		for (String file : fFilePaths) {
			String r_file = resolvePath(file);
			fLog.debug("Resolved path for \"" + file + "\" is \"" + r_file + "\"");
			SVDBFile pp_file = processPreProcFile(r_file, true);
			
			if (pp_file == null) {
				fLog.error("Failed to find file \"" + r_file + "\"");
				return;
			}
			
			SVDBFileTree ft_root = new SVDBFileTree((SVDBFile)pp_file.duplicate());
			buildPreProcFileMap(null, ft_root);
		}
	}
 */
	
	@Override
	protected void discoverRootFiles(IProgressMonitor monitor) {
		clearFilesList();
		clearIncludePaths();
		clearDefines();
		
		monitor.beginTask("Discover Root Files", 4);
		
		// Add an include path for the arg file location
		addIncludePath(getResolvedBaseLocationDir());
		
		InputStream in = fFileSystemProvider.openStream(getResolvedBaseLocation());
		
		if (in != null) {
			ITextScanner sc = new InputStreamTextScanner(in, getResolvedBaseLocation());
			SVFScanner scanner = new SVFScanner();
		
			monitor.worked(1);
			try {
				scanner.scan(sc);
			} catch (Exception e) {
				fLog.error("Failed to read argument file \"" + 
						getResolvedBaseLocation() + "\"", e);
			}
			
			monitor.worked(1);
			for (String f : scanner.getFilePaths()) {
				String exp_f = SVDBIndexUtil.expandVars(f, true);
				fLog.debug("[FILE PATH] " + f + " (" + exp_f + ")");
				String res_f = resolvePath(exp_f);
				
				if (fFileSystemProvider.fileExists(res_f)) {
					addFile(res_f);
				} else {
					fLog.error("Expanded path \"" + exp_f + "\" does not exist");
				}
			}
			
			monitor.worked(1);
			for (String inc : scanner.getIncludePaths()) {
				String path = SVDBIndexUtil.expandVars(inc, true);
				fLog.debug("[INC PATH] " + inc + " (" + path + ")");
				
				addIncludePath(path);
			}
			
			monitor.worked(1);
			for (Entry<String, String> entry : scanner.getDefineMap().entrySet()) {
				fLog.debug("[DEFINE] " + entry.getKey() + "=" + entry.getValue());
				addDefine(entry.getKey(), entry.getValue());
			}
			
			fFileSystemProvider.closeStream(in);
			monitor.done();
		} else {
			monitor.done();
			fLog.error("failed to open file \"" + getResolvedBaseLocation() + "\"");
		}
	}



/*	
	@Override
	protected void buildIndex(IProgressMonitor monitor) {
		getPreProcFileMap(monitor); // force pre-proc info to be built

		SubProgressMonitor sub_monitor = new SubProgressMonitor(monitor, 1);
		sub_monitor.beginTask("Processing Top-Level Files", fFilePaths.size());
		for (String file : fFilePaths) {
			file = resolvePath(file);
			SVDBFile pp_file = findPreProcFile(file);
			
			if (pp_file == null) {
				fLog.error("Failed to find file \"" + file + "\"");
				return;
			}
			
			SVDBFileTree ft_root = fFileTreeMap.get(file);
			IPreProcMacroProvider mp = createMacroProvider(ft_root);
			
			sub_monitor.subTask("Processing " + file);
			processFile(ft_root, mp);
			sub_monitor.worked(1);
		}
		
		fIndexFileMapValid = true;
		
		signalIndexRebuilt();
	}
 */	
	
/*	
	@Override
	public SVDBSearchResult<SVDBFile> findIncludedFile(String path) {
		SVDBSearchResult<SVDBFile> ret = null;
		
		if ((ret = super.findIncludedFile(path)) != null) {
			return ret;
		}
		
		// Otherwise, search through each include path
		for (String inc : fIncludePaths) {
			inc = inc + "/" + path;
			inc = resolvePath(inc);
			
			if (fFileSystemProvider.fileExists(inc)) {
				SVDBFile pp_file = processPreProcFile(inc, true);
				
				ret = new SVDBSearchResult<SVDBFile>(pp_file, this);
				break;
			}
		}
		
		return ret;
	}
 */	

}
