package net.sf.sveditor.ui.pref;

import net.sf.sveditor.ui.SVUiPlugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;

public class SVEditorIndexPrefsPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	public SVEditorIndexPrefsPage() {
		super(GRID);
		setPreferenceStore(SVUiPlugin.getDefault().getPreferenceStore());
		setDescription("Index Preferences");
	}

	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void createFieldEditors() {
		addField( new BooleanFieldEditor(SVEditorPrefsConstants.P_AUTO_REBUILD_INDEX, 
				"Enable Index Auto-Rebuild:", getFieldEditorParent()));
		addField( new BooleanFieldEditor(SVEditorPrefsConstants.P_ENABLE_SHADOW_INDEX, 
				"Enable Shadow Index:", getFieldEditorParent()));
		
	}

}
