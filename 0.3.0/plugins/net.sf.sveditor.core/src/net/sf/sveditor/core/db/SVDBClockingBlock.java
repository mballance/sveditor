package net.sf.sveditor.core.db;

import net.sf.sveditor.core.db.persistence.DBFormatException;
import net.sf.sveditor.core.db.persistence.IDBReader;
import net.sf.sveditor.core.db.persistence.IDBWriter;
import net.sf.sveditor.core.db.persistence.ISVDBPersistenceFactory;
import net.sf.sveditor.core.db.persistence.SVDBPersistenceReader;

public class SVDBClockingBlock extends SVDBScopeItem {
	
	public static void init() {
		ISVDBPersistenceFactory f = new ISVDBPersistenceFactory() {
			
			public SVDBItem readSVDBItem(IDBReader reader, SVDBItemType type,
					SVDBFile file, SVDBScopeItem parent) throws DBFormatException {
				return new SVDBClockingBlock(file, parent, type, reader);
			}
		};
		SVDBPersistenceReader.registerPersistenceFactory(f, SVDBItemType.ClockingBlock);
	}
	
	public SVDBClockingBlock(String name) {
		super(name, SVDBItemType.ClockingBlock);
	}
	
	public SVDBClockingBlock(SVDBFile file, SVDBScopeItem parent, SVDBItemType type, IDBReader reader) throws DBFormatException {
		super(file, parent, type, reader);
	}

	@Override
	public void dump(IDBWriter writer) {
		super.dump(writer);
	}

	@Override
	public SVDBItem duplicate() {
		SVDBClockingBlock ret = new SVDBClockingBlock(getName());
		ret.init(this);
		
		return ret;
	}

	@Override
	public void init(SVDBItem other) {
		super.init(other);
	}

}
