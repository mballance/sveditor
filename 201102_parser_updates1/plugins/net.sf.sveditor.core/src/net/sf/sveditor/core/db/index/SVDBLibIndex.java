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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.SVFileUtils;
import net.sf.sveditor.core.db.ISVDBChildItem;
import net.sf.sveditor.core.db.ISVDBFileFactory;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.ISVDBNamedItem;
import net.sf.sveditor.core.db.ISVDBScopeItem;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBFileMerger;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBMarker;
import net.sf.sveditor.core.db.SVDBPreProcObserver;
import net.sf.sveditor.core.db.persistence.DBFormatException;
import net.sf.sveditor.core.db.persistence.IDBReader;
import net.sf.sveditor.core.db.search.SVDBSearchResult;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.scanner.IPreProcMacroProvider;
import net.sf.sveditor.core.scanner.SVPreProcDefineProvider;
import net.sf.sveditor.core.scanner.SVPreProcScanner;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

public class SVDBLibIndex extends AbstractSVDBIndex implements ISVDBFileSystemChangeListener {
	protected Map<String, SVDBFileTree>				fFileTreeMap;
	protected String								fRoot;
	protected String								fResolvedRoot;
	protected SVPreProcDefineProvider				fDefineProvider;
	protected List<String>							fIncludePaths;
	protected boolean								fBaseLocationAndDirAreSame;
	private String									fBaseLocationDir;
	
	public SVDBLibIndex(
			String 					project, 
			String 					root,
			ISVDBFileSystemProvider fs_provider) {
		super(project, null);
		
		fDefineProvider = new SVPreProcDefineProvider(null);
		fFileTreeMap 	= new HashMap<String, SVDBFileTree>();
		fRoot 			= root;
		fResolvedRoot	= SVDBIndexUtil.expandVars(fRoot, true);
		fLog = LogFactory.getLogHandle("SVDBLibIndex");
		
		fIncludePaths = new ArrayList<String>();
		
		setFileSystemProvider(fs_provider);
		fBaseLocationAndDirAreSame = false;		
	}
	
	public String getTypeID() {
		return SVDBLibPathIndexFactory.TYPE;
	}
	
	public String getTypeName() {
		return "LibraryIndex";
	}
	
	protected String getResolvedBaseLocationDir() {
		if (fBaseLocationDir == null) {
			if (fBaseLocationAndDirAreSame) {
				fBaseLocationDir = getResolvedBaseLocation();
			} else {
				fBaseLocationDir = SVFileUtils.getPathParent(getResolvedBaseLocation());
			}
		}
		return fBaseLocationDir;
	}
	
	protected void initPaths() {
		// Add an include path for the base directory
		fIncludePaths.clear();
		if (fFileSystemProvider.fileExists(getResolvedBaseLocation())) {
			fIncludePaths.add(SVFileUtils.getPathParent(getResolvedBaseLocation()));
		}
	}
	
	@Override
	public void setFileSystemProvider(ISVDBFileSystemProvider fsProvider) {
		if (fFileSystemProvider != null) {
			fFileSystemProvider.removeFileSystemChangeListener(this);
		}
		super.setFileSystemProvider(fsProvider);
		
		if (fFileSystemProvider != null) {
			fFileSystemProvider.init(getResolvedBaseLocation());
			fFileSystemProvider.addFileSystemChangeListener(this);
		}
	}

	public Map<String, SVDBFileTree> getFileTreeMap(IProgressMonitor monitor) {
		getPreProcFileMap(monitor); // Ensure the map is built
		return fFileTreeMap;
	}

	public String getBaseLocation() {
		return fRoot;
	}
	
	public String getResolvedBaseLocation() {
		if (fResolvedRoot == null) {
			fResolvedRoot = SVDBIndexUtil.expandVars(fRoot, true);
		}
		
		return fResolvedRoot;
	}
	
	public void rebuildIndex() {
		fLog.debug("rebuildIndex \"" + getBaseLocation() + "\"");
		
		if (fFileSystemProvider != null) {
			for (SVDBFile file : fPreProcFileMap.values()) {
				fFileSystemProvider.clearMarkers(file.getFilePath());
			}
		}
		
		fIndexFileMap.clear();
		fPreProcFileMap.clear();
		fFileTreeMap.clear();
		
		fIndexFileMapValid = false;
		fPreProcFileMapValid  = false;
	}
	
