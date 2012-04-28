package net.sf.sveditor.core.templates;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.log.LogFactory;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

public class WSExternalTemplateFinder extends AbstractExternalTemplateFinder {
	private IContainer				fPath;
	
	public WSExternalTemplateFinder(IContainer path) {
		super(new WSInStreamProvider());
		fPath = path;
		fLog = LogFactory.getLogHandle("WSExternalTemplateFinder");
	}

	@Override
	protected List<String> findTemplatePaths() {
		List<String> templates = new ArrayList<String>();
		
		findTemplatePaths(templates, fPath);
		
		return templates;
	}
	
	private void findTemplatePaths(List<String> templates, IContainer parent) {
		IResource resources[] = null;
		
		try {
			resources = parent.members();
		} catch (CoreException e) {}
		
		if (resources != null) {
			for (IResource r : resources) {
				if (r instanceof IFile && r.getName().endsWith(".svt")) {
					templates.add(((IFile)r).getFullPath().toOSString());
				}
			}
		}
	}

	@Override
	protected List<String> listFiles(String path) {
		List<String> files = new ArrayList<String>();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		
		IContainer c = null;

		c = root.getFolder(new Path(path));
		
		if (c != null) {
			IResource resources[] = null;
			
			try {
				resources = c.members();
			} catch (CoreException e) {}
			
			if (resources != null) {
				for (IResource r : resources) {
					if (r instanceof IFile) {
						files.add(((IFile)r).getFullPath().toOSString());
					}
				}
			}
			
		}
		
		return files;
	}

	@Override
	protected InputStream openFile(String path) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		InputStream in = null;

		IFile file = root.getFile(new Path(path));
		
		if (file.exists()) {
			
			try {
				in = file.getContents();
			} catch (CoreException e) {}
		}
		
		return in;
	}

	@Override
	protected void closeStream(InputStream in) {
		try {
			in.close();
		} catch (IOException e) {}
	}

}
