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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.db.index.cache.ISVDBIndexCache;
import net.sf.sveditor.core.db.index.cache.ISVDBIndexCacheFactory;
import net.sf.sveditor.core.db.index.plugin_lib.SVDBPluginLibIndexFactory;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;

/**
 * Central storage for leaf indexes. Provides a central place to locate
 * an index and supports persistence and loading
 * 
 * @author ballance
 *
 */
public class SVDBIndexRegistry  {
	public static final String								GLOBAL_PROJECT = "GLOBAL";
	
	private SVDBIndexCollectionMgr							fGlobalIndexMgr;
	private Map<String, List<ISVDBIndex>>					fProjectIndexMap;
	private Map<String, List<SVDBPersistenceDescriptor>>  	fDatabaseDescMap;
	private ISVDBIndexCacheFactory							fCacheFactory;
	private LogHandle										fLog;

	/**
	 * SVDBIndexRegistry constructor. Only intended to be called by
	 * 
	 * @param state_location
	 */
	public SVDBIndexRegistry() {
		this(false);
	}
	
	public SVDBIndexRegistry(boolean standalone_test_mode) {
		fProjectIndexMap = new WeakHashMap<String, List<ISVDBIndex>>();
		fDatabaseDescMap = new HashMap<String, List<SVDBPersistenceDescriptor>>();
		fLog = LogFactory.getLogHandle("SVDBIndexRegistry");
	}
	
	/*
	@Deprecated
	public void init(final File state_location) {
		fProjectIndexMap.clear();
		fCacheFactory = new ISVDBIndexCacheFactory() {
			
			public ISVDBIndexCache createIndexCache(
					String project_name,
					String base_location) {
				File cache_dir = new File(state_location, 
						project_name + "_" + SVFileUtils.computeMD5(base_location));
				SVDBDirFS fs = new SVDBDirFS(cache_dir);
				return new SVDBFileIndexCache(fs);
			}
		};
		fGlobalIndexMgr = getGlobalIndexMgr();
	}
	 */

	public void init(ISVDBIndexCacheFactory cache_factory) {
		fCacheFactory = cache_factory;
		fProjectIndexMap.clear();
		
		/*
		fProjectIndexMap.clear();
		fDatabaseDescMap.clear();
		if (state_location != null) {
			fDatabaseDir = new File(state_location, "db");
			load_database_descriptors();
		} else {
			fDatabaseDir = null;
		}
		 */
		
		fGlobalIndexMgr = getGlobalIndexMgr();
	}

	public List<ISVDBIndex> getProjectIndexList(String project) {
		if (fProjectIndexMap.containsKey(project)) {
			return fProjectIndexMap.get(project);
		} else {
			return new ArrayList<ISVDBIndex>();
		}
	}
	
	public SVDBIndexCollectionMgr getGlobalIndexMgr() {
		if (fGlobalIndexMgr == null) {
			fGlobalIndexMgr = new SVDBIndexCollectionMgr(GLOBAL_PROJECT);
			
			// Ensure the global index has access to the built-in types
			ISVDBIndex index = findCreateIndex(
					new NullProgressMonitor(),
					SVDBIndexRegistry.GLOBAL_PROJECT, 
					SVCorePlugin.SV_BUILTIN_LIBRARY, 
					SVDBPluginLibIndexFactory.TYPE, null);
			if (index != null) {
				fGlobalIndexMgr.addPluginLibrary(index);
			}
		}
		return fGlobalIndexMgr;
	}

	@Deprecated
	public ISVDBIndex findCreateIndex(
			String 					project, 
			String 					base_location, 
			String 					type,
			Map<String, Object>		config) {
		return findCreateIndex(new NullProgressMonitor(), project,
				base_location, type, config);
	}
	
	public ISVDBIndex findCreateIndex(
			IProgressMonitor		monitor,
			String 					project, 
			String 					base_location, 
			String 					type,
			ISVDBIndexCache			cache,
			Map<String, Object>		config) {
		ISVDBIndex ret = null;
		
		fLog.debug("findCreateIndex: " + base_location + " ; " + type);
		
		if (!fProjectIndexMap.containsKey(project)) {
			fProjectIndexMap.put(project, new ArrayList<ISVDBIndex>());
		}
		
		List<ISVDBIndex> project_index = fProjectIndexMap.get(project); 
		
		for (ISVDBIndex index : project_index) {
			if (index.getBaseLocation().equals(base_location) &&
					index.getTypeID().equals(type)) {
				ret = index;
				break;
			}
		}
		
		if (ret == null) {
			fLog.debug("    Index does not exist -- creating");
			// See about creating a new index
			ISVDBIndexFactory factory = findFactory(type);
			
			ret = factory.createSVDBIndex(project, base_location, cache, config);
			
			SubProgressMonitor m = new SubProgressMonitor(monitor, 1);
			ret.init(m);
			
			project_index.add(ret);
		} else {
			fLog.debug("    Index already exists");
		}
		
		return ret;
		
	}

