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
import java.util.List;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.SVFileUtils;
import net.sf.sveditor.core.db.index.cache.ISVDBIndexCache;
import net.sf.sveditor.core.db.index.cache.ISVDBIndexCacheFactory;
import net.sf.sveditor.core.db.index.cache.InMemoryIndexCache;
import net.sf.sveditor.core.db.index.plugin_lib.SVDBPluginLibIndexFactory;
import net.sf.sveditor.core.log.ILogLevel;
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
public class SVDBIndexRegistry implements ILogLevel {
	public static final String								GLOBAL_PROJECT = "GLOBAL";
	
	private SVDBIndexCollectionMgr							fIndexCollectionMgr;
	private SVDBIndexCollection								fGlobalIndexMgr;
	private List<ISVDBIndex>								fIndexList;
	private ISVDBIndexCacheFactory							fCacheFactory;
	private boolean											fAutoRebuildEn;
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
		fIndexList = new ArrayList<ISVDBIndex>();
		fLog = LogFactory.getLogHandle("SVDBIndexRegistry");
		fAutoRebuildEn = true;
		fIndexCollectionMgr = new SVDBIndexCollectionMgr();
	}
	
	public void setEnableAutoRebuild(boolean en) {
		fAutoRebuildEn = en;
		
		clearStaleIndexes();
		
		for (ISVDBIndex i : fIndexList) {
			i.setEnableAutoRebuild(fAutoRebuildEn);
		}
	}
	
	public SVDBIndexCollectionMgr getIndexCollectionMgr() {
		return fIndexCollectionMgr;
	}
	
	public void init(ISVDBIndexCacheFactory cache_factory) {
		fLog.debug(LEVEL_MIN, "SVDBIndexRegistry.init()");
		fCacheFactory = cache_factory;
		fIndexList.clear();
		
		fGlobalIndexMgr = getGlobalIndexMgr();
	}

	public void test_init(ISVDBIndexCacheFactory cache_factory) {
		fLog.debug(LEVEL_MIN, "SVDBIndexRegistry.test_init()");
		fCacheFactory = cache_factory;
		fIndexList.clear();
	}
	
	public List<ISVDBIndex> getAllProjectLists() {
		List<ISVDBIndex> ret = new ArrayList<ISVDBIndex>();
		synchronized (fIndexList) {
			for (ISVDBIndex i : fIndexList) {
				ret.add(i);
			}
		}
		return ret ;
	}

	public List<ISVDBIndex> getProjectIndexList(String project) {
		List<ISVDBIndex> ret = new ArrayList<ISVDBIndex>();
		
		clearStaleIndexes();
		
		synchronized (fIndexList) {
			for (ISVDBIndex i : fIndexList) {
				if (i.getProject().equals(project)) {
					ret.add(i);
				}
			}
		}
		
		return ret;
	}
	
	public List<ISVDBIndex> getIndexList() {
		List<ISVDBIndex> ret = new ArrayList<ISVDBIndex>();
		
		clearStaleIndexes();
		
		synchronized (fIndexList) {
			for (ISVDBIndex i : fIndexList) {
				if (i != null) {
					ret.add(i);
				}
			}
		}
		
		return ret;
	}
	
	public void disposeIndex(ISVDBIndex index, String reason) {
		fLog.debug(LEVEL_MID, "Dispose Index " + index.getBaseLocation() + " ; " + index.getConfig());
		synchronized (fIndexList) {
			fLog.debug(LEVEL_MIN, "Remove index \"" + index.getBaseLocation() + "\"");
			if (!fIndexList.remove(index)) {
				fLog.debug(LEVEL_MIN, "Warning: Index not managed by registry");
			}
		}
		
		// Clean up cache storage
		ISVDBIndexCache cache = index.getCache();
		
		index.dispose();
	
		// Clear out data from the cache
		cache.clear(new NullProgressMonitor());
	}
	
	public SVDBIndexCollection getGlobalIndexMgr() {
		if (fGlobalIndexMgr == null) {
			fGlobalIndexMgr = new SVDBIndexCollection(fIndexCollectionMgr, GLOBAL_PROJECT);
			
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

	/**
	 * Finds or creates an index
	 * 
	 * @param project        project this index is associated with
	 * @param base_location  base location for the index
	 * @param type           index type
	 * @return
	 */
	public synchronized ISVDBIndex findCreateIndex(
			IProgressMonitor		monitor,
			String 					project, 
			String 					base_location, 
			String 					type,
			SVDBIndexConfig			config) {
		ISVDBIndex ret = null;
		
		base_location = SVFileUtils.normalize(base_location);
		
		fLog.debug(LEVEL_MIN, "--> findCreateIndex: " + 
				base_location + " ; " + type + " " + project);
		
		synchronized (fIndexList) {
			for (ISVDBIndex i : fIndexList) {
				fLog.debug("  Checking: " + i.getBaseLocation() + " ; " + 
						i.getTypeID() + " ; " + i.getProject());
				if (i.getProject().equals(project) &&
						i.getBaseLocation().equals(base_location) &&
						i.getTypeID().equals(type)) {
					fLog.debug("  found match");
					ret = i;
					break;
				}
			}
		}
		
		// After finding an entry based using basic parameters, ensure
		// that the configuration parameters match as well
		if (ret != null) {
			// Compare configuration to see if we're actually being
			// asked to create a different index
			if (!SVDBIndexConfig.equals(config, ret.getConfig())) {
				fLog.debug(LEVEL_MID, "Index Config for " + ret.getBaseLocation() + " does not match");
				disposeIndex(ret, "Index config does not match");
				ret = null;
			}
		}
		
		if (ret == null) {
			// See about creating a new index
			ISVDBIndexFactory factory = findFactory(type);
			ISVDBIndexCache cache = null;
			
			if (type.equals(SVDBShadowIndexFactory.TYPE)) {
				cache = new InMemoryIndexCache();
			} else {
				cache = fCacheFactory.createIndexCache(project, base_location);
			}
			
			ret = factory.createSVDBIndex(project, base_location, cache, config);
			ret.setEnableAutoRebuild(fAutoRebuildEn);
			
			fLog.debug(LEVEL_MIN, "    Index " + base_location + 
					" does not exist -- creating: " + ret);
			
			SubProgressMonitor m = new SubProgressMonitor(monitor, 1);
			ret.init(m);
			
			synchronized (fIndexList) {
				fLog.debug(LEVEL_MIN, "Add new index \"" + ret.getBaseLocation() + "\"");
				fIndexList.add(ret);
			}
		} else {
			fLog.debug("    Index already exists");
		}
		
		fLog.debug(LEVEL_MIN, "<-- findCreateIndex: " + 
				base_location + " ; " + type + " " + project);
		return ret;
	}

	public ISVDBIndex findCreateIndex(
			String 					project, 
			String 					base_location, 
			String 					type,
			ISVDBIndexFactory		factory,
			SVDBIndexConfig			config) {
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
			SVDBIndexConfig			config) {
		ISVDBIndex ret = null;
		
		fLog.debug("findCreateIndex: " + base_location + " ; " + type);
		
		synchronized (fIndexList) {
			for (ISVDBIndex i : fIndexList) {
				if (i.getProject().equals(project) &&
						i.getBaseLocation().equals(base_location) &&
						i.getTypeID().equals(type)) {
					ret = i;
					break;
				}
			}
		}
		
		if (ret == null) {
			fLog.debug("    Index does not exist -- creating");
			ISVDBIndexCache cache = fCacheFactory.createIndexCache(project, base_location);
			
			// See about creating a new index
			ret = factory.createSVDBIndex(project, base_location, cache, config);
			ret.setEnableAutoRebuild(fAutoRebuildEn);
			
			SubProgressMonitor m = new SubProgressMonitor(monitor, 1);
			ret.init(m);
			
			synchronized (fIndexList) {
				fLog.debug(LEVEL_MIN, "Add new index \"" + ret.getBaseLocation() + "\"");
				fIndexList.add(ret);
			}
		} else {
			fLog.debug("    Index already exists");
		}
		
		return ret;
	}
	
	public void rebuildIndex(IProgressMonitor monitor, String project) {
		fLog.debug("rebuildIndex \"" + project + "\"");
		clearStaleIndexes();
		
		synchronized (fIndexList) {
			monitor.beginTask("rebuildIndex", fIndexList.size());
			for (ISVDBIndex i : fIndexList) {
				if (i.getProject().equals(project)) {
					i.rebuildIndex(new SubProgressMonitor(monitor, 1));
				}
				else
					monitor.worked(1);
			}
			monitor.done();
		}
	}

	/**
	 * Saves the state of loaded indexes to the state_location directory
	 */
	public void save_state() {
		fLog.debug("save_state()");
		
		synchronized (fIndexList) {
			for (ISVDBIndex i : fIndexList) {
				i.dispose();
			}
		}
		
		if (fCacheFactory != null) {
			List<ISVDBIndexCache> cache_l = new ArrayList<ISVDBIndexCache>();
			synchronized (fIndexList) {
				for (ISVDBIndex i : fIndexList) {
					if (!cache_l.contains(i.getCache()) && i.getCache() != null) {
						cache_l.add(i.getCache());
					}
				}
			}
			
			// Compact the cache-storage area
			fCacheFactory.compactCache(cache_l);
		}
		
		fIndexList.clear();
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
	
	private void clearStaleIndexes() {
		/*
		synchronized (fIndexList) {
			for (int i=0; i<fIndexList.size(); i++) {
				if (fIndexList.get(i).get() == null) {
					System.out.println("Removing stale index");
					fIndexList.remove(i);
					i--;
				}
			}
		}
		 */
	}
	
}