	public void load(
			IDBReader index_data, 
			List<SVDBFile> ppFiles, 
			List<SVDBFile> dbFiles) throws DBFormatException {
		fLoadUpToDate = true;
		fLog.debug("load - ppFiles.size=" + ppFiles.size() + " dbFiles.size=" + dbFiles.size());
		
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
		
		initPaths();
		
		load_base(index_data, ppFiles, dbFiles);
		
		if (isLoaded()) {
			fLog.debug("Index is loaded... Loading markers and FileTreeMap");
			loadMarkers();
			
			SVDBFile pp_file = findPreProcFile(getResolvedBaseLocation());
			SVDBFileTree ft_root = new SVDBFileTree((SVDBFile)pp_file.duplicate());
			buildPreProcFileMap(null, ft_root);
		} else {
			fLog.debug("Index is not loaded...");
		}
	}
	
	// Load markers from the FileIndexMap
	protected void loadMarkers() {
		
		// Add in the markers
		for (SVDBFile f : fIndexFileMap.values()) {
			List<SVDBMarker> marker_list = null;
			
			for (ISVDBItemBase it : f.getItems()) {
				if (it.getType() == SVDBItemType.Marker) {
					if (marker_list == null) {
						marker_list = new ArrayList<SVDBMarker>();
					}
					marker_list.add((SVDBMarker)it);
				}
			}
		}
	}
	
	/**
	 * Add markers to the specified file path from the specified
	 * SVDBFile
	 * 
	 * @param path
	 * @param file
	 */
	protected void addMarkers(String path, SVDBFile file) {
		fLog.debug("addMarkers: " + path);
		for (ISVDBItemBase it : file.getItems()) {
			if (it.getType() == SVDBItemType.Marker) {
				SVDBMarker m = (SVDBMarker)it;
				String type = (m.getName().equals(SVDBMarker.MARKER_ERR))?
						ISVDBFileSystemProvider.MARKER_TYPE_ERROR:
						ISVDBFileSystemProvider.MARKER_TYPE_WARNING;
				String msg = m.getMessage();
				msg += " in " + getTypeName() + " index " + getBaseLocation();
				fFileSystemProvider.addMarker(path, 
						type, m.getLocation().getLine(), msg); 
			}
		}
	}

	/**
	 * Propagates markers from the PreProcFile and the FileTree data structure
	 * to the specified DB object (typically the Index DB)
	 * @param db_file
	 */
	protected void propagateMarkersPreProc2DB(
			SVDBFileTree	ft,
			SVDBFile		svdb_pp,
			SVDBFile 		db_file) {
		String path_s = db_file.getFilePath();
		
		for (ISVDBItemBase it : svdb_pp.getItems()) {
			if (it.getType() == SVDBItemType.Marker) {
				fLog.debug("Propagate marker: " + ((SVDBMarker)it).getMessage());
				if (it instanceof ISVDBChildItem) {
					db_file.addItem((ISVDBChildItem)it);
				}
			}
		}
		
		for (ISVDBItemBase it : ft.getSVDBFile().getItems()) {
			if (it.getType() == SVDBItemType.Marker) {
				SVDBMarker m = (SVDBMarker)it;
				String type = (m.getName().equals(SVDBMarker.MARKER_ERR))?
						ISVDBFileSystemProvider.MARKER_TYPE_ERROR:
						ISVDBFileSystemProvider.MARKER_TYPE_WARNING;
				String msg = m.getMessage() + " in " +
					getTypeName() + " " + getBaseLocation();
				fFileSystemProvider.addMarker(path_s, 
						type, m.getLocation().getLine(), msg);
			}
		}
	}

