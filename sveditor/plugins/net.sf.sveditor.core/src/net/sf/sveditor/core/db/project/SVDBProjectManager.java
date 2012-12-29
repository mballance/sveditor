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


package net.sf.sveditor.core.db.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.db.index.plugin_lib.SVDBPluginLibDescriptor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public class SVDBProjectManager implements IResourceChangeListener {
	private WeakHashMap<IPath, SVDBProjectData>		fProjectMap;
	private List<ISVDBProjectSettingsListener>		fListeners;
	
	public SVDBProjectManager() {
		fProjectMap = new WeakHashMap<IPath, SVDBProjectData>();
		fListeners = new ArrayList<ISVDBProjectSettingsListener>();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}
	
	public void init() {
		fProjectMap.clear();
	}
	
	public void addProjectSettingsListener(ISVDBProjectSettingsListener l) {
		synchronized (fListeners) {
			fListeners.add(l);
		}
	}
	
	public void removeProjectSettingsListener(ISVDBProjectSettingsListener l) {
		synchronized (fListeners) {
			fListeners.remove(l);
		}
	}
	
	void projectSettingsChanged(SVDBProjectData data) {
		synchronized (fListeners) {
			for (int i=0; i<fListeners.size(); i++) {
				fListeners.get(i).projectSettingsChanged(data);
			}
		}
	}
	
	public List<SVDBProjectData> getProjectList() {
		List<SVDBProjectData> ret = new ArrayList<SVDBProjectData>();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		
		for (IProject p : root.getProjects()) {
			if (p.isOpen() && p.getFile(".svproject").exists()) {
				SVDBProjectData pd = getProjectData(p);
				if (pd != null) {
					ret.add(pd);
				}
			}
		}
		
		return ret;
	}
	
	public SVDBProjectData getProjectData(IProject proj) {
		SVDBProjectData ret = null;
		
		if (fProjectMap.containsKey(proj.getFullPath())) {
			ret = fProjectMap.get(proj.getFullPath());
		} else {
			/*
			IFile svproject;
			SVProjectFileWrapper f_wrapper = null;
			if ((svproject = proj.getFile(".svproject")).exists()) {
				InputStream in = null;
				try {
					svproject.refreshLocal(IResource.DEPTH_ZERO, null);
					in = svproject.getContents();
				} catch (CoreException e) {
					e.printStackTrace();
				}

				try {
					f_wrapper = new SVProjectFileWrapper(in);
				} catch (Exception e) {
					// File format is bad
					f_wrapper = null;
				}
			}
			
			if (f_wrapper == null) {
				f_wrapper = new SVProjectFileWrapper();
				
				setupDefaultProjectFile(f_wrapper);
				
				// Write the file
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				f_wrapper.toStream(bos);
				ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
				
				try {
					if (svproject.exists()) {
						svproject.setContents(bis, true, true, null);
					} else {
						svproject.create(bis, true, null);
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
			 */
			
			ret = new SVDBProjectData(proj);
			
			fProjectMap.put(proj.getFullPath(), ret);
		}
		
		return ret;
	}

	/**
	 * Setup the default project data.
	 * - Includes default plugin libraries
	 * 
	 * @param file_wrapper
	 */
	public static void setupDefaultProjectFile(SVProjectFileWrapper file_wrapper) {
		List<SVDBPluginLibDescriptor> lib_d = SVCorePlugin.getDefault().getPluginLibList();
		
		for (SVDBPluginLibDescriptor d : lib_d) {
			if (d.isDefault()) {
				file_wrapper.getPluginPaths().add(new SVDBPath(d.getId()));
			}
		}
	}
	
	public void resourceChanged(IResourceChangeEvent event) {
		final Set<IProject> changed_project = new HashSet<IProject>();
		
		if (event.getDelta() != null) {
			try {
				event.getDelta().accept(new IResourceDeltaVisitor() {
					
					public boolean visit(IResourceDelta delta)
							throws CoreException {
						IProject p = delta.getResource().getProject();
						if (p != null && fProjectMap.containsKey(p.getFullPath())) {
							if (delta.getResource().equals(".project") && delta.getKind() == IResourceDelta.CHANGED) {
								if (!changed_project.contains(p)) {
									changed_project.add(p);
								}
							}
						}
						return true;
					}
				});
			} catch (CoreException e) {
			}
		}
		
		for (IProject p : changed_project) {
			SVDBProjectData pd = fProjectMap.get(p.getFullPath());
			
			// Only refresh if the project-file wrapper detects
			// that something has changed
			pd.setProjectFileWrapper(pd.getProjectFileWrapper(), false);
			// re-scan project data file
//			pd.refreshProjectFile();
		}
	}
	
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}
}
