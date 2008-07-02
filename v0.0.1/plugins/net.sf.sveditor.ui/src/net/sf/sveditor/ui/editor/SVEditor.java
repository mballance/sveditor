package net.sf.sveditor.ui.editor;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class SVEditor extends TextEditor {
	private ListenerList				fReconcileListeners;
	private SVOutlinePage				fOutline;
	private SVHighlightingManager		fHighlightManager;

	public SVEditor() {
		super();
		
		fReconcileListeners = new ListenerList();
		setDocumentProvider(SVEditorDocumentProvider.getDefault());
		
	}
	
	protected void initializeEditor() {
		super.initializeEditor();
	}
	
	@Override
	public void dispose() {
		super.dispose();
		
		if (fOutline != null) {
			fOutline.dispose();
			fOutline = null;
		}
	}

	public void createPartControl(Composite parent) {
		setSourceViewerConfiguration(new SVSourceViewerConfiguration(this));
		
		super.createPartControl(parent);
		
		if (fHighlightManager == null) {
			fHighlightManager = new SVHighlightingManager();
			fHighlightManager.install(
					(SourceViewer)getSourceViewer(),
					(SVPresentationReconciler)getSourceViewerConfiguration().getPresentationReconciler(getSourceViewer()),
					this);
		}
		
		
		/**
		 * Add semantic highlighting
		 */
		
	}
	
	public void setSelection(int lineno) {
		/*
		ISourceViewer viewer = getSourceViewer();
		viewer.setSelectedRange(offset, length);
		viewer.revealRange(offset, length);
		 */
//		ITextViewerExtension5 ext = (ITextViewerExtension5)getSourceViewer();
//		ext.exposeModelRange()
		
		IDocument doc = getDocumentProvider().getDocument(getEditorInput());
		try {
			int offset = doc.getLineOffset(lineno);
			setHighlightRange(offset, 1, false);
			getSourceViewer().revealRange(offset, 1);
		} catch (BadLocationException e) {
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		if (adapter.equals(IContentOutlinePage.class)) {
			if (fOutline == null) {
				fOutline = new SVOutlinePage(this);
			}
			return fOutline;
		}
		return super.getAdapter(adapter);
	}
}