	/**
	 * findIncludedFile()
	 * 
	 * Search the include paths within this index
	 */
	public SVDBSearchResult<SVDBFile> findIncludedFile(String path) {
		
		for (String inc_dir : fIncludePaths) {
			String inc_path = inc_dir + "/" + path;

			fLog.debug("Include Path: \"" + inc_path + "\"");

			Map<String, SVDBFile> pp_map = fPreProcFileMap; // FileMap in progress

			if (pp_map.containsKey(inc_path)) {
				fLog.debug("findIncludedFile: \"" + inc_path + "\" already in map");
				return new SVDBSearchResult<SVDBFile>(pp_map.get(inc_path), this);
			} else {
				SVDBFile pp_file = null;

				if (fFileSystemProvider.fileExists(inc_path)) {
					fLog.debug("findIncludedFile: building entry for \"" + inc_path + "\"");

					pp_file = processPreProcFile(inc_path, true);
				} else {
					fLog.debug("findIncludedFile: file \"" + inc_path + "\" does not exist");
				}

				if (pp_file != null) {
					return new SVDBSearchResult<SVDBFile>(pp_file, this);
				}
			}
		}
		
		String res_path = resolvePath(path);
		
		if (fFileSystemProvider.fileExists(res_path)) {
			SVDBFile pp_file = null;
			if ((pp_file = processPreProcFile(res_path, true)) != null) {
				return new SVDBSearchResult<SVDBFile>(pp_file, this);
			}
		}
		
		
		return null;
	}
	
	@Override
	protected boolean isLoadUpToDate() {
		
		// Now, iterate through and check lastModified timestamps
		for (SVDBFile svdb_f : fPreProcFileMap.values()) {
			String path = svdb_f.getFilePath();
			
			if (!fFileSystemProvider.fileExists(path) ||  
					(svdb_f.getLastModified() != fFileSystemProvider.getLastModifiedTime(path))) {
				debug("    file \"" + path + "\": saved timestamp: " +
						svdb_f.getLastModified() + " ; current timestamp: " + 
						fFileSystemProvider.getLastModifiedTime(path));
				return false;
			}
		}
		
		// Now, collect all missing include files. Rebuild if any exist
		Map<SVDBFile, List<String>> missing_inc = find_missing_inc_svdb();
		for (Entry<SVDBFile, List<String>> entry : missing_inc.entrySet()) {
			for (String inc : entry.getValue()) {
				SVDBSearchResult<SVDBFile> inc_svdb = findIncludedFile(inc);
				
				if (inc_svdb != null) {
					// Found the include, so rebuild
					return false;
				}
			}
		}
		
		return true;
	}
	
	public SVDBFile parse(InputStream in, String path, IProgressMonitor monitor) {
		
		ISVDBFileFactory factory = SVCorePlugin.createFileFactory(fDefineProvider);

		path = SVFileUtils.normalize(path);

		InputStreamCopier copier = new InputStreamCopier(in);
		in = null;

		// Ensure database is built
		getFileDB(monitor);

		SVDBFileTree file_tree = fFileTreeMap.get(path);

		if (file_tree == null) {
			// First, see if the file actually exists
			if (getFileSystemProvider().fileExists(path)) {
				// invalidate the index
				rebuildIndex();
				
				// Ensure database is built
				getFileDB(monitor);
				getFileTreeMap(monitor);
				
				file_tree = fFileTreeMap.get(path);
			} else {
				fLog.error("parse: File \"" + path + "\" not in FileTreeMap of " + getResolvedBaseLocation() + " and does not exist");
				return null;
			}
			
			if (file_tree == null) {
				fLog.error("parse: File \"" + path + "\" not in FileTreeMap of " + getResolvedBaseLocation());
				for (SVDBFileTree ft : fFileTreeMap.values()) {
					fLog.error("    " + ft.getFilePath());
				}
				return null;
			}
		}

		SVPreProcScanner 	sc = new SVPreProcScanner();
		SVDBPreProcObserver ob = new SVDBPreProcObserver();
		sc.setObserver(ob);

		file_tree = file_tree.duplicate();

		sc.init(copier.copy(), path);
		sc.scan();

		SVDBFile svdb_pp = ob.getFiles().get(0);

		fLog.debug("Processed pre-proc file");

		fFileSystemProvider.clearMarkers(file_tree.getFilePath());
		file_tree.setSVDBFile(svdb_pp);
		addIncludeFiles(file_tree, file_tree.getSVDBFile());
		
		fDefineProvider.setMacroProvider(createMacroProvider(file_tree));
		SVDBFile svdb_f = factory.parse(copier.copy(), file_tree.getFilePath());
		svdb_f.setLastModified(fFileSystemProvider.getLastModifiedTime(path));

		propagateMarkersPreProc2DB(file_tree, svdb_pp, svdb_f);
		addMarkers(path, svdb_f);

		return svdb_f;
	}
	
