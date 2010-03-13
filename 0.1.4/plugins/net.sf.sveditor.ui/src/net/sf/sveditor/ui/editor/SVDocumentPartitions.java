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


package net.sf.sveditor.ui.editor;


public interface SVDocumentPartitions {
	String SV_MULTILINE_COMMENT  = "__sv_multiline_comment";
	String SV_SINGLELINE_COMMENT = "__sv_multiline_comment";
	String SV_KEYWORD            = "__sv_keyword";
	String SV_STRING             = "__sv_string";
	String SV_CODE				 = "__sv_code";
	
	
	String[] SV_PARTITION_TYPES = {
			SV_MULTILINE_COMMENT,
			SV_SINGLELINE_COMMENT,
			SV_CODE
	};
	
	String SV_PARTITIONING = "__sv_partitioning";
}
