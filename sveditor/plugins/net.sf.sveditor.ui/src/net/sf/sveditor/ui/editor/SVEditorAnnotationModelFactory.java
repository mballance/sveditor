package net.sf.sveditor.ui.editor;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModelFactory;

public class SVEditorAnnotationModelFactory extends ResourceMarkerAnnotationModelFactory {

	public IAnnotationModel createAnnotationModel(IPath location) {
		
		IFile file= FileBuffers.getWorkspaceFileAtLocation(location);
		if (file != null) {
			
		}
		
		return super.createAnnotationModel(location);
	}

}
