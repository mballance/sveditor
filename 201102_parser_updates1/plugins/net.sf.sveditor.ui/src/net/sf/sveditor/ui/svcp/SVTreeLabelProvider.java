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


package net.sf.sveditor.ui.svcp;

import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.ISVDBNamedItem;
import net.sf.sveditor.core.db.SVDBFunction;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBModIfcDecl;
import net.sf.sveditor.core.db.SVDBModIfcClassParam;
import net.sf.sveditor.core.db.SVDBParamValueAssign;
import net.sf.sveditor.core.db.SVDBTask;
import net.sf.sveditor.core.db.SVDBTypeInfo;
import net.sf.sveditor.core.db.SVDBTypeInfoUserDef;
import net.sf.sveditor.core.db.stmt.SVDBAlwaysStmt;
import net.sf.sveditor.core.db.stmt.SVDBEventControlStmt;
import net.sf.sveditor.core.db.stmt.SVDBParamPortDecl;
import net.sf.sveditor.core.db.stmt.SVDBVarDeclItem;
import net.sf.sveditor.core.db.stmt.SVDBVarDeclStmt;
import net.sf.sveditor.ui.SVDBIconUtils;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class SVTreeLabelProvider extends LabelProvider implements IStyledLabelProvider {
	protected boolean							fShowFunctionRetType;
	
	private WorkbenchLabelProvider				fLabelProvider;
	
	
	public SVTreeLabelProvider() {
		fLabelProvider = new WorkbenchLabelProvider();
		fShowFunctionRetType = true;
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof ISVDBItemBase) {
			return SVDBIconUtils.getIcon((ISVDBItemBase)element);
		} else {
			return super.getImage(element);
		}
	}
	
	public StyledString getStyledText(Object element) {
		if (element instanceof SVDBVarDeclItem) {
			SVDBVarDeclItem var = (SVDBVarDeclItem)element;
			SVDBVarDeclStmt var_r = var.getParent();
			StyledString ret = new StyledString(var.getName());
			
			if (var_r.getTypeInfo() != null) {
				ret.append(" : " + var_r.getTypeName(), StyledString.QUALIFIER_STYLER);
				
				SVDBTypeInfo type = var_r.getTypeInfo();
				
				if (type.getType() == SVDBItemType.TypeInfoUserDef) {
					SVDBTypeInfoUserDef cls = (SVDBTypeInfoUserDef)type;
					if (cls.getParameters() != null && 
							cls.getParameters().getParameters().size() > 0) {
						ret.append("<", StyledString.QUALIFIER_STYLER);
						
						for (int i=0; i<cls.getParameters().getParameters().size(); i++) {
							SVDBParamValueAssign p = 
								cls.getParameters().getParameters().get(i);
							ret.append(p.getName(), StyledString.QUALIFIER_STYLER);
							if (i+1 < cls.getParameters().getParameters().size()) {
								ret.append(", ", StyledString.QUALIFIER_STYLER);
							}
						}
						
						ret.append(">", StyledString.QUALIFIER_STYLER);
					}
				}
			}
			return ret; 
		} else if (element instanceof ISVDBNamedItem) {
			StyledString ret = new StyledString(((ISVDBNamedItem)element).getName());
			
			if (element instanceof SVDBTask) {
				SVDBTask tf = (SVDBTask)element;
				
				ret.append("(");
				for (int i=0; i<tf.getParams().size(); i++) {
					SVDBParamPortDecl p = tf.getParams().get(i);
					ret.append(p.getTypeName());
					if (i+1 < tf.getParams().size()) {
						ret.append(", ");
					}
				}
				
				ret.append(")");
				
				if (tf.getType() == SVDBItemType.Function) {
					SVDBFunction f = (SVDBFunction)tf;
					if (f.getReturnType() != null && 
							!f.getReturnType().equals("void") &&
							fShowFunctionRetType) {
						ret.append(": " + f.getReturnType(), StyledString.QUALIFIER_STYLER);
					}
				}
			} else if (element instanceof SVDBModIfcDecl) {
				SVDBModIfcDecl decl = (SVDBModIfcDecl)element;

				if (decl.getParameters().size() > 0) {
					ret.append("<", StyledString.QUALIFIER_STYLER);

					for (int i=0; i<decl.getParameters().size(); i++) {
						SVDBModIfcClassParam p = decl.getParameters().get(i);
						ret.append(p.getName(), StyledString.QUALIFIER_STYLER);

						if (i+1 < decl.getParameters().size()) {
							ret.append(", ", StyledString.QUALIFIER_STYLER);
						}
					}
					
					ret.append(">", StyledString.QUALIFIER_STYLER);
				}
			} 
			if (element instanceof SVDBAlwaysStmt) {
				SVDBAlwaysStmt always = (SVDBAlwaysStmt)element;
				if (always.getBody() != null && always.getBody().getType() == SVDBItemType.EventControlStmt) {
					SVDBEventControlStmt stmt = (SVDBEventControlStmt)always.getBody();
					ret = new StyledString(stmt.getExpr().toString().trim());
				}
			}
			
			return ret; 
		} else {
			return new StyledString(element.toString());
		}
	}

	@Override
	public String getText(Object element) {
		return getStyledText(element).toString();
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		fLabelProvider.addListener(listener);
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		fLabelProvider.removeListener(listener);
	}
	
	@Override
	public boolean isLabelProperty(Object element, String property) {
		return fLabelProvider.isLabelProperty(element, property);
	}

	@Override
	public void dispose() {
		super.dispose();
		fLabelProvider.dispose();
	}
}
