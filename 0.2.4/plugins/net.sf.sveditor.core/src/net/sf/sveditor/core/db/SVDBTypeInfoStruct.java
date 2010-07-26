package net.sf.sveditor.core.db;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.db.persistence.DBFormatException;
import net.sf.sveditor.core.db.persistence.IDBReader;
import net.sf.sveditor.core.db.persistence.IDBWriter;

public class SVDBTypeInfoStruct extends SVDBTypeInfo {
	private List<SVDBVarDeclItem>			fFields;
	
	public SVDBTypeInfoStruct() {
		super("<<ANONYMOUS>>", SVDBDataType.Struct);
		fFields = new ArrayList<SVDBVarDeclItem>();
	}
	
	@SuppressWarnings("unchecked")
	public SVDBTypeInfoStruct(SVDBFile file, SVDBScopeItem parent, SVDBItemType type, IDBReader reader) throws DBFormatException {
		super(SVDBDataType.Struct, file, parent, type, reader);
		fFields = (List<SVDBVarDeclItem>)reader.readItemList(file, parent);
	}
	
	public List<SVDBVarDeclItem> getFields() {
		return fFields;
	}
	
	public void addField(SVDBVarDeclItem f) {
		fFields.add(f);
	}

	@Override
	public void dump(IDBWriter writer) {
		super.dump(writer);
		writer.writeItemList(fFields);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SVDBTypeInfoStruct) {
			SVDBTypeInfoStruct o = (SVDBTypeInfoStruct)obj;
			
			if (fFields.size() == o.fFields.size()) {
				for (int i=0; i<fFields.size(); i++) {
					if (!fFields.get(i).equals(o.fFields.get(i))) {
						return false;
					}
				}
			} else {
				return false;
			}
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public SVDBItem duplicate() {
		SVDBTypeInfoStruct ret = new SVDBTypeInfoStruct();
		ret.setName(getName());
		
		for (SVDBVarDeclItem f : fFields) {
			ret.addField((SVDBVarDeclItem)f.duplicate());
		}
		
		return ret;
	}
}
