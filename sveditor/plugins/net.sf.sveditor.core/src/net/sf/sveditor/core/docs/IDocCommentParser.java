/****************************************************************************
 * Copyright (c) 2008-2010 Matthew Ballance and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Armond Paiva - initial implementation
 ****************************************************************************/

package net.sf.sveditor.core.docs;

import java.util.Set;

import net.sf.sveditor.core.docs.model.DocTopic;

public interface IDocCommentParser {

	public String isDocComment(String comment) ;
	
	public void parse(String comment, Set<DocTopic> docTopics) ;
	
	public int parseComment(String lines[], Set<DocTopic> parsedTopics) ;
		
}
