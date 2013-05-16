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


package net.sf.sveditor.ui.pref;
import net.sf.sveditor.ui.SVUiPlugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class SVEditorPrefsPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public SVEditorPrefsPage() {
		super(GRID);
		setPreferenceStore(SVUiPlugin.getDefault().getPreferenceStore());
		setDescription("Syntax Coloring");
	}

	public void createFieldEditors() {
		addField( new ColorStyleFieldEditor(SVEditorPrefsConstants.P_DEFAULT_C, "D&efault text color:", SVEditorPrefsConstants.P_DEFAULT_S, getFieldEditorParent()));
		addField( new ColorStyleFieldEditor(SVEditorPrefsConstants.P_COMMENT_C, "&Comment color:", SVEditorPrefsConstants.P_COMMENT_S, getFieldEditorParent()));
		addField( new ColorStyleFieldEditor(SVEditorPrefsConstants.P_STRING_C , "&String color:", SVEditorPrefsConstants.P_STRING_S, getFieldEditorParent()));
		addField( new ColorStyleFieldEditor(SVEditorPrefsConstants.P_KEYWORD_C, "Ke&yword color:", SVEditorPrefsConstants.P_KEYWORD_S, getFieldEditorParent()));
		addField( new ColorStyleFieldEditor(SVEditorPrefsConstants.P_NUMBERS_C, "N&umber color:", SVEditorPrefsConstants.P_NUMBERS_S, getFieldEditorParent()));
		addField( new ColorStyleFieldEditor(SVEditorPrefsConstants.P_BRACE_C  , "B&racket color:", SVEditorPrefsConstants.P_BRACE_S, getFieldEditorParent()));
		addField( new ColorStyleFieldEditor(SVEditorPrefsConstants.P_OPERATORS_C, "O&perator color:", SVEditorPrefsConstants.P_OPERATORS_S, getFieldEditorParent()));
		addField( new ColorStyleFieldEditor(SVEditorPrefsConstants.P_SVT_PARAMETERS_S, "SVT Template Parameter color:", 
				SVEditorPrefsConstants.P_SVT_PARAMETERS_S, getFieldEditorParent()));
		
		addField( new BooleanFieldEditor(SVEditorPrefsConstants.P_AUTO_INDENT_ENABLED_S, "Enable Auto-&Indent:", getFieldEditorParent()));

		addField( new ComboFieldEditor(SVEditorPrefsConstants.P_DEBUG_LEVEL_S, "Debug &Level:", 
				new String[][] {
					{"Off", "LEVEL_OFF"}, 
					{"Minimum", "LEVEL_MIN"}, 
					{"Medium", "LEVEL_MID"},
					{"Maximum", "LEVEL_MAX"}}, getFieldEditorParent()));
		addField( new BooleanFieldEditor(SVEditorPrefsConstants.P_DEBUG_CONSOLE_S, 
				"Debu&g to Console:", getFieldEditorParent()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
}
