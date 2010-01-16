package net.sf.sveditor.core.db;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.db.persistence.DBFormatException;
import net.sf.sveditor.core.db.persistence.IDBReader;
import net.sf.sveditor.core.db.persistence.IDBWriter;

public class SVDBTypedef extends SVDBItem {
	private List<String>					fEnumNames;
	private List<Integer>					fEnumVals;
	private boolean							fIsEnum;
	private String							fTypeName;
	
	
	public SVDBTypedef(String type, String name) {
		super(name, SVDBItemType.Typedef);
		fTypeName = type;
	}
	
	public SVDBTypedef(String name) {
		super(name, SVDBItemType.Typedef);
		
		fIsEnum = true;
		fEnumVals = new ArrayList<Integer>();
		fEnumNames = new ArrayList<String>();
	}
	
	public SVDBTypedef(SVDBFile file, SVDBScopeItem parent, SVDBItemType type, IDBReader reader) throws DBFormatException {
		super(file, parent, type, reader);
		
		fIsEnum = (reader.readInt() != 0);
		
		if (fIsEnum) {
			fEnumNames = reader.readStringList();
			fEnumVals  = reader.readIntList();
		} else {
			fTypeName = reader.readString();
		}
	}

	@Override
	public void dump(IDBWriter writer) {
		super.dump(writer);
		
		writer.writeInt((fIsEnum)?1:0);
		
		if (fIsEnum) {
			writer.writeStringList(fEnumNames);
			writer.writeIntList(fEnumVals);
		} else {
			writer.writeString(fTypeName);
		}
	}
	
	public List<String> getEnumNames() {
		return fEnumNames;
	}
	
	public List<Integer> getEnumVals() {
		return fEnumVals;
	}
	
	public boolean isEnumType() {
		return fIsEnum;
	}
	
	public String getTypeName() {
		return fTypeName;
	}

	@Override
	public SVDBItem duplicate() {
		SVDBTypedef ret = new SVDBTypedef(fTypeName);
		
		ret.init(this);

		return ret;
	}

	@Override
	public void init(SVDBItem other) {
		SVDBTypedef ot = (SVDBTypedef)other;
		
		fIsEnum = ot.fIsEnum;
		
		super.init(other);
		
		fTypeName = ot.fTypeName;
		
		if (fIsEnum) {
			fEnumNames = new ArrayList<String>();
			fEnumVals = new ArrayList<Integer>();
			fEnumNames.addAll(ot.fEnumNames);
			fEnumVals.addAll(ot.fEnumVals);
		} else {
			fEnumNames = null;
			fEnumVals = null;
		}
	}
	
	
	
}
