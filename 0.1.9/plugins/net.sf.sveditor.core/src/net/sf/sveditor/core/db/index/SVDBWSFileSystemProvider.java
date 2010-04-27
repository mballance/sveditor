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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.SVFileUtils;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class SVDBWSFileSystemProvider implements ISVDBFileSystemProvider, 
		IResourceChangeListener, IResourceDeltaVisitor {
	
	private List<ISVDBFileSystemChangeListener>			fChangeListeners;
	
	public SVDBWSFileSystemProvider() {
		fChangeListeners = new ArrayList<ISVDBFileSystemChangeListener>();
	}
	
	public void init(String path) {
		IFile 		file;
		IContainer 	folder = null;
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		
		if (path.startsWith("${workspace_loc}")) {
			path = path.substring("${workspace_loc}".length());
		}
		
		try {
			folder = root.getFolder(new Path(path));

			if (!folder.exists()) {
				file = root.getFile(new Path(path));
				folder = file.getParent();
				
				if (!folder.exists()) {
					folder = null;
				}
			}
		} catch (IllegalArgumentException e) {} // Happens when the folder is a project
		
		if (folder == null) {
			// Try looking at open projects
			String pname = path;
			
			if (pname.startsWith("/")) {
				pname = pname.substring(1);
			}
			if (pname.endsWith("/")) {
				pname = pname.substring(0, pname.length()-1);
			}
			
			for (IProject p_t : root.getProjects()) {
				if (p_t.isOpen() && p_t.getName().equals(pname)) {
					folder = p_t;
				}
			}
		}
		
		if (folder != null) {
			try {
				folder.refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (CoreException e) { }
		}
		
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);		
	}

	public void addMarker(
			String 			path,
			final String			type,
			final int				lineno,
			final String			msg) {
		if (path.startsWith("${workspace_loc}")) {
			path = path.substring("${workspace_loc}".length());
			
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			
			final IFile file = root.getFile(new Path(path));

			WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
				
				@Override
				protected void execute(IProgressMonitor monitor) throws CoreException,
						InvocationTargetException, InterruptedException {
					IMarker marker = null;
					
					try {
						marker = file.createMarker(IMarker.PROBLEM);
						if (type.equals(MARKER_TYPE_ERROR)) {
							marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
						} else if (type.equals(MARKER_TYPE_WARNING)) {
							marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
						} else {
							marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
						}
						marker.setAttribute(IMarker.LINE_NUMBER, lineno);
						marker.setAttribute(IMarker.MESSAGE, msg);
					} catch (CoreException e) {
						if (marker != null) {
							marker.delete();
						}
					}
				}
			};

			try {
				op.run(new NullProgressMonitor());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void clearMarkers(String path) {
		if (path.startsWith("${workspace_loc}")) {
			path = path.substring("${workspace_loc}".length());
			
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			
			IFile file = root.getFile(new Path(path));
			
			if (file.exists()) {
				try {
					IMarker markers[] = file.findMarkers(
							IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);

					for (IMarker m : markers) {
						m.delete();
					}
				} catch (CoreException e) {
					// e.printStackTrace();
				}
			}
		}
	}

	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);		
	}
	
	public boolean fileExists(String path) {
		if (path.startsWith("${workspace_loc}")) {
			path = path.substring("${workspace_loc}".length());
			
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			
			IFile file = root.getFile(new Path(path));
			
			return file.exists();
		} else {
			// Also look at the filesystem
			return new File(path).exists();
		}
	}

	public void closeStream(InputStream in) {
		try {
			if (in != null) {
				in.close();
			}
		} catch (IOException e) { }
	}

	public InputStream openStream(String path) {
		InputStream ret = null;
		
		if (path.startsWith("${workspace_loc}")) {
			path = path.substring("${workspace_loc}".length());
			
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			
			IFile file = root.getFile(new Path(path));
			if (!file.exists()) {
				return null;
			}
			
			for (int i=0; i<2; i++) {
				try {
					ret = file.getContents();
					break;
				} catch (CoreException e) {
					// Often times, we can just refresh the resource to avoid
					// an indexing failure
					if (i == 0 && e.getMessage().contains("out of sync")) {
						try {
							file.getParent().refreshLocal(IResource.DEPTH_INFINITE, null);
						} catch (CoreException e2) {}
					} else {
						e.printStackTrace();
					}
				}
			}
		} else {
			try {
				ret = new FileInputStream(path);
			} catch (IOException e) {}
		}
		
		return ret;
	}
	
	public String resolvePath(String path) {
		if (!path.startsWith("${workspace_loc}")) {
			return path;
		}
		
		// Trim workspace_loc off so we can recognize when we've reached the root
		path = path.substring("${workspace_loc}".length());
		StringBuilder ret = new StringBuilder();
		
		int i=path.length()-1;
		int end;
		int skipCnt = 0;
		
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
		
		if (skipCnt > 0) {
			throw new RuntimeException("exceeded skipCnt");
		}
		
		return ret.reverse().toString();
	}
	
	protected String normalizePath(String path) {
		StringBuilder ret = new StringBuilder();
		
		int i=path.length()-1;
		int end;
		int skipCnt = 0;
		
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
		
		if (skipCnt > 0) {
			throw new RuntimeException("exceeded skipCnt");
		}
		
		return ret.reverse().toString();
	}
	

	public long getLastModifiedTime(String path) {
		if (path.startsWith("${workspace_loc}")) {
			path = path.substring("${workspace_loc}".length());
			
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			
			IFile file = root.getFile(new Path(path));
			
			if (file != null) {
				return file.getModificationStamp();
			} else {
				return 0;
			}
		} else {
			return new File(path).lastModified();
		}
	}

	public void addFileSystemChangeListener(ISVDBFileSystemChangeListener l) {
		fChangeListeners.add(l);
	}

	public void removeFileSystemChangeListener(ISVDBFileSystemChangeListener l) {
		fChangeListeners.remove(l);
	}

	public synchronized boolean visit(IResourceDelta delta) throws CoreException {
		
		if (delta.getResource() instanceof IFile) {
			String file = "${workspace_loc}";
			
			file += SVFileUtils.normalize(((IFile)delta.getResource()).getFullPath().toOSString());
			
			if (delta.getKind() == IResourceDelta.REMOVED) {
				// remove from the queue (if present) and the index
				for (ISVDBFileSystemChangeListener l : fChangeListeners) {
					l.fileRemoved(file);
				}
			} else if (delta.getKind() == IResourceDelta.ADDED) {
				for (ISVDBFileSystemChangeListener l : fChangeListeners) {
					l.fileAdded(file);
				}
			} else if (delta.getKind() == IResourceDelta.CHANGED) {
				if ((delta.getFlags() & IResourceDelta.CONTENT) != 0) {
					for (ISVDBFileSystemChangeListener l : fChangeListeners) {
						l.fileChanged(file);
					}
				}
			}
		}

		return true;
	}

	public void resourceChanged(IResourceChangeEvent event) {
		try {
			if (event.getDelta() != null) {
				event.getDelta().accept(this);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
}
