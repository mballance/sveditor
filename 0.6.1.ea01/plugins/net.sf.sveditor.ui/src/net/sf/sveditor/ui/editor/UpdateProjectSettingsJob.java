package net.sf.sveditor.ui.editor;

import net.sf.sveditor.core.Tuple;
import net.sf.sveditor.core.db.index.ISVDBIndex;
import net.sf.sveditor.core.db.index.SVDBIndexCollectionMgr;
import net.sf.sveditor.core.db.index.SVDBIndexUtil;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class UpdateProjectSettingsJob extends Job {
	private SVEditor 						fEditor;
	private String							fProjectName;
	
	public UpdateProjectSettingsJob(SVEditor editor, String project_name) {
		super(editor.getTitle() + " - Updating project settings");
		fEditor = editor;
		fProjectName = project_name;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		/*
		fLog.debug("Updating index information for editor \"" + 
				fSVDBFilePath + "\"");
		 */
		Tuple<ISVDBIndex, SVDBIndexCollectionMgr> result;
		String file_path = fEditor.getFilePath();
		
		result = SVDBIndexUtil.findIndexFile(file_path, fProjectName, true);
		
		if (result == null) {
			/*
			fLog.error("Failed to find index for \"" + fSVDBFilePath + "\"");
			 */
			fEditor.int_projectSettingsUpdated(null, null);
		} else {
			fEditor.int_projectSettingsUpdated(result.first(), result.second());
		}
		
		return Status.OK_STATUS;
	}

}
