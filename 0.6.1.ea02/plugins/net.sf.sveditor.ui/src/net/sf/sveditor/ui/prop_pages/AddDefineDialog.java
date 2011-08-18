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


package net.sf.sveditor.ui.prop_pages;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class AddDefineDialog extends Dialog {
	private Text				fName;
	private String				fNameStr;
	private Text				fValue;
	private String				fValueStr;
	
	public AddDefineDialog(Shell shell) {
		super(shell);
	}
	
	public void setInitialName(String path) {
		fNameStr = path;
	}
	
	public void setInitialValue(String value) {
		fValueStr = value;
	}
	
	public String getName() {
		return fNameStr;
	}
	
	public String getValue() {
		return fValueStr;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite frame = new Composite(parent, SWT.NONE);
		frame.setLayout(new GridLayout(2, false));

		Label l;
		GridData gd;
		
		l = new Label(frame, SWT.NONE);
		l.setText("Name:");
		fName = new Text(frame, SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.widthHint = 250;
		fName.setLayoutData(gd);
		fName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fNameStr = fName.getText();
			}
		});
		
		if (fNameStr != null) {
			fName.setText(fNameStr);
		}

		l = new Label(frame, SWT.NONE);
		l.setText("Value:");
		fValue = new Text(frame, SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.widthHint = 250;
		fValue.setLayoutData(gd);
		fValue.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fValueStr = fValue.getText();
			}
		});
		
		if (fValueStr != null) {
			fValue.setText(fValueStr);
		} else {
			fValueStr = "";
		}

		return frame;
	}
	
	

}