	/**
	 * buildPreProcFileMap()
	 * 
	 * Creating the pre-processor map requires that we build the
	 * 
	 */
	@Override
	protected void buildPreProcFileMap() {
		fLog.debug("buildPreProcFileMap()");
		
		initPaths();
		
		// Say the index is valid for now
		fPreProcFileMapValid = true;

		SVDBFile pp_file = processPreProcFile(getResolvedBaseLocation(), true);

		if (pp_file == null) {
			fLog.error("buildPreProcFileMap: Failed to find file \"" + 
					getResolvedBaseLocation() + "\"");
			return;
		}
		
		
		SVDBFileTree ft_root = new SVDBFileTree((SVDBFile)pp_file.duplicate());
		
		buildPreProcFileMap(null, ft_root);
		
		fPreProcFileMapValid = true;
	}
	
	/**
	 * Recurse through included files
	 * 
	 * @param parent
	 * @param root		-- 
	 * @param file
	 */
	protected void buildPreProcFileMap(
			SVDBFileTree 	parent,
			SVDBFileTree 	root) {
		SVDBFileTreeUtils	ft_utils = new SVDBFileTreeUtils();
		SVDBFile			file = root.getSVDBFile();

		if (parent != null) {
			root.getIncludedByFiles().add(parent);
		}
		
		ft_utils.resolveConditionals(root, 
				new SVPreProcDefineProvider(createPreProcMacroProvider(root)));
		
		if (fFileTreeMap.containsKey(file.getFilePath())) {
			fFileTreeMap.remove(file.getFilePath());
		}
		fFileTreeMap.put(file.getFilePath(), root);
		
		addIncludeFiles(root, root.getSVDBFile());
	}
	
	/**
	 * buildIndex()
	 * 
	 * Called by AbstractSVDBIndex to build the index
	 */
	protected void buildIndex(IProgressMonitor monitor) {
		fLog.debug("--> buildIndex()");
		getPreProcFileMap(monitor); // force pre-proc info to be built
		
		SVDBFile pp_file = findPreProcFile(getResolvedBaseLocation());

		if (pp_file == null) {
			fLog.error("Failed to find file \"" + getResolvedBaseLocation() + "\"");
			
			for (SVDBFile f : getPreProcFileMap(new NullProgressMonitor()).values()) {
				fLog.error("        " + f.getFilePath());
			}
			fIndexFileMapValid = true;
			return;
		}

		SVDBFileTree ft_root = fFileTreeMap.get(getResolvedBaseLocation());
		
		IPreProcMacroProvider mp = createMacroProvider(ft_root);
		processFile(ft_root, mp);
		
		fIndexFileMapValid = true;

		signalIndexRebuilt();
	}
	
	protected void signalIndexRebuilt() {
		for (ISVDBIndexChangeListener l : fIndexChageListeners) {
			l.index_rebuilt();
		}
	}
	
	protected SVDBFile processPreProcFile(String path, boolean replace) {
		SVPreProcScanner 	sc = new SVPreProcScanner();
		SVDBPreProcObserver ob = new SVDBPreProcObserver();

		sc.setObserver(ob);
		
		fLog.debug("processPreProcFile: path=" + path);
		InputStream in = fFileSystemProvider.openStream(path);
		
		if (in == null) {
			fLog.error(getClass().getName() + ": failed to open \"" + path + "\"");
			return null;
		}

		sc.init(in, path);
		sc.scan();
		
		try {
			in.close();
		} catch (IOException e) { }

		SVDBFile file = ob.getFiles().get(0);
		
		if (replace) {
			if (fPreProcFileMap.containsKey(file.getFilePath())) {
				fPreProcFileMap.remove(file.getFilePath());
			}
			fPreProcFileMap.put(file.getFilePath(), file);
		}

		file.setLastModified(fFileSystemProvider.getLastModifiedTime(path));
		
		return file;
	}
	
