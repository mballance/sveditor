package net.sf.sveditor.core.db.index.cache.file;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.List;

import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBFileTree;
import net.sf.sveditor.core.db.SVDBMarker;


public class SVDBFileIndexCacheEntry {
	
	public static final int					SVDB_FILE_MASK 			= (1 << 0);
	public static final int					SVDB_PREPROC_FILE_MASK 	= (1 << 1);
	public static final int					SVDB_FILETREE_MASK 		= (1 << 2);
	public static final int					MARKERS_MASK 			= (1 << 3);
	public static final int					ALL_MASK 				= 0xF;

	private int								fCacheId;
	private String							fPath;
	private int								fType;
	
	private	SVDBFileIndexCacheEntry			fPrev;
	private	SVDBFileIndexCacheEntry			fNext;

	private boolean							fOnList;
	private boolean							fCached;

	/**
	 * Mask indicating which entries are currently 'loaded' 
	 * into this entry. 
	 */
	private int								fLoadedMask;

	/**
	 * Mask indicating which loaded entries are currently dirty.
	 * This means that there is an on-disk image, but it's 
	 * out of date
	 */
	private int								fDirtyMask;

	private SVDBFile						fSVDBFileRef;
	private Reference<SVDBFile>				fSVDBFile;
	// Storage fileid for the SVDBFile
	private int								fSVDBFileId;

	// The PreProcFile is obsolete. In ArgFileIndex2, pre-processor
	// information is stored in the FileTree
	private SVDBFile						fSVDBPreProcFileRef;
	private Reference<SVDBFile>				fSVDBPreProcFile;
	private int								fSVDBPreProcFileId;
	
	private SVDBFileTree					fSVDBFileTreeRef;
	private Reference<SVDBFileTree>			fSVDBFileTree;
	private int								fSVDBFileTreeId;
	
	private List<SVDBMarker>				fMarkersRef;
	private Reference<List<SVDBMarker>>		fMarkers;
	private int								fMarkersId;
	
	private long							fLastModified;

	public SVDBFileIndexCacheEntry(int cache_id, String path, int type) {
		fCacheId = cache_id;
		fPath = path;
		fSVDBFileId = -1;
		fSVDBPreProcFileId = -1;
		fSVDBFileTreeId = -1;
		fMarkersId = -1;
		fCached = false;
		fType = type;
		fOnList = false;
	
		// Initially, consider all entries 
		fDirtyMask = 0;
		fLoadedMask = ALL_MASK;
	}
	
	public void setOnList() {
		fOnList = true;
	}
	
	public void clrOnList() {
		fOnList = false;
	}
	
	public boolean onList() {
		return fOnList;
	}
	
	public void write(SVDBFileSystemDataOutput dos) throws IOException {
		// Save just enough information that the entry can be restored
		dos.writeInt(fCacheId);
		dos.writeString(fPath);
		dos.writeInt(fType);
		dos.writeInt(fSVDBFileId);
		dos.writeInt(fSVDBPreProcFileId);
		dos.writeInt(fSVDBFileTreeId);
		dos.writeInt(fMarkersId);
		dos.writeLong(fLastModified);
	}
	
	public static SVDBFileIndexCacheEntry read(SVDBFileSystemDataInput dis) throws IOException {
		SVDBFileIndexCacheEntry ret = null;
		
		int cache_id = dis.readInt();
		String path = dis.readString();
		int type = dis.readInt();
		
		ret = new SVDBFileIndexCacheEntry(cache_id, path, type);
		ret.setSVDBFileId(dis.readInt());
		ret.setSVDBPreProcFileId(dis.readInt());
		ret.setSVDBFileTreeId(dis.readInt());
		ret.setMarkersId(dis.readInt());
		ret.setLastModified(dis.readLong());
		
		return ret;
	}
	
	public int getCacheId() {
		return fCacheId;
	}
	
	public void setCacheId(int id) {
		fCacheId = id;
	}
	
	public int dirtyMask() {
		return fDirtyMask;
	}
	
	public void setDirtyMask(int mask) {
		fDirtyMask = mask;
	}
	
	public int loadedMask() {
		return fLoadedMask;
	}
	
	public void setLoadedMask(int mask) {
		fLoadedMask = mask;
	}
	
	public String getPath() {
		return fPath;
	}
	
	public int getType() {
		return fType;
	}
	
	SVDBFileIndexCacheEntry getPrev() {
		return fPrev;
	}
	
	void setPrev(SVDBFileIndexCacheEntry prev) {
		fPrev = prev;
	}

	SVDBFileIndexCacheEntry getNext() {
		return fNext;
	}
	
	void setNext(SVDBFileIndexCacheEntry next) {
		fNext = next;
	}
	
	boolean isCached() {
		return fCached;
	}