	/**
	 * Finds or creates an index
	 * 
	 * @param project        project this index is associated with
	 * @param base_location  base location for the index
	 * @param type           index type
	 * @return
	 */
	public ISVDBIndex findCreateIndex(
			IProgressMonitor		monitor,
			String 					project, 
			String 					base_location, 
			String 					type,
			Map<String, Object>		config) {
		ISVDBIndex ret = null;
		
		fLog.debug("findCreateIndex: " + base_location + " ; " + type);
		
		if (!fProjectIndexMap.containsKey(project)) {
			fProjectIndexMap.put(project, new ArrayList<ISVDBIndex>());
		}
		
		List<ISVDBIndex> project_index = fProjectIndexMap.get(project); 
		
		for (ISVDBIndex index : project_index) {
			if (index.getBaseLocation().equals(base_location) &&
					index.getTypeID().equals(type)) {
				ret = index;
				break;
			}
		}
		
		if (ret == null) {
			fLog.debug("    Index does not exist -- creating");
			// See about creating a new index
			ISVDBIndexFactory factory = findFactory(type);
			ISVDBIndexCache cache = fCacheFactory.createIndexCache(project, base_location);
			
			ret = factory.createSVDBIndex(project, base_location, cache, config);
			
			SubProgressMonitor m = new SubProgressMonitor(monitor, 1);
			ret.init(m);
			
			project_index.add(ret);
		} else {
			fLog.debug("    Index already exists");
		}
		
		return ret;
	}

	public ISVDBIndex findCreateIndex(
			String 					project, 
			String 					base_location, 
			String 					type,
			ISVDBIndexFactory		factory,
			Map<String, Object>		config) {
		return findCreateIndex(new NullProgressMonitor(), project,
				base_location, type, factory, config);
	}

	/**
	 * Finds or creates an index
	 * 
	 * @param project        project this index is associated with
	 * @param base_location  base location for the index
	 * @param type           index type
	 * @return
	 */
	public ISVDBIndex findCreateIndex(
			IProgressMonitor		monitor,
			String 					project, 
			String 					base_location, 
			String 					type,
			ISVDBIndexFactory		factory,
			Map<String, Object>		config) {
		ISVDBIndex ret = null;
		
		fLog.debug("findCreateIndex: " + base_location + " ; " + type);
		
		if (!fProjectIndexMap.containsKey(project)) {
			fProjectIndexMap.put(project, new ArrayList<ISVDBIndex>());
		}
		
		List<ISVDBIndex> project_index = fProjectIndexMap.get(project); 
		
		for (ISVDBIndex index : project_index) {
			if (index.getBaseLocation().equals(base_location) &&
					index.getTypeID().equals(type)) {
				ret = index;
				break;
			}
		}
		
		if (ret == null) {
			fLog.debug("    Index does not exist -- creating");
			ISVDBIndexCache cache = fCacheFactory.createIndexCache(project, base_location);
			
			// See about creating a new index
			ret = factory.createSVDBIndex(project, base_location, cache, config);
			
			SubProgressMonitor m = new SubProgressMonitor(monitor, 1);
			ret.init(m);
			
			project_index.add(ret);
		} else {
			fLog.debug("    Index already exists");
		}
		
		return ret;
	}

	public void rebuildIndex(String project) {
		fLog.debug("rebuildIndex \"" + project + "\"");
		if (!fProjectIndexMap.containsKey(project)) {
			fLog.debug("    skipping - not a registered project");
			return;
		}
		
		for (ISVDBIndex index : fProjectIndexMap.get(project)) {
			index.rebuildIndex();
		}
		
		// Also clear any persistence data for the project
		if (fDatabaseDescMap.containsKey(project)) {
			List<SVDBPersistenceDescriptor> db_desc = fDatabaseDescMap.get(project);
			
			for (SVDBPersistenceDescriptor d : db_desc) {
				d.getDBFile().delete();
			}
			fDatabaseDescMap.remove(project);
		}
	}

	/**
	 * Saves the state of loaded indexes to the state_location directory
	 */
	public void save_state() {
		fLog.debug("save_state()");
		
		for (List<ISVDBIndex> index_l : fProjectIndexMap.values()) {
			for (ISVDBIndex index : index_l) {
				index.dispose();
			}
		}
		/*
		for (String proj_name : fProjectIndexMap.keySet()) {
			save_state(proj_name, fProjectIndexMap.get(proj_name));
		}
		 */
		
		// Now, iterate through each saved database and clean up
		// any leftover saved-database files
		
	}
	
	
	private ISVDBIndexFactory findFactory(String type) {
		ISVDBIndexFactory ret = null;
		IExtensionRegistry rgy = Platform.getExtensionRegistry();
		
		
		IExtensionPoint ext_pt = rgy.getExtensionPoint(
				SVCorePlugin.PLUGIN_ID, "SVDBIndexFactories");
		
		for (IExtension ext_l : ext_pt.getExtensions()) {
			for (IConfigurationElement cel : ext_l.getConfigurationElements()) {
				String id = cel.getAttribute("id");
				
				if (type.equals(id)) {
					try {
						ret = (ISVDBIndexFactory)cel.createExecutableExtension(
							"class");
					} catch (Exception e) {
						fLog.error("Failed to create .exe class for IndexFactory " +
								"extension \"" + id + "\"", e);
					}
					break;
				}
			}
		}
		
		return ret;
	}
	
}
