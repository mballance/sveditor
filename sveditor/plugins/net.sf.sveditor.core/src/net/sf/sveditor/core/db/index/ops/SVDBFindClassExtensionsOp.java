package net.sf.sveditor.core.db.index.ops;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import net.sf.sveditor.core.db.SVDBClassDecl;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.index.ISVDBIndex;
import net.sf.sveditor.core.db.index.ISVDBIndexOperation;
import net.sf.sveditor.core.db.index.ISVDBIndexOperationRunner;
import net.sf.sveditor.core.db.index.SVDBContainerDeclCacheItem;
import net.sf.sveditor.core.db.index.SVDBDeclCacheItem;
import net.sf.sveditor.core.db.refs.ISVDBRefSearchSpec;
import net.sf.sveditor.core.db.refs.ISVDBRefVisitor;
import net.sf.sveditor.core.db.refs.SVDBRefItem;
import net.sf.sveditor.core.db.refs.SVDBRefSearchSpecClassExtension;

public class SVDBFindClassExtensionsOp implements ISVDBIndexOperation, ISVDBRefVisitor {
	private SVDBClassDecl					fCls;
	private String							fClsName;
	private List<SVDBDeclCacheItem>			fExtensions;
	private Set<String>						fFoundSet;
	
	public SVDBFindClassExtensionsOp(SVDBClassDecl cls) {
		fCls = cls;
		fClsName = cls.getName();
		fExtensions = new ArrayList<SVDBDeclCacheItem>();
		fFoundSet = new HashSet<String>();
	}
	
	public SVDBFindClassExtensionsOp(String cls) {
		fClsName = cls;
		fExtensions = new ArrayList<SVDBDeclCacheItem>();
		fFoundSet = new HashSet<String>();
	}

	@Override
	public void index_operation(IProgressMonitor monitor, ISVDBIndex index) {
		ISVDBRefSearchSpec rs = new SVDBRefSearchSpecClassExtension(fCls);
		
		index.findReferences(monitor, rs, this);
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void visitRef(ISVDBRefSearchSpec ref_spec, SVDBRefItem item) {
		
		if (item.getLeaf().getType() == SVDBItemType.ClassDecl) {
			SVDBClassDecl cls = (SVDBClassDecl)item.getLeaf();
			if (cls.getSuperClass() != null && 
					cls.getSuperClass().getName() != null &&
					cls.getSuperClass().getName().equals(fClsName)) {
				if (fFoundSet.add(cls.getName())) {
					fExtensions.add(new SVDBContainerDeclCacheItem(cls));
				}
			}
		}
	}

	public static List<SVDBDeclCacheItem> execOp(
			IProgressMonitor			monitor,
			ISVDBIndexOperationRunner 	index,
			SVDBClassDecl				cls) {
		SVDBFindClassExtensionsOp op = new SVDBFindClassExtensionsOp(cls);
		
		index.execOp(monitor, op, false);
		
		return op.fExtensions;
	}
	
	public static List<SVDBDeclCacheItem> execOp(
			IProgressMonitor			monitor,
			ISVDBIndexOperationRunner 	index,
			String						name) {
		SVDBFindClassExtensionsOp op = new SVDBFindClassExtensionsOp(name);
		
		index.execOp(monitor, op, false);
		
		return op.fExtensions;
	}
	
}