	public SVPreProcScanner createPreProcScanner(String path) {
		InputStream in = getFileSystemProvider().openStream(path);
		SVDBFileTree ft = getFileTreeMap(new NullProgressMonitor()).get(path);
		
		if (ft == null) {
			Map<String, SVDBFileTree> m = getFileTreeMap(new NullProgressMonitor());
			fLog.error("Failed to find pre-proc file for \"" + path + "\"");
			fLog.debug("map.size=" + m.size());
			for (String p : m.keySet()) {
				fLog.debug("    " + p);
			}
			return null;
		}

		IPreProcMacroProvider mp = createMacroProvider(ft);
		SVPreProcDefineProvider dp = new SVPreProcDefineProvider(mp);

		SVPreProcScanner pp = new SVPreProcScanner();
		pp.setDefineProvider(dp);
		//pp.setScanner(this);
		//pp.setObserver(this);

		pp.init(in, path);
		pp.setExpandMacros(true);
		pp.setEvalConditionals(true);
		
		return pp;
	}
	
	

	protected void processFile(
			SVDBFileTree				path,
			IPreProcMacroProvider 		mp) {
		
		fDefineProvider.setMacroProvider(mp);
		ISVDBFileFactory factory = SVCorePlugin.createFileFactory(fDefineProvider); 
		
		String path_s = path.getFilePath();

		InputStream in = fFileSystemProvider.openStream(path_s);
		
		if (in == null) {
			fLog.error("Failed to open file \"" + path_s + "\"");
		}
		
		BufferedInputStream in_b = new BufferedInputStream(in);

		SVDBFile svdb_f = factory.parse(in_b, path.getFilePath());

		// Problem parsing the file..
		if (svdb_f == null) {
			return;
		}
		
		svdb_f.setLastModified(
				fFileSystemProvider.getLastModifiedTime(path.getFilePath()));
		
		fFileSystemProvider.clearMarkers(path_s);
		
		// Reflect markers from pre-processor to index database
		propagateMarkersPreProc2DB(path, fPreProcFileMap.get(path_s), svdb_f);
		addMarkers(path_s, svdb_f);
		
		if (fIndexFileMap.containsKey(path.getFilePath())) {
			// Merge the files together. This happens during an update
			SVDBFile existing = fIndexFileMap.get(path.getFilePath());
			SVDBFileMerger.merge(existing, svdb_f, null, null, null);
			existing.setLastModified(svdb_f.getLastModified());
		} else {
			// Just add the file. This happens on first parse
			fIndexFileMap.put(path.getFilePath(), svdb_f);
		}

		// Now, recurse through the files included
		for (SVDBFileTree ft_t : path.getIncludedFiles()) {
			// Note: process files that are currently not in the FileIndex, 
			// but are in the pre-processor list. This ensures that we 
			// don't try to process files included from another index
			if (!fIndexFileMap.containsKey(ft_t.getFilePath()) &&
					fPreProcFileMap.containsKey(ft_t.getFilePath())) {
				mp = createMacroProvider(ft_t);
				processFile(ft_t, mp);
			}
		}

		fFileSystemProvider.closeStream(in);
	}

	protected void addIncludeFiles(
			SVDBFileTree 		root,
			ISVDBScopeItem 		scope) {
		for (int i=0; i<scope.getItems().size(); i++) {
			ISVDBItemBase it = scope.getItems().get(i);

			if (it.getType() == SVDBItemType.Include) {
				fLog.debug("Include file: " + ((ISVDBNamedItem)it).getName());
				
				// Look local first
				SVDBSearchResult<SVDBFile> f = findIncludedFileGlobal(((ISVDBNamedItem)it).getName());
				
				if (f != null) {
					fLog.debug("Found include file \"" + ((ISVDBNamedItem)it).getName() + "\" in index \"" + 
							f.getIndex().getBaseLocation() + "\"");
					fLog.debug("Adding included file \"" + ((ISVDBNamedItem)it).getName() + " to FileTree \"" +
							root.getFilePath() + "\"");
					SVDBFileTree ft = new SVDBFileTree((SVDBFile)f.getItem().duplicate());
					root.getIncludedFiles().add(ft);
					buildPreProcFileMap(root, ft);
				} else {
					fLog.debug("Failed to find include file \"" + ((ISVDBNamedItem)it).getName() + 
							"\" (from file " + root.getFilePath() + ")");
					SVDBFileTree ft = new SVDBFileTree(((ISVDBNamedItem)it).getName());
					root.getIncludedFiles().add(ft);
					ft.getIncludedByFiles().add(root);
					
					// Create a marker for the missing include file
					SVDBFile real_svdb = fPreProcFileMap.get(root.getFilePath());
					if (real_svdb != null) {
						SVDBMarker err = new SVDBMarker(SVDBMarker.MARKER_ERR,
								SVDBMarker.KIND_MISSING_INC,
								"Failed to find include file \"" + ((ISVDBNamedItem)it).getName() + "\"");
						err.setAttr(SVDBMarker.MISSING_INC_PATH, ((ISVDBNamedItem)it).getName());
						real_svdb.addItem(err);
						err.setLocation(it.getLocation());
					} else {
						fLog.error("Failed to find PreProc file for \"" + root.getFilePath() + "\"");
					}
					
					/*
					fLog.error("AbstractSVDBLibIndex: " +
							getBaseLocation() + " failed to find include file " + it.getName());
					 */
				}
				
			} else if (it instanceof ISVDBScopeItem) {
				addIncludeFiles(root, (ISVDBScopeItem)it);
			}
		}
	}
	