	/**
	 * clear references and set 'cached' to false
	 */
	void clearCached() {
		fSVDBFileRef = null;
		fSVDBFileTreeRef = null;
		fSVDBPreProcFileRef = null;
		fMarkersRef = null;
		
		// Assume nothing loaded until entry is brought back into the cache
		fLoadedMask = 0;
		fCached = false;
	}

	/**
	 * 
	 * @return
	 */
	int setCached() {
		fCached = true;
		if (fSVDBFile != null) {
			fSVDBFileRef = fSVDBFile.get();
			if (fSVDBFileRef != null) {
				fLoadedMask |= SVDB_FILE_MASK;
			}
		}
		if (fSVDBFileTree != null) {
			fSVDBFileTreeRef = fSVDBFileTree.get();
			if (fSVDBFileTreeRef != null) {
				fLoadedMask |= SVDB_FILETREE_MASK;
			}
		}
		if (fSVDBPreProcFile != null) {
			fSVDBPreProcFileRef = fSVDBPreProcFile.get();
			if (fSVDBPreProcFileRef != null) {
				fLoadedMask |= SVDB_PREPROC_FILE_MASK;
			}
		}
		if (fMarkers != null) {
			fMarkersRef = fMarkers.get();
			if (fMarkersRef != null) {
				fLoadedMask |= MARKERS_MASK;
			}
		}
		
		return fLoadedMask;
	}
	
	SVDBFile getSVDBFileRef() {
		if (fSVDBFile != null) {
			return fSVDBFile.get();
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	void setSVDBFileRef(SVDBFile file) {
		fSVDBFileRef = file;
		fSVDBFile = (Reference<SVDBFile>)createRef(file);
		fDirtyMask |= SVDB_FILE_MASK;
	}
	
	int getSVDBFileId() {
		return fSVDBFileId;
	}
	
	void setSVDBFileId(int id) {
		fSVDBFileId = id;
	}
	
	SVDBFile getSVDBPreProcFileRef() {
		if (fSVDBPreProcFile != null) {
			return fSVDBPreProcFile.get();
		} else {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	void setSVDBPreProcFileRef(SVDBFile file) {
		fSVDBPreProcFileRef = file;
		fSVDBPreProcFile = (Reference<SVDBFile>)createRef(file);
		fDirtyMask |= SVDB_PREPROC_FILE_MASK;
	}
	
	int getSVDBPreProcFileId() {
		return fSVDBPreProcFileId;
	}
	
	void setSVDBPreProcFileId(int id) {
		fSVDBPreProcFileId = id;
	}
	
	SVDBFileTree getSVDBFileTreeRef() {
		if (fSVDBFileTree != null) {
			return fSVDBFileTree.get();
		} else {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	void setSVDBFileTreeRef(SVDBFileTree ft) {
		fSVDBFileTreeRef = ft;
		fSVDBFileTree = (Reference<SVDBFileTree>)createRef(ft);
		fDirtyMask |= SVDB_FILETREE_MASK;
	}
	
	int getSVDBFileTreeId() {
		return fSVDBFileTreeId;
	}
	
	void setSVDBFileTreeId(int id) {
		fSVDBFileTreeId = id;
	}
	
	List<SVDBMarker> getMarkersRef() {
		if (fMarkers != null) {
			return fMarkers.get();
		} else {
			return null;
		}
	}
	
	int getMarkersId() {
		return fMarkersId;
	}
	
	void setMarkersId(int id) {
		fMarkersId = id;
	}
	
	@SuppressWarnings("unchecked")
	public void setMarkersRef(List<SVDBMarker> markers) {
		fDirtyMask |= MARKERS_MASK;
		fMarkers = (Reference<List<SVDBMarker>>)createRef(markers);
		fMarkersRef = markers;
	}

	@SuppressWarnings("unchecked")
	void setFileTree(SVDBFileTree ft) {
		fSVDBFileTree = (Reference<SVDBFileTree>)createRef(ft);
		fSVDBFileTreeRef = ft;
	}
	
	@SuppressWarnings("unchecked")
	void setPreProcFile(SVDBFile file) {
		fSVDBPreProcFile = (Reference<SVDBFile>)createRef(file);
		fSVDBPreProcFileRef = file;
	}
	
	@SuppressWarnings("unchecked")
	void setFile(SVDBFile file) {
		fSVDBFile    = (Reference<SVDBFile>)createRef(file);
		fSVDBFileRef = file;
	}
	
	long getLastModified() {
		return fLastModified;
	}
	
	void setLastModified(long modified) {
		fLastModified = modified;
	}

	@SuppressWarnings({"unchecked","rawtypes"})
	private Reference createRef(Object r) {
		return new SoftReference(r);
	}
	
}
