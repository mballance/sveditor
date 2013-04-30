package net.sf.sveditor.core.db.index.cache.file;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBFileTree;
import net.sf.sveditor.core.db.SVDBMarker;
import net.sf.sveditor.core.db.index.cache.ISVDBIndexCache;
import net.sf.sveditor.core.db.index.cache.ISVDBIndexCacheMgrInt;
import net.sf.sveditor.core.db.persistence.DBFormatException;
import net.sf.sveditor.core.db.persistence.DBWriteException;
import net.sf.sveditor.core.db.persistence.IDBReader;
import net.sf.sveditor.core.db.persistence.IDBWriter;
import net.sf.sveditor.core.db.persistence.SVDBPersistenceRW;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;

public class SVDBFileIndexCacheMgr implements ISVDBIndexCacheMgrInt {
	
	private SVDBFileIndexCacheEntry					fCacheHead;
	private SVDBFileIndexCacheEntry					fCacheTail;
	private int										fCacheSize;
	
	private SVDBFileIndexCacheEntry					fUnCachedHead;
	private SVDBFileIndexCacheEntry					fUnCachedTail;
	
	private int										fMaxCacheSize = 100;
	
	private boolean									fUseSoftRef = true;
	
	private List<IDBReader>							fPersistenceRdrSet;
	private List<IDBWriter>							fPersistenceWriterSet;

	private List<SVDBFileIndexCache>				fIndexList;

	// File ID for the file containing index data
	private int										fIndexDataId;
	
	private SVDBFileSystem							fFileSystem;
	private LogHandle								fLog;
	private boolean									fDebugEn;

	
	public SVDBFileIndexCacheMgr() {
		fPersistenceRdrSet = new ArrayList<IDBReader>();
		fPersistenceWriterSet = new ArrayList<IDBWriter>();
		fLog = LogFactory.getLogHandle("SVDBFileIndexCacheMgr");
		fIndexList = new ArrayList<SVDBFileIndexCache>();
	}
	