	public void fileAdded(String path) {
		// fileAdded is ignored for LibIndex, since all the
		// files are explicitly specified
		int ext_idx;
		
		if ((ext_idx = path.lastIndexOf('.')) != -1) {
			// Check the missing-include paths to see if this file could match
			String ext = path.substring(ext_idx);
			
			if (fSVExtensions.contains(ext)) {
				Map<SVDBFileTree, List<String>> missing_inc = find_missing_inc();
				String last_path_elem = new File(path).getName();
				boolean found = false;
				
				for (Entry<SVDBFileTree, List<String>> entry : missing_inc.entrySet()) {
					for (String inc : entry.getValue()) {
						String inc_name = new File(inc).getName();
						
						if (last_path_elem.equals(inc_name)) {
							found = true;
							break;
						}
					}
				}
				
				// Desired outcome is a set of files that must be reprocessed 
				// to deal with this change. For now, we'll just invalidate 
				// the entire index
				if (found) {
					fLog.debug("Invalidating index");
					rebuildIndex();
				}
			}
		}
	}

	public void fileChanged(String path) {
		if (fPreProcFileMapValid && fPreProcFileMap.containsKey(path)) {
			fLog.debug("fileChanged: " + path);
			// Rescan the file
			SVDBFile orig_file = fPreProcFileMap.get(path);
			SVDBFile pp_file = processPreProcFile(path, true);

			// Merge the new content to the existing content
			SVDBFileMerger.merge(orig_file, pp_file, null, null, null);
			orig_file.setLastModified(pp_file.getLastModified());
			SVDBFileTree ft = fFileTreeMap.get(path);
			if (ft != null) {
				ft.setSVDBFile(pp_file);
			} else {
				fLog.error("During fileChanged processing, file \"" + 
						path + "\" not in FileTreeMap");
			}
			
			// Update the pre-processor map
			buildPreProcFileMap(null, ft);

			if (fIndexFileMapValid && fIndexFileMap.containsKey(path)) {
				// rebuild the index
				SVDBFileTree ft_root = fFileTreeMap.get(path);
				IPreProcMacroProvider mp = createMacroProvider(ft_root);
				
				processFile(ft_root, mp);
			}
		} else {
		}
	}
	
	private Map<SVDBFileTree, List<String>> find_missing_inc() {
		Map<SVDBFileTree, List<String>> ret = new HashMap<SVDBFileTree, List<String>>();
		
		for (SVDBFileTree ft : fFileTreeMap.values()) {
			for (SVDBFileTree inc : ft.getIncludedFiles()) {
				if (inc.getSVDBFile() == null) {
					if (!ret.containsKey(ft)) {
						ret.put(ft, new ArrayList<String>());
					}
					ret.get(ft).add(inc.getFilePath());
				}
			}
		}
		
		return ret;
	}

	private Map<SVDBFile, List<String>> find_missing_inc_svdb() {
		Map<SVDBFile, List<String>> ret = new HashMap<SVDBFile, List<String>>();
		
		for (SVDBFile ft : fPreProcFileMap.values()) {
			for (ISVDBItemBase it : ft.getItems()) {
				if (it.getType() == SVDBItemType.Marker) {
					SVDBMarker m = (SVDBMarker)it;
					
					if (m.getKind().equals(SVDBMarker.KIND_MISSING_INC)) {
						if (!ret.containsKey(ft)) {
							ret.put(ft, new ArrayList<String>());
						}
						ret.get(ft).add(m.getAttr(SVDBMarker.MISSING_INC_PATH));
					}
				}
			}
		}
		
		return ret;
	}

