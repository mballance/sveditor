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


package net.sf.sveditor.ui;

import java.util.HashMap;
import java.util.Map;

import net.sf.sveditor.core.db.IFieldItemAttr;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.stmt.SVDBStmt;
import net.sf.sveditor.core.db.stmt.SVDBTypedefStmt;
import net.sf.sveditor.core.db.stmt.SVDBVarDeclItem;
import net.sf.sveditor.core.db.stmt.SVDBVarDeclStmt;

import org.eclipse.swt.graphics.Image;

public class SVDBIconUtils implements ISVIcons {
	
	private static final Map<SVDBItemType, String>		fImgDescMap;
	
	static {
		fImgDescMap = new HashMap<SVDBItemType, String>();

		fImgDescMap.put(SVDBItemType.File, FILE_OBJ);
		fImgDescMap.put(SVDBItemType.ModuleDecl, MODULE_OBJ);
		fImgDescMap.put(SVDBItemType.InterfaceDecl, INT_OBJ);
		fImgDescMap.put(SVDBItemType.ClassDecl, CLASS_OBJ);
		fImgDescMap.put(SVDBItemType.MacroDef, DEFINE_OBJ);
		fImgDescMap.put(SVDBItemType.Include, INCLUDE_OBJ);
		fImgDescMap.put(SVDBItemType.PackageDecl, PACKAGE_OBJ);
		fImgDescMap.put(SVDBItemType.TypeInfoStruct, STRUCT_OBJ);
		fImgDescMap.put(SVDBItemType.Covergroup, COVERGROUP_OBJ);
		fImgDescMap.put(SVDBItemType.Coverpoint, COVERPOINT_OBJ);
		fImgDescMap.put(SVDBItemType.CoverpointCross, COVERPOINT_CROSS_OBJ);
		fImgDescMap.put(SVDBItemType.Sequence, SEQUENCE_OBJ);
		fImgDescMap.put(SVDBItemType.Property, PROPERTY_OBJ);
		fImgDescMap.put(SVDBItemType.Constraint, CONSTRAINT_OBJ);
		fImgDescMap.put(SVDBItemType.AlwaysStmt, ALWAYS_BLOCK_OBJ);
		fImgDescMap.put(SVDBItemType.InitialStmt, INITIAL_OBJ);
		fImgDescMap.put(SVDBItemType.Assign, ASSIGN_OBJ);
		fImgDescMap.put(SVDBItemType.GenerateBlock, GENERATE_OBJ);
		fImgDescMap.put(SVDBItemType.ClockingBlock, CLOCKING_OBJ);
		fImgDescMap.put(SVDBItemType.ImportItem, IMPORT_OBJ);
		fImgDescMap.put(SVDBItemType.ModIfcInst, MOD_IFC_INST_OBJ);
		fImgDescMap.put(SVDBItemType.ModIfcInstItem, MOD_IFC_INST_OBJ);
	}
	
	public static Image getIcon(String key) {
		return SVUiPlugin.getImage(key);
	}
	
	public static Image getIcon(ISVDBItemBase it) {
		if (it.getType() == SVDBItemType.VarDeclItem) {
			SVDBVarDeclItem decl = (SVDBVarDeclItem)it;
			SVDBVarDeclStmt decl_p = decl.getParent();
			
			if (decl_p == null) {
				System.out.println("Parent of " + decl.getName() + " @ " + decl.getLocation().getLine() + " is NULL");
			}
			int attr = decl_p.getAttr();
			if (decl_p.getParent() != null && 
					(decl_p.getParent().getType() == SVDBItemType.Task ||
							decl_p.getParent().getType() == SVDBItemType.Function)) {
				return SVUiPlugin.getImage(LOCAL_OBJ);
			} else {
				if ((attr & IFieldItemAttr.FieldAttr_Local) != 0) {
					return SVUiPlugin.getImage(FIELD_PRIV_OBJ);
				} else if ((attr & IFieldItemAttr.FieldAttr_Protected) != 0) {
					return SVUiPlugin.getImage(FIELD_PROT_OBJ);
				} else {
					return SVUiPlugin.getImage(FIELD_PUB_OBJ);
				}
			}
		} else if (it instanceof IFieldItemAttr) {
			int            attr = ((IFieldItemAttr)it).getAttr();
			SVDBItemType   type = it.getType();
			
			if (type == SVDBItemType.ModIfcInstItem) {
				return SVUiPlugin.getImage(MOD_IFC_INST_OBJ);
			} else if (type == SVDBItemType.Task || 
					type == SVDBItemType.Function) {
				if ((attr & IFieldItemAttr.FieldAttr_Local) != 0) {
					return SVUiPlugin.getImage(TASK_PRIV_OBJ);
				} else if ((attr & IFieldItemAttr.FieldAttr_Protected) != 0) {
					return SVUiPlugin.getImage(TASK_PROT_OBJ);
				} else {
					return SVUiPlugin.getImage(TASK_PUB_OBJ);
				}
			} else if (SVDBStmt.isType(it, SVDBItemType.ParamPortDecl)) {
				return SVUiPlugin.getImage(LOCAL_OBJ);
			}
		} else if (it instanceof ISVDBItemBase) {
			SVDBItemType type = ((ISVDBItemBase)it).getType();
			
			if (fImgDescMap.containsKey(type)) {
				return SVUiPlugin.getImage(fImgDescMap.get(type));
			} else if (it.getType() == SVDBItemType.TypedefStmt) {
				SVDBTypedefStmt td = (SVDBTypedefStmt)it;
				
				if (td.getTypeInfo().getType() == SVDBItemType.TypeInfoEnum) {
					return SVUiPlugin.getImage(ENUM_TYPE_OBJ);
				} else {
					return SVUiPlugin.getImage(TYPEDEF_TYPE_OBJ);
				}
			}
		}
		
		return null;
	}
}
