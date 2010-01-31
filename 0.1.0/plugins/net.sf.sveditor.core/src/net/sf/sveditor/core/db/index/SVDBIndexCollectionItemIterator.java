package net.sf.sveditor.core.db.index;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBScopeItem;

public class SVDBIndexCollectionItemIterator implements ISVDBItemIterator<SVDBItem> {
	List<ISVDBIndex>			fIndexList;
	int							fIndexListIdx = 0;
	ISVDBItemIterator<SVDBItem>	fIndexIterator;
	ISVDBIndex					fOverrideIndex;
	SVDBFile					fOverrideFile;
	
	public SVDBIndexCollectionItemIterator() {
		fIndexList = new ArrayList<ISVDBIndex>();
	}
	
	public void setOverride(ISVDBIndex index, SVDBFile file) {
		fOverrideIndex = index;
		fOverrideFile  = file;
	}
	
	public void addIndex(ISVDBIndex index) {
		fIndexList.add(index);
	}

	public boolean hasNext() {
		if (fIndexIterator != null && !fIndexIterator.hasNext()) {
			fIndexIterator = null;
		}
		
		while ((fIndexIterator == null || !fIndexIterator.hasNext()) &&
				fIndexListIdx < fIndexList.size()) {
			fIndexIterator = fIndexList.get(fIndexListIdx).getItemIterator();
			fIndexListIdx++;
		}
		
		return ((fIndexIterator != null && fIndexIterator.hasNext())
				|| fIndexListIdx < fIndexList.size());
	}

	public SVDBItem nextItem() {
		boolean had_next = hasNext();
		
		if (fIndexIterator != null && !fIndexIterator.hasNext()) {
			fIndexIterator = null;
		}

		if (fIndexIterator == null && fIndexListIdx < fIndexList.size()) {
			fIndexIterator = fIndexList.get(fIndexListIdx).getItemIterator();
			fIndexListIdx++;
		}

		if (fIndexList.get(fIndexListIdx-1) == fOverrideIndex) {
			((SVDBIndexItemIterator)fIndexIterator).setOverride(fOverrideFile);
		}

		SVDBItem ret = null;
		if (fIndexIterator != null) {
			ret = fIndexIterator.nextItem();
		}
		
		if (ret == null && had_next) {
			System.out.println("[ERROR] ret == null && had_next");
			try {
				throw new Exception();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return ret;
	}
	
}