	public void fileRemoved(String path) {
		if (fPreProcFileMapValid && fPreProcFileMap.containsKey(path)) {
			fLog.debug("fileRemoved: \"" + path + "\"");
			rebuildIndex();
			/*
			fPreProcFileMap.remove(path);
			fFileTreeMap.remove(path);
			fIndexFileMap.remove(path);
			 */
		}
	}

	public SVDBFile findPreProcFile(String path) {
		debug("findPreProcFile \"" + path + "\"");
		return getPreProcFileMap(new NullProgressMonitor()).get(path);
	}

	protected String resolvePath(String path_orig) {
		String path = path_orig;
		String norm_path = null;
		
		fLog.debug("resolvePath: " + path_orig);

		// relative to the base location or one of the include paths
		if (path.startsWith("..")) {
			if ((norm_path = resolveRelativePath(getResolvedBaseLocationDir(), path)) == null) {
				for (String inc_path : fIncludePaths) {
					if ((norm_path = resolveRelativePath(inc_path, path)) != null) {
						break; 
					}
				}
			}
		} else {
			if (path.equals(".")) {
				path = getResolvedBaseLocationDir();
			} else if (path.startsWith(".")) { 
				path = getResolvedBaseLocationDir() + "/" + path.substring(2);
			} else {
				if (!fFileSystemProvider.fileExists(path)) {
					//  See if this is an implicit path
					String imp_path = getResolvedBaseLocationDir() + "/" + path;
					if (fFileSystemProvider.fileExists(imp_path)) {
						// This path is an implicit relative path that is 
						// relative to the base directory
						path = imp_path;
					}
				}
			}
			norm_path = normalizePath(path);
		}
		
		return (norm_path != null)?norm_path:path_orig;
	}
	
	private String resolveRelativePath(String base, String path) {
		// path = getResolvedBaseLocationDir() + "/" + path;
		String norm_path = normalizePath(base + "/" + path);

		if (fFileSystemProvider.fileExists(norm_path)) {
			return norm_path;
		} else if (getBaseLocation().startsWith("${workspace_loc}")) {
			// This could be a reference outside the workspace. Check
			// whether we should reference this as a filesystem path 
			// by computing the absolute path
			String base_loc = getResolvedBaseLocationDir();
			base_loc = base_loc.substring("${workspace_loc}".length());

			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IContainer base_dir = null;
			try {
				base_dir = root.getFolder(new Path(base_loc));
			} catch (IllegalArgumentException e) {}

			if (base_dir == null) {
				if (base_loc.length() > 0) {
					base_dir = root.getProject(base_loc.substring(1));
				}
			}

			if (base_dir != null && base_dir.exists()) {
				IPath base_dir_p = base_dir.getLocation();
				if (base_dir_p != null) {
					File path_f_t = new File(base_dir_p.toFile(), path);
					try {
						if (path_f_t.exists()) {
							fLog.debug("Path does exist outside the project: " + path_f_t.getCanonicalPath());
							norm_path = SVFileUtils.normalize(path_f_t.getCanonicalPath());
							return norm_path;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		return null;
	}

	protected String normalizePath(String path) {
		StringBuilder ret = new StringBuilder();
		
		int i=path.length()-1;
		int end;
		int skipCnt = 0;
		
		// First, skip any trailing '/'
		while (i >=0 && (path.charAt(i) == '/' || path.charAt(i) == '\\')) {
			i--;
		}
		
		while (i >= 0) {
			// scan backwards find the next path element
			end = ret.length();
			
			while (i>=0 && path.charAt(i) != '/' && path.charAt(i) != '\\') {
				ret.append(path.charAt(i));
				i--;
			}
			
			if (i != -1) {
				ret.append("/");
				i--;
			}

			if ((ret.length() - end) > 0) {
				String str = ret.substring(end, ret.length()-1);
				if (str.equals("..")) {
					skipCnt++;
					// remove .. element
					ret.setLength(end);
				} else if (skipCnt > 0) {
					ret.setLength(end);
					skipCnt--;
				}
			}
		}

		/*
		if (skipCnt > 0) {
			// throw new RuntimeException("exceeded skipCnt while normalizing \"" + path + "\"");
		} 
		 */
		return ret.reverse().toString();
	}

	private void debug(String msg) {
		fLog.debug(msg);
	}

}
