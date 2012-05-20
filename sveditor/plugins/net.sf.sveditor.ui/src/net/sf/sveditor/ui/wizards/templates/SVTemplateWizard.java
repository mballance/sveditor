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


package net.sf.sveditor.ui.wizards.templates;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.SVFileUtils;
import net.sf.sveditor.core.templates.ITemplateFileCreator;
import net.sf.sveditor.core.templates.TemplateProcessor;
import net.sf.sveditor.core.text.TagProcessor;
import net.sf.sveditor.ui.SVUiPlugin;
import net.sf.sveditor.ui.wizards.ISVSubWizard;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public class SVTemplateWizard extends BasicNewResourceWizard {
	public static final String						ID = SVUiPlugin.PLUGIN_ID + ".svMethodologyClass";
	private SVTemplateSelectionPage						fBasicsPage;
	private SVTemplateParameterPage						fParamsPage;
	private ISVSubWizard								fSubWizard;
	private Map<String, Object>							fOptions;
	

	public SVTemplateWizard() {
		super();
		fOptions = new HashMap<String, Object>();
	}
	
	public void addPages() {
		super.addPages();
		
		fBasicsPage = new SVTemplateSelectionPage();
		fParamsPage = new SVTemplateParameterPage();
		
		Object sel = getSelection().getFirstElement();
		if (sel != null && sel instanceof IResource) {
			IResource r = (IResource)sel;
			
			if (!(r instanceof IContainer)) {
				r = r.getParent();
			}
			fParamsPage.setSourceFolder(r.getFullPath().toOSString());
		}
		addPage(fBasicsPage);
		addPage(fParamsPage);
	}
	
	@Override
	public boolean canFinish() {
		if (fSubWizard != null) {
			return fSubWizard.canFinish();
		} else {
			return super.canFinish();
		}
	}
	
	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		IWizardPage next;
		
		if (fSubWizard != null) {
			next = fSubWizard.getNextPage(page);
		} else {
			next = super.getNextPage(page);
		}
		
		if (next == fParamsPage) {
			fParamsPage.setTemplate(fBasicsPage.getTemplate());
		}
		
		return next;
	}
	
	@Override
	public IWizardPage getPreviousPage(IWizardPage page) {
		if (fSubWizard != null) {
			return fSubWizard.getPreviousPage(page);
		} else {
			return super.getPreviousPage(page);
		}
	}
	
	public void setSubWizard(ISVSubWizard sub) {
		fSubWizard = sub;
		if (fSubWizard != null) {
			fSubWizard.init(this, fOptions);
		}
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);
		setNeedsProgressMonitor(true);
		
		SVCorePlugin.getDefault().getTemplateRgy().load_extensions();
	}

	@Override
	public boolean performFinish() {
		final IContainer folder = SVFileUtils.getWorkspaceFolder(fParamsPage.getSourceFolder());
		final TagProcessor tp = fParamsPage.getTagProcessor(false);
		

		try {
			getContainer().run(true, true, new IRunnableWithProgress() {

				public void run(final IProgressMonitor monitor) 
						throws InvocationTargetException, InterruptedException {
					// TODO:
					monitor.beginTask("Creating Files", 5 /*fParamsPage.getFileNames().size()*/);
					TemplateProcessor templ_proc = new TemplateProcessor(new ITemplateFileCreator() {

						public void createFile(String path, InputStream content) {
							IFile file = folder.getFile(new Path(path));

							monitor.worked(1);
							try {
								if (file.exists()) {
									file.setContents(content, true, true, new NullProgressMonitor());
								} else {
									file.create(content, true, new NullProgressMonitor());
								}
							} catch (CoreException e) {
								e.printStackTrace();
							}
						}
					});
					templ_proc.process(fBasicsPage.getTemplate(), tp);
					monitor.done();
				}
			});
		} catch (InterruptedException e) {}
		catch (InvocationTargetException e) {}
		
//		final IFile file_path = c.getFile(new Path(fBasicsPage.getFileName()));
	
		
		/*
		ISVDBIndexIterator index_it = null;
		if (fBasicsPage.getProjectData() != null) {
			index_it = fBasicsPage.getProjectData().getProjectIndexMgr();
		}
		final ISVDBIndexIterator index_it_f = index_it;
		 */
		
		/*
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				NewClassGenerator gen = new NewClassGenerator();
				gen.generate(index_it_f, file_path, fBasicsPage.getName(), 
						fBasicsPage.getSuperClass(), fBasicsPage.getOverrideNew(), monitor);
			}
		};
		
		try {
			getContainer().run(false, false, op);
		} catch (Exception e) {
			return false;
		}
		 */
	
		/*
		try {
			SVEditorUtil.openEditor("${workspace_loc}/" + file_path.getFullPath());
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		 */

		return true;
	}


}
