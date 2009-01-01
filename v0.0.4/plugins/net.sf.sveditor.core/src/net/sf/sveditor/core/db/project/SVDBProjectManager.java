package net.sf.sveditor.core.db.project;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import net.sf.sveditor.core.ISVDBFileProvider;
import net.sf.sveditor.core.ISVDBIndex;
import net.sf.sveditor.core.SVDBFilesystemIndex;
import net.sf.sveditor.core.SVDBWorkspaceIndex;
import net.sf.sveditor.core.SVProjectFileWrapper;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public class SVDBProjectManager implements IResourceChangeListener {
	private WeakHashMap<IPath, SVDBProjectData>		fProjectMap;
	private WeakHashMap<File, ISVDBIndex>			fBuildPathEntries;
	
	public SVDBProjectManager() {
		fProjectMap = new WeakHashMap<IPath, SVDBProjectData>();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}
	
	public ISVDBIndex findBuildPathIndex(IFile root) {
		File file = root.getLocation().toFile();
		
		if (fBuildPathEntries.containsKey(file)) {
			return fBuildPathEntries.get(file);
		} else {
			ISVDBFileProvider file_provider = null;
			
			if (fProjectMap.containsKey(root.getProject())) {
				fProjectMap.get(root.getProject()).getFileProvider();
				
			}
			ISVDBIndex ret = new SVDBWorkspaceIndex(root.getFullPath(), file_provider); 
			fBuildPathEntries.put(ret.getBaseLocation(), ret);
			
			return ret;
		}
	}
	
	public ISVDBIndex findBuildPathIndex(File root, ISVDBFileProvider provider) {

			if (fBuildPathEntries.containsKey(root)) {
				return fBuildPathEntries.get(root);
			} else {
				ISVDBIndex ret = new SVDBFilesystemIndex(root, provider); 
				fBuildPathEntries.put(root, ret);
				return ret;
			}
	}
	
	public SVDBProjectData getProjectData(IProject proj) {
		SVDBProjectData ret = null;
		
		if (fProjectMap.containsKey(proj.getFullPath())) {
			ret = fProjectMap.get(proj.getFullPath());
		} else {
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
			
			ret = new SVDBProjectData(f_wrapper, svproject.getFullPath());
			
			fProjectMap.put(proj.getFullPath(), ret);
		}
		
		return ret;
	}
	
	public SVDBProjectData getProjectData(IFolder dir) {
		SVDBProjectData ret = null;
		
		if (fProjectMap.containsKey(dir.getFullPath())) {
			ret = fProjectMap.get(dir.getFullPath());
		} else {
			IFile svproject;
			SVProjectFileWrapper f_wrapper = null;
			if ((svproject = dir.getFile(".svproject")).exists()) {
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
					e.printStackTrace();
				}
			}
			
			if (f_wrapper == null) {
				f_wrapper = new SVProjectFileWrapper();
				
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
			
			ret = new SVDBProjectData(f_wrapper, svproject.getFullPath());
			
			fProjectMap.put(dir.getFullPath(), ret);
		}
		
		return ret;
	}

	
	public void resourceChanged(IResourceChangeEvent event) {
		final List<IProject> changed_projects = new ArrayList<IProject>();
		
		if (event.getDelta() != null) {
			try {
				event.getDelta().accept(new IResourceDeltaVisitor() {
					
					public boolean visit(IResourceDelta delta)
							throws CoreException {
						IProject p = delta.getResource().getProject();
						if (p != null && 
								fProjectMap.containsKey(p.getFullPath()) &&
								!changed_projects.contains(p)) {
							changed_projects.add(p);
						}
						return true;
					}
				});
			} catch (CoreException e) {
			}
		}
		
		/*
		for (IProject p : changed_projects) {
			SVDBProjectData pd = fProjectMap.get(p.getFullPath());
			
			// re-scan project data file
			pd.refreshProjectFile();
		}
		 */
		
		// TODO: Now, iterate through the projects and re-scan the files
	}
	
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}
}