	public boolean init(SVDBFileSystem fs) {
		fFileSystem = fs;
		
		fCacheHead = null;
		fCacheTail = null;
		fCacheSize = 0;
		
		fUnCachedHead = null;
		fUnCachedTail = null;
		
		fIndexList.clear();
		fIndexDataId = -1;
		
		// Attempt to load data from the filesystem
		SVDBFileSystemDataInput user_data = fFileSystem.getUserData();
		
		if (user_data != null) {
			try {
				fIndexDataId = user_data.readInt();
				SVDBFileSystemDataInput index_data = fFileSystem.readFile("index data", fIndexDataId);
			
				read_state(index_data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return false;
	}
	
	private void write_state(SVDBFileSystemDataOutput dat) throws IOException, DBWriteException {

		
		// Write back the number of indexes
		dat.writeInt(fIndexList.size());
		
		// Now, write back the project/base location details for each index
		for (SVDBFileIndexCache cache : fIndexList) {
			cache.write(dat);
		}
		
		// Now, write back the number of cache entries
		int n_entries = count_entries(fCacheHead) + count_entries(fUnCachedHead);
		
		dat.writeInt(n_entries);
		SVDBFileIndexCacheEntry entry;
		
		entry = fCacheHead;
		while (entry != null) {
			// Ensure entry contents are written back
			writeBackEntry(entry);
			
			entry.write(dat);
			entry = entry.getNext();
		}
		
		entry = fUnCachedHead;
		while (entry != null) {
			entry.write(dat);
			entry = entry.getNext();
		}
	
	}
	
	private void read_state(SVDBFileSystemDataInput din) throws IOException {
		fIndexList.clear();
		
		// Read back the number of indexes
		int index_list_size = din.readInt();

		for (int i=0; i<index_list_size; i++) {
			SVDBFileIndexCache cache = SVDBFileIndexCache.read(this, din);
			fIndexList.add(cache);
		}
	
		// Read back the cache entries
		int n_entries = din.readInt();
		
		for (int i=0; i<n_entries; i++) {
			SVDBFileIndexCacheEntry entry = SVDBFileIndexCacheEntry.read(din);
			addToUnCachedList(entry);
		}
	}
	
	private int count_entries(SVDBFileIndexCacheEntry entry) {
		int count = 0;
		
		while (entry != null) {
			count++;
			entry = entry.getNext();
		}
		
		return count;
	}

	public void sync() {
		
		// TODO: save cache and entry data to the filesystem
		// - TODO: Write-back any dirty cache entries (future)
		// - Construct the filesystem user data:
		//   - List of index caches
	
		SVDBFileSystemDataOutput dat = new SVDBFileSystemDataOutput();
		SVDBFileSystemDataOutput ud = new SVDBFileSystemDataOutput();

		try {
			if (fIndexDataId != -1) {
				// Delete the file previously used to store index data
				fFileSystem.deleteFile("index info", fIndexDataId);
				fIndexDataId = -1;
			}
			
			write_state(dat);
			
			fIndexDataId = fFileSystem.writeFile("index info", dat);
			
			// User data is:
			// - Handle to index info
			ud.writeInt(fIndexDataId);
			fFileSystem.setUserData(ud);
			
			// Synchronize the filesystem to ensure everything is up-to-date
			fFileSystem.sync();		
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DBWriteException e) {
			e.printStackTrace();
		}
	}
	
	public SVDBFileIndexCache findIndexCache(
			String 			project_name,
			String 			base_location) {
		SVDBFileIndexCache ret = null;
		
		synchronized (fIndexList) {
			for (SVDBFileIndexCache c : fIndexList) { 
				if (c.getProjectName().equals(project_name) &&
						c.getBaseLocation().equals(base_location)) {
					ret = c;
					break;
				}
			}
		}
		
		return ret;
	}

	public SVDBFileIndexCache createIndexCache(
			String 			project_name,
			String 			base_location) {
		SVDBFileIndexCache ret;

		synchronized (fIndexList) {
			int id = -1;
			for (int i=0; i<fIndexList.size(); i++) {
				if (fIndexList.get(i) == null) {
					id = i;
					break;
				}
			}
			
			if (id == -1) {
				ret = new SVDBFileIndexCache(this, 
						fIndexList.size(), project_name, base_location);
				fIndexList.add(ret);
			} else {
				ret = new SVDBFileIndexCache(this, id,
						project_name, base_location);
				fIndexList.set(id, ret);
			}
		}
		
		return ret;
	}

	public void compactCache(List<ISVDBIndexCache> cache_list) {
		// TODO Auto-generated method stub

	}

	public void dispose() {
		// Close down the cache 
		sync();

		try {
			fFileSystem.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		fIndexDataId = -1;
	}

	/**
	 * Remove all entries for this cache
	 * @param cache
	 */
	synchronized void clearIndexCache(SVDBFileIndexCache cache) {
		SVDBFileIndexCacheEntry entry;

		try {
			entry = fCacheHead;
			while (entry != null) {
				if (entry.getCacheId() == cache.getCacheId()) {
					deleteEntry(entry);
				}
				entry = entry.getNext();
			}

			entry = fUnCachedHead;
			while (entry != null) {
				if (entry.getCacheId() == cache.getCacheId()) {
					deleteEntry(entry);
				}
				entry = entry.getNext();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private synchronized void deleteEntry(SVDBFileIndexCacheEntry entry) throws IOException {
		if (!entry.onList()) {
			try {
				throw new Exception("Attempting to remove " + entry.getPath() + " that isn't on list");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		if (entry.isCached()) {
			removeFromCachedList(entry);
		} else {
			removeFromUnCachedList(entry);
		}
		entry.clrOnList();
		
		deleteStorage(entry);
	}


	/**
	 * Synchronize cache entries associated with 'cache' to 
	 * the filesystem
	 * 
	 * @param cache
	 */
	public void sync(SVDBFileIndexCache cache) {
	}

	/**
	 * Removes a client cache from the manager
	 * @param cache
	 */
	void removeIndexCache(SVDBFileIndexCache cache) {
		synchronized (fIndexList) {
			fIndexList.remove(cache);
		}
	
		// Clear the entries of the cache
		clearIndexCache(cache);
	}
	
	/**
	 * Ensures that the cache item is up-to-date
	 * @param entry
	 */
	synchronized void ensureUpToDate(SVDBFileIndexCacheEntry entry) {
		if (entry.isCached()) {
			moveElementToCachedTail(entry);
		} else {
			// Need to bring back into the cache
			readBackEntry(entry);
			
			removeFromUnCachedList(entry);
			
			entry.setCached();
		
			// addToCachedList() ensures index size is observed
			addToCachedList(entry);
		}
	}

	/**
	 * Removes the specified entry from the index cache mgr
	 * 
	 * @param entry
	 */
	synchronized void removeEntry(SVDBFileIndexCacheEntry entry) {
		if (entry.isCached()) {
			removeFromCachedList(entry);
		} else {
			removeFromUnCachedList(entry);
		}

		try {
			deleteStorage(entry);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Deletes storage associated with the specified entry
	 * 
	 * @param entry
	 * @throws IOException
	 */
	private void deleteStorage(SVDBFileIndexCacheEntry entry) throws IOException {
		if (entry.getMarkersId() != -1) {
			fFileSystem.deleteFile(
					entry.getPath(), entry.getMarkersId());
			entry.setMarkersId(-1);
		}
		if (entry.getSVDBFileId() != -1) {
			fFileSystem.deleteFile(
					entry.getPath(), entry.getSVDBFileId());
			entry.setSVDBFileId(-1);
		}
		if (entry.getSVDBPreProcFileId() != -1) {
			fFileSystem.deleteFile(
					entry.getPath(), entry.getSVDBPreProcFileId());
			entry.setSVDBPreProcFileId(-1);
		}
		if (entry.getSVDBFileTreeId() != -1) {
			fFileSystem.deleteFile(
					entry.getPath(), entry.getSVDBFileTreeId());
			entry.setSVDBFileTreeId(-1);
		}	
	}

	/**
	 * Add a new entry to the uncached list
	 * 
	 * @param entry
	 */
	private synchronized void addToUnCachedList(SVDBFileIndexCacheEntry entry) {
		entry.setOnList();
		if (fUnCachedHead == null) {
			// First entry
			fUnCachedHead = entry;
			fUnCachedTail = entry;
			entry.setPrev(null);
			entry.setNext(null);
		} else {
			// Add the new file info to the linked list
			fUnCachedTail.setNext(entry);
			entry.setPrev(fUnCachedTail);
			fUnCachedTail = entry;
			entry.setNext(null);
		}
	}

	/**
	 * This method is used by a cache to find an entry that was previously
	 * saved and restored but not yet associated with the cache
	 * 
	 * @param cache_id
	 * @param path
	 * @return
	 */
	synchronized SVDBFileIndexCacheEntry findCacheEntry(int cache_id, String path) {
		SVDBFileIndexCacheEntry entry = fCacheHead;
		
		while (entry != null) {
			if (entry.getCacheId() == cache_id && entry.getPath().equals(path)) {
				return entry;
			}
			entry = entry.getNext();
		}
		
		entry = fUnCachedHead;
		
		while (entry != null) {
			if (entry.getCacheId() == cache_id && entry.getPath().equals(path)) {
				return entry;
			}
			entry = entry.getNext();
		}
	
		// Failed to find
		return null;
	}
	
	/**
	 * Load the specified cache from entries held by the cache manager
	 * 
	 * @param cache_id
	 * @param cache
	 */
	void loadCache(int cache_id, Map<String, SVDBFileIndexCacheEntry> cache) {
		cache.clear();
		
		SVDBFileIndexCacheEntry entry = fCacheHead;
		
		while (entry != null) {
			if (entry.getCacheId() == cache_id) {
				cache.put(entry.getPath(), entry);
			}
			entry = entry.getNext();
		}
		
		entry = fUnCachedHead;
		
		while (entry != null) {
			if (entry.getCacheId() == cache_id) {
				cache.put(entry.getPath(), entry);
			}
			entry = entry.getNext();
		}		
	}

	public synchronized void addToCachedList(SVDBFileIndexCacheEntry entry) {
		if (entry == null) {
			try {
				throw new Exception();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		entry.setOnList();
		if (fCacheHead == null) {
			// First entry
			fCacheHead = entry;
			fCacheTail = entry;
			entry.setPrev(null);
			entry.setNext(null);
		} else {
			// Add the new file info to the linked list
			fCacheTail.setNext(entry);
			entry.setPrev(fCacheTail);
			fCacheTail = entry;
			entry.setNext(null);
		}
		
		fCacheSize++;
		
		while (fCacheSize > fMaxCacheSize) {
			uncacheEntry(fCacheHead);
		}
	}

	/**
	 * Remove the specified entry from the Uncached list. It is an error
	 * to call this method with entry.isCached() false
	 * 
	 * @param entry
	 */
	synchronized void removeFromUnCachedList(SVDBFileIndexCacheEntry entry) {
		if (entry.getPrev() == null) {
			fUnCachedHead = entry.getNext();
		} else {
			entry.getPrev().setNext(entry.getNext());
		}

		if (entry.getNext() == null) {
			fUnCachedTail = entry.getPrev();
		} else {
			entry.getNext().setPrev(entry.getPrev());
		}
	}

	synchronized void removeFromCachedList(SVDBFileIndexCacheEntry entry) {
		// Ensure the entry is not marked cached
		entry.clearCached();
		
		if (entry.getPrev() == null) {
			fCacheHead = entry.getNext();
		} else {
			entry.getPrev().setNext(entry.getNext());
		}

		if (entry.getNext() == null) {
			fCacheTail = entry.getPrev();
		} else {
			entry.getNext().setPrev(entry.getPrev());
		}
	}

	/**
	 * Clear references in the specified entry, such that 
	 * the system can reclaim memory. 
	 * Write data back to the filesystem.
	 * 
	 * @param info
	 */
	void uncacheEntry(SVDBFileIndexCacheEntry info) {
		// First, 
		if (info.isCached()) {
			// Remove the entry from the cached list, and move
			// to the uncached list
			removeFromCachedList(info);
			
			addToUnCachedList(info);
		
			// Write-back the data
			writeBackEntry(info);
			
			// Release references
			info.clearCached();
			fCacheSize--;
		}
	}
	
	synchronized void moveElementToCachedTail(SVDBFileIndexCacheEntry info) {
		if (!info.isCached()) {
			try {
				throw new Exception();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		
		if (fCacheHead == null && fCacheTail == null) {
			try {
				throw new Exception("moveElement with none on list: " + info.getPath() + " cached=" + info.isCached());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (fCacheTail != info) {
			if (info.getPrev() == null) {
				fCacheHead = info.getNext();
			} else {
				info.getPrev().setNext(info.getNext());
			}

			if (info.getNext() == null) {
				fCacheTail = info.getPrev();
			} else {
				info.getNext().setPrev(info.getPrev());
			}

			if (fCacheHead == null) {
				// First entry
				fCacheHead = info;
				fCacheTail = info;
				info.setPrev(null);
				info.setNext(null);
			} else {
				// Add the file info to the linked list
				fCacheTail.setNext(info);
				info.setPrev(fCacheTail);
				fCacheTail = info;
				info.setNext(null);
			}
		}
	}
	
	private void removeElement(SVDBFileIndexCacheEntry info) {
			if (info.getPrev() == null) {
				fCacheHead = info.getNext();
			} else {
				info.getPrev().setNext(info.getNext());
			}

			if (info.getNext() == null) {
				fCacheTail = info.getPrev();
			} else {
				info.getNext().setPrev(info.getPrev());
			}
			
			// Release references
			info.clearCached();
			fCacheSize--;
	}	
	
	IDBReader allocReader() {
		IDBReader reader = null;
		synchronized (fPersistenceRdrSet) {
			if (fPersistenceRdrSet.size() > 0) {
				reader = fPersistenceRdrSet.remove(fPersistenceRdrSet.size()-1);
			}
		}
		if (reader == null) {
			reader = new SVDBPersistenceRW();
		}
		
		return reader;
	}
	
	void freeReader(IDBReader reader) {
		synchronized (fPersistenceRdrSet) {
			fPersistenceRdrSet.add(reader);
		}
	}	

	IDBWriter allocWriter() {
		IDBWriter writer = null;
		synchronized (fPersistenceWriterSet) {
			if (fPersistenceWriterSet.size() > 0) {
				writer = fPersistenceWriterSet.remove(fPersistenceWriterSet.size()-1);
			}
		}
		if (writer == null) {
			writer = new SVDBPersistenceRW();
		}
		return writer;
	}
	
	void freeWriter(IDBWriter writer) {
		synchronized (fPersistenceWriterSet) {
			fPersistenceWriterSet.add(writer);
		}
	}
	
	private void writeBackEntry(SVDBFileIndexCacheEntry entry) {
		if (entry.getSVDBFileRef() != null) {
			writeBackSVDBFile(entry);
		}
		
		if (entry.getSVDBPreProcFileRef() != null) {
			writeBackSVDBPreProcFile(entry);
		}
		
		if (entry.getSVDBFileTreeRef() != null) {
			writeBackSVDBFileTree(entry);
		}
		
		if (entry.getMarkersRef() != null) {
			writeBackMarkers(entry);
		}
	}
	
	private void readBackEntry(SVDBFileIndexCacheEntry entry) {
		if (entry.getSVDBFileId() != -1) {
			if (entry.getSVDBFileRef() != null) {
				// Just reset the reference
				entry.setSVDBFileRef(entry.getSVDBFileRef());
			} else {
				readBackSVDBFile(entry);
			}
		}
	
		if (entry.getSVDBPreProcFileId() != -1) {
			if (entry.getSVDBPreProcFileRef() != null) {
				// Just reset the reference
				entry.setSVDBPreProcFileRef(entry.getSVDBPreProcFileRef());
			} else {
				readBackSVDBPreProcFile(entry);
			}
		}
		
		if (entry.getSVDBFileTreeId() != -1) {
			if (entry.getSVDBFileTreeRef() != null) {
				entry.setSVDBFileTreeRef(entry.getSVDBFileTreeRef());
			} else {
				readBackSVDBFileTree(entry);
			}
		}
		
		if (entry.getMarkersId() != -1) {
			if (entry.getMarkersRef() != null) {
				entry.setMarkersRef(entry.getMarkersRef());
			} else {
				readBackMarkers(entry);
			}
		}
	}

	private void writeBackSVDBFile(SVDBFileIndexCacheEntry entry) {
		try {
			if (entry.getSVDBFileId() != -1) {
				// Free the old file
				fFileSystem.deleteFile(entry.getPath(), entry.getSVDBFileId());
			}
			IDBWriter writer = allocWriter();
			SVDBFile file = entry.getSVDBFileRef();
			SVDBFileSystemDataOutput data_out = new SVDBFileSystemDataOutput();
			writer.init(data_out);
			writer.writeObject(SVDBFile.class, file);
		
			ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
			DataOutputStream data_out2 = new DataOutputStream(bos2);
			writer.init(data_out2);
			writer.writeObject(SVDBFile.class, file);
			
			int file_id = fFileSystem.writeFile(entry.getPath(), data_out);
			
			entry.setSVDBFileId(file_id);
			
			freeWriter(writer);
		} catch (IOException e) {
			fLog.error("IO Exception during writeback", e);
		} catch (DBWriteException e) {
			fLog.error("DBWrite Exception during writeback", e);
		}
	}

	private void readBackSVDBFile(SVDBFileIndexCacheEntry entry) {
		try {
			IDBReader reader = allocReader();
			SVDBFile file = new SVDBFile();
			SVDBFileSystemDataInput data_in = fFileSystem.readFile(entry.getPath() + ":file", entry.getSVDBFileId());
			reader.init(data_in);
			reader.readObject(null, SVDBFile.class, file);
			
			entry.setSVDBFileRef(file);
			
			freeReader(reader);
		} catch (IOException e) {
			fLog.error("IO Exception during writeback", e);
		} catch (DBFormatException e) {
			fLog.error("DBFormat Exception during file readback " +
					entry.getPath(), e);
		}
	}

	private void writeBackSVDBPreProcFile(SVDBFileIndexCacheEntry entry) {
		try {
			if (entry.getSVDBPreProcFileId() != -1) {
				// Free the old file
				fFileSystem.deleteFile(entry.getPath() + ":preProcFile", entry.getSVDBPreProcFileId());
			}
			IDBWriter writer = allocWriter();
			SVDBFile file = entry.getSVDBPreProcFileRef();
			SVDBFileSystemDataOutput data_out = new SVDBFileSystemDataOutput();
			writer.init(data_out);
			writer.writeObject(SVDBFile.class, file);
			
			int file_id = fFileSystem.writeFile(entry.getPath() + ":preProcFile", data_out);
			
			entry.setSVDBPreProcFileId(file_id);
			
			freeWriter(writer);
		} catch (IOException e) {
			fLog.error("IO Exception during writeback", e);
		} catch (DBWriteException e) {
			fLog.error("DBWrite Exception during writeback", e);
		}
	}
	
	private void readBackSVDBPreProcFile(SVDBFileIndexCacheEntry entry) {
		try {
			IDBReader reader = allocReader();
			SVDBFile file = new SVDBFile();
			SVDBFileSystemDataInput data_in = fFileSystem.readFile(
					entry.getPath() + ":preProcFile", entry.getSVDBPreProcFileId());
			reader.init(data_in);
			reader.readObject(null, SVDBFile.class, file);
			
			entry.setSVDBPreProcFileRef(file);
			
			freeReader(reader);
		} catch (IOException e) {
			fLog.error("IO Exception during writeback", e);
		} catch (DBFormatException e) {
			fLog.error("DBFormat Exception during readback of PreProc File " + entry.getPath(), e);
		}
	}
	
	private synchronized void writeBackSVDBFileTree(SVDBFileIndexCacheEntry entry) {
		try {
			if (entry.getSVDBFileTreeId() != -1) {
				// Free the old file
				fFileSystem.deleteFile(entry.getPath() + ":fileTree", entry.getSVDBFileTreeId());
			}
			IDBWriter writer = allocWriter();
			SVDBFileTree file = entry.getSVDBFileTreeRef();
			SVDBFileSystemDataOutput data_out = new SVDBFileSystemDataOutput();
			writer.init(data_out);
			writer.writeObject(SVDBFileTree.class, file);
			
			int file_id = fFileSystem.writeFile(entry.getPath() + ":fileTree", data_out);
			
			entry.setSVDBFileTreeId(file_id);
			
			freeWriter(writer);
		} catch (IOException e) {
			fLog.error("IO Exception during writeback", e);
		} catch (DBWriteException e) {
			fLog.error("DBWrite Exception during writeback", e);
		}
	}
	
	private void readBackSVDBFileTree(SVDBFileIndexCacheEntry entry) {
		try {
			IDBReader reader = allocReader();
			SVDBFileTree ft = new SVDBFileTree();
			
			SVDBFileSystemDataInput data_in = fFileSystem.readFile(
					entry.getPath() + ":fileTree", entry.getSVDBFileTreeId());
			reader.init(data_in);
			reader.readObject(null, SVDBFileTree.class, ft);
			
			entry.setSVDBFileTreeRef(ft);
			
			freeReader(reader);
		} catch (IOException e) {
			fLog.error("IO Exception during writeback", e);
		} catch (DBFormatException e) {
			fLog.error("DBFormat Exception during readback of FileTree " + entry.getPath(), e);
		}
	}
	
	private void writeBackMarkers(SVDBFileIndexCacheEntry entry) {
		try {
			if (entry.getMarkersId() != -1) {
				// Free the old file
				fFileSystem.deleteFile(entry.getPath() + ":markers", entry.getMarkersId());
			}
			IDBWriter writer = allocWriter();
			List<SVDBMarker> markers = entry.getMarkersRef();
			SVDBFileSystemDataOutput data_out = new SVDBFileSystemDataOutput();
			writer.init(data_out);
			writer.writeItemList(markers);
			
			int file_id = fFileSystem.writeFile(entry.getPath() + ":markers", data_out);
			
			entry.setMarkersId(file_id);
			
			freeWriter(writer);
		} catch (IOException e) {
			fLog.error("IO Exception during writeback", e);
		} catch (DBWriteException e) {
			fLog.error("DBWrite Exception during writeback", e);
		}
	}	

	@SuppressWarnings("unchecked")
	private void readBackMarkers(SVDBFileIndexCacheEntry entry) {
		try {
			IDBReader reader = allocReader();
			SVDBFileSystemDataInput data_in = fFileSystem.readFile(
					entry.getPath() + ":markers", entry.getMarkersId());
			reader.init(data_in);
			List<SVDBMarker> markers = (List<SVDBMarker>)reader.readItemList(null);
			
			entry.setMarkersRef(markers);
			
			freeReader(reader);
		} catch (IOException e) {
			fLog.error("IO Exception during readback", e);
		} catch (DBFormatException e) {
			fLog.error("DBFormat Exception during readback of Markers " + entry.getPath(), e);
		}
	}
}
