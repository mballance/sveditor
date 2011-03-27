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


package net.sf.sveditor.core.db.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.sveditor.core.db.ISVDBChildItem;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBLocation;
import net.sf.sveditor.core.db.attr.SVDBDoNotSaveAttr;
import net.sf.sveditor.core.db.attr.SVDBParentAttr;

@SuppressWarnings("rawtypes")
public class SVDBPersistenceReader implements IDBReader, IDBPersistenceTypes {
	private InputStream								fIn;
	private byte									fBuf[];
	private byte									fTmp[];
	private int										fBufIdx;
	private int										fBufSize;
	private int										fPos;
	private static Map<Class, Map<Integer, Enum>>	fEnumMap;
	private Map<SVDBItemType, Class>				fClassMap;
	private static final boolean					fDebugEn = false;
	private int										fLevel = 0;
	
	static {
		fEnumMap = new HashMap<Class, Map<Integer,Enum>>();
	}

	public SVDBPersistenceReader(InputStream in) {
		fIn      	= in;
		fBuf     	= new byte[1024*1024];
		fBufIdx  	= 0;
		fBufSize 	= 0;
		fPos		= 0;
		fClassMap 	= new HashMap<SVDBItemType, Class>();
		
		// Locate the class for each SVDBItemType element
		ClassLoader cl = getClass().getClassLoader();
		for (SVDBItemType v : SVDBItemType.values()) {
			String key = "SVDB" + v.name();
			Class cls = null;
			for (String pref : new String [] {"net.sf.sveditor.core.db.", 
											  "net.sf.sveditor.core.db.stmt.",
											  "net.sf.sveditor.core.db.expr."}) {
				try {
					cls = cl.loadClass(pref + key);
				} catch (Exception e) { }
			}
			
			if (cls == null) {
				System.out.println("Failed to locate class " + key);
			} else {
				fClassMap.put(v, cls);
			}
		}
	}
	
	public void init(InputStream in) {
		fIn = in;
		fBufIdx  = 0;
		fBufSize = 0;
	}
	
	public void readObject(ISVDBChildItem parent, Class cls, Object target) throws DBFormatException {
		if (fDebugEn) {
			debug("--> " + (++fLevel) + " readObject: " + cls.getName());
		}
		
		if (cls.getSuperclass() != null && cls.getSuperclass() != Object.class) {
			readObject(parent, cls.getSuperclass(), target);
		}
		
		Field fields[] = cls.getDeclaredFields();
		
		for (Field f : fields) {
			f.setAccessible(true);
			
			if (!Modifier.isStatic(f.getModifiers())) {
				
				if (f.getAnnotation(SVDBParentAttr.class) != null) {
					try {
						f.set(target, parent);
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
					continue;
				}
				
				if (f.getAnnotation(SVDBDoNotSaveAttr.class) != null) {
					continue;
				}
				
				try {
					Class field_class = f.getType();
					
					if (fDebugEn) {
						debug("   read field " + f.getName() + " in " + cls.getName() + ": " + field_class.getName());
					}

					if (Enum.class.isAssignableFrom(field_class)) {
						f.set(target, readEnumType(field_class));
					} else if (List.class.isAssignableFrom(field_class)) {
						Type t = f.getGenericType();
						if (t instanceof ParameterizedType) {
							ParameterizedType pt = (ParameterizedType)t;
							Type args[] = pt.getActualTypeArguments();
							if (args.length != 1) {
								throw new DBFormatException("" + args.length + "-parameter list unsupported");
							}
							Class c = (Class)args[0];
							if (fDebugEn) {
								debug("Read list field " + f.getName() + " w/item type " + c.getName());
							}
							if (c == String.class) {
								f.set(target, readStringList());
							} else if (c == Integer.class) {
								f.set(target, readIntList());
							} else if (ISVDBItemBase.class.isAssignableFrom(c)) {
								f.set(target, readItemList(parent));
							} else {
								throw new DBFormatException("Type Arg: " + ((Class)args[0]).getName());
							}
						} else {
							throw new DBFormatException("Non-parameterized list");
						}
					} else if (Map.class.isAssignableFrom(field_class)) {
						Type t = f.getGenericType();
						if (t instanceof ParameterizedType) {
							ParameterizedType pt = (ParameterizedType)t;
							Type args[] = pt.getActualTypeArguments();
							Class key_c = (Class)args[0];
							Class val_c = (Class)args[1];
							if (key_c == String.class && val_c == String.class) {
								f.set(target, readMapStringString());
							} else {
								throw new DBFormatException("Map<" + key_c.getName() + ", " + val_c.getName() + ">: Class " + cls.getName());
							}
						} else {
							throw new DBFormatException("Non-parameterized list");
						}
					} else if (field_class == String.class) {
						f.set(target, readString());
					} else if (field_class == int.class) {
						f.setInt(target, readInt());
					} else if (field_class == long.class) {
						f.setLong(target, readLong());
					} else if (field_class == boolean.class) {
						f.setBoolean(target, readBoolean());
					} else if (SVDBLocation.class == field_class) {
						f.set(target, readSVDBLocation());
					} else if (ISVDBItemBase.class.isAssignableFrom(field_class)) {
						f.set(target, readSVDBItem(parent));
					} else {
						throw new DBFormatException("Unhandled class " + field_class.getName());
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					throw new DBFormatException("Generic Load Failure: " + e.getMessage());
				}
			}
		}

		if (fDebugEn) {
			debug("<-- " + (fLevel--) + " readObject: " + cls.getName());
		}
	}

	public String readBaseLocation() throws DBFormatException {
		DBHeader hdr = new DBHeader();
		
		readObject(null, DBHeader.class, hdr);
		
		if (hdr.magic == null || !hdr.magic.equals("SDB")) {
			throw new DBFormatException("Database not prefixed with SDB (" + hdr.magic + ")");
		}
		
		return hdr.base_location;
	}
	
	@SuppressWarnings("unchecked")
	public Enum readEnumType(Class enum_type) throws DBFormatException {
		int type = readRawType();
		
		if (type == TYPE_NULL) {
			return null;
		}
		
		if (type != TYPE_ENUM) {
			throw new DBFormatException("Expecting TYPE_ENUM ; received " + type);
		}

		if (!fEnumMap.containsKey(enum_type)) {
			Enum vals[] = null;
			try {
				Method m = null;
				m = enum_type.getMethod("values");
				vals = (Enum[])m.invoke(null);
			} catch (Exception ex) {
				throw new DBFormatException("Enum class " + 
						enum_type.getName() + " does not have a values() method");
			}
			Map<Integer, Enum> em = new HashMap<Integer, Enum>();
			for (int i=0; i<vals.length; i++) {
				em.put(i, vals[i]);
			}
			
			fEnumMap.put(enum_type, em);
		}
		Map<Integer, Enum> enum_vals = fEnumMap.get(enum_type);

		int val = readInt();
		
		Enum ret = enum_vals.get(val); 
		
		if (ret == null) {
			throw new DBFormatException("Value " + val + " does not exist in Enum " + enum_type.getName());
		}
		
		return ret;
	}
	
	private Map<String, String> readMapStringString() throws DBFormatException {
		Map<String, String> ret = new HashMap<String, String>();
		int type = readRawType();
		
		if (type == TYPE_NULL) {
			return null;
		}
		
		if (type != TYPE_MAP) {
			throw new DBFormatException("Expecting TYPE_MAP ; received " + type);
		}
		
		int size = readInt();
		for (int i=0; i<size; i++) {
			String key = readString();
			String val = readString();
			ret.put(key, val);
		}
		
		return ret;
	}
	
	private int readRawType() throws DBFormatException {
		int ret = -1;
		ret = getch();
		
		if (ret < TYPE_INT_8 || ret >= TYPE_MAX) {
			throw new DBFormatException("Invalid type " + ret);
		}
		return ret;
	}
	
	public int readInt() throws DBFormatException {
		int type = readRawType();
		int ret = -1;
		if (type < TYPE_INT_8 || type > TYPE_INT_32) {
			throw new DBFormatException("Invalid int type " + type);
		}
		
		if (fTmp == null || fTmp.length < 4) {
			fTmp = new byte[4];
		}
		readRawBytes(fTmp, 0, (type-TYPE_INT_8+1));
		
		switch (type) {
			case TYPE_INT_8:
				ret = ((int)fTmp[0] & 0xFF);
				break;
			case TYPE_INT_16:
				ret = ((int)fTmp[1] & 0xFF);
				ret <<= 8;
				ret |= ((int)fTmp[0] & 0xFF);
				break;
			case TYPE_INT_24:
				ret = ((int)fTmp[2] & 0xFF);
				ret <<= 8;
				ret |= ((int)fTmp[1] & 0xFF);
				ret <<= 8;
				ret |= ((int)fTmp[0] & 0xFF);
				break;
			case TYPE_INT_32:
				ret = ((int)fTmp[3] & 0xFF);
				ret <<= 8;
				ret |= ((int)fTmp[2] & 0xFF);
				ret <<= 8;
				ret |= ((int)fTmp[1] & 0xFF);
				ret <<= 8;
				ret |= ((int)fTmp[0] & 0xFF);
				break;
		}
		
		return ret;
	}
	
	public boolean readBoolean() throws DBFormatException {
		int type = readRawType();
		
		if (type == TYPE_BOOL_TRUE) {
			return true;
		} else if (type == TYPE_BOOL_FALSE) {
			return false;
		} else {
			throw new DBFormatException("Expecting BOOL_TRUE or BOOL_FALSE ; received " + type);
		}
	}

	public List<Integer> readIntList() throws DBFormatException {
		int type = readRawType();
		
		if (type == TYPE_NULL) {
			return null;
		}
		
		if (type != TYPE_INT_LIST) {
			throw new DBFormatException("Expecting INT_LIST, receive " + type);
		}
		
		int size = readInt();
		
		List<Integer> ret = new ArrayList<Integer>();
		for (int i=0; i<size; i++) {
			ret.add(readInt());
		}
		
		return ret;
	}

	@SuppressWarnings("unchecked")
	public List readItemList(ISVDBChildItem parent) throws DBFormatException {
		int type = readRawType();
		
		if (type == TYPE_NULL) {
			return null;
		}
		
		if (type != TYPE_ITEM_LIST) {
			throw new DBFormatException("Expect TYPE_ITEM_LIST, receive " + type);
		}
		
		int size = readInt();
		
		if (fDebugEn) {
			debug("readSVDBItemList: " + size);
		}
		
		List ret = new ArrayList();
		for (int i=0; i<size; i++) {
			ret.add(readSVDBItem(parent));
		}
		
		return ret;
	}
	
	public SVDBLocation readSVDBLocation() throws DBFormatException {
		int type = readRawType();
		
		if (type == TYPE_NULL) {
			debug("    readLocation: NULL");
			return null;
		}
		
		if (type != TYPE_SVDB_LOCATION) {
			throw new DBFormatException("Expecting TYPE_SVDB_LOCATION ; received " + type);
		}


		int line = readInt();
		int pos  = readInt();

		if (fDebugEn) {
			debug("    readLocation: " + line + ":" + pos);
		}

		return new SVDBLocation(line, pos);
	}

	public SVDBItemType readItemType() throws DBFormatException {
		return (SVDBItemType)readEnumType(SVDBItemType.class);
	}

	public long readLong() throws DBFormatException {
		int type = readRawType();
		long ret = -1;
		if (type < TYPE_INT_8 || type > TYPE_INT_64) {
			throw new DBFormatException("Invalid int type " + type);
		}
		
		if (fTmp == null || fTmp.length < 4) {
			fTmp = new byte[4];
		}
		readRawBytes(fTmp, 0, (type-TYPE_INT_8+1));
		
		switch (type) {
		case TYPE_INT_8:
			ret = ((int)fTmp[0] & 0xFF);
			break;
		case TYPE_INT_16:
			ret = ((int)fTmp[1] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[0] & 0xFF);
			break;
		case TYPE_INT_24:
			ret = ((int)fTmp[2] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[1] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[0] & 0xFF);
			break;
		case TYPE_INT_32:
			ret = ((int)fTmp[3] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[2] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[1] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[0] & 0xFF);
			break;
		case TYPE_INT_40:
			ret = ((int)fTmp[4] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[3] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[2] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[1] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[0] & 0xFF);
			break;
		case TYPE_INT_48:
			ret = ((int)fTmp[5] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[4] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[3] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[2] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[1] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[0] & 0xFF);
			break;
		case TYPE_INT_56:
			ret = ((int)fTmp[6] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[5] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[4] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[3] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[2] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[1] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[0] & 0xFF);
			break;
		case TYPE_INT_64:
			ret = ((int)fTmp[7] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[6] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[5] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[4] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[3] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[2] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[1] & 0xFF);
			ret <<= 8;
			ret |= ((int)fTmp[0] & 0xFF);
			break;
		}
		
		return ret;
	}

	public String readString() throws DBFormatException {
		int type = readRawType();
		
		if (type == TYPE_NULL) {
			return null;
		}
		
		if (type != TYPE_STRING) {
			throw new DBFormatException("Expecting TYPE_STRING, received " + type);
		}
		
		int len = readInt();
		
		if (len < 0) {
			throw new DBFormatException("Received string length < 0: " + len);
		}
		if (fTmp == null || fTmp.length < len) {
			fTmp = new byte[len];
		}
		readRawBytes(fTmp, 0, len);

		String ret = new String(fTmp, 0, len);
		
		if (fDebugEn) {
			debug("readString: " + ret);
		}
		return ret;
	}

	public byte [] readByteArray() throws DBFormatException {
		int type = readRawType();
		
		if (type == TYPE_NULL) {
			return null;
		}
		
		if (type != TYPE_BYTE_ARRAY) {
			throw new DBFormatException("Expecting TYPE_BYTE_ARRAY, received " + type);
		}
		int size = readInt();
		byte ret[] = new byte[size];
		
		readRawBytes(ret, 0, size);
		
		return ret;
	}

	public List<String> readStringList() throws DBFormatException {
		int type = readRawType();
		
		if (type == TYPE_NULL) {
			return null;
		}
		
		if (type != TYPE_STRING_LIST) {
			throw new DBFormatException("Expecting TYPE_STRING_LIST, received " + type);
		}
		
		int size = readInt();
		
		List<String> ret = new ArrayList<String>();
		for (int i=0; i<size; i++) {
			ret.add(readString());
		}
		
		return ret;
	}

	public List<Long> readLongList() throws DBFormatException {
		int type = readRawType();
		
		if (type == TYPE_NULL) {
			return null;
		}
		
		if (type != TYPE_LONG_LIST) {
			throw new DBFormatException("Expecting TYPE_LONG_LIST, received " + type);
		}
		
		int size = readInt();
		
		List<Long> ret = new ArrayList<Long>();
		for (int i=0; i<size; i++) {
			ret.add(readLong());
		}
		
		return ret;
	}

	public ISVDBItemBase readSVDBItem(ISVDBChildItem parent) throws DBFormatException {
		int type = readRawType();
		
		if (type == TYPE_NULL) {
			return null;
		} else if (type != TYPE_ITEM) {
			throw new DBFormatException("Expecting TYPE_ITEM ; received " + type);
		}
		
		SVDBItemType item_type   = readItemType();
		
		if (fDebugEn) {
			debug("readSVDBItem: " + item_type);
		}
		
		ISVDBItemBase ret = null;
		
		if (fClassMap.containsKey(item_type)) {
			Class cls = fClassMap.get(item_type);
			Object obj = null;
			try {
				obj = cls.newInstance();
			} catch (Exception e) {
				throw new DBFormatException("Failed to create object: " + item_type + " " + e.getMessage());
			}

			readObject(parent, cls, obj);
			ret = (ISVDBItemBase)obj;
		} else {
			throw new DBFormatException("Unsupported SVDBItemType " + item_type);
		}
		
		return ret;
	}

	public void readType(int exp) throws DBFormatException {
		int type = getch();
		
		if (type == -1) {
			throw new DBFormatException("Unexpected EOF");
		}
		if (type >= TYPE_MAX) {
			throw new DBFormatException("Invalid primitive type " + type);
		}
		if (type != exp) {
			throw new DBFormatException("Expected type " + exp + " received " + type);
		}
	}

	public int getch() throws DBFormatException {
		int ch = -1;

		if (fBufIdx >= fBufSize) {
			try {
				fBufSize = fIn.read(fBuf, 0, fBuf.length);
				fBufIdx  = 0;
			} catch (IOException e) { }
		}
		
		if (fBufIdx < fBufSize) {
			ch = fBuf[fBufIdx++];
			fPos++;
		} else {
			throw new DBFormatException("Unexpected EOF");
		}
		
		return ch;
	}
	
	public void readRawBytes(byte data[], int offset, int length) throws DBFormatException {
		int idx=0;
		while (idx < length) {
			int remaining = (length-idx);
			
			if ((fBufSize-fBufIdx) > 0) {
				int this_len;
				if (remaining > (fBufSize-fBufIdx)) {
					this_len = (fBufSize-fBufIdx);
				} else {
					this_len = remaining;
				}
				System.arraycopy(fBuf, fBufIdx, data, idx, this_len);
				fBufIdx += this_len;
				idx += this_len;
				fPos += this_len;
			}
			
			if (fBufIdx >= fBufSize) {
				// Refill 
				try {
					fBufSize = fIn.read(fBuf, 0, fBuf.length);
					fBufIdx  = 0;
				} catch (IOException e) { }
			}
			
			if (fBufSize == 0 && (idx < length)) {
				// EOF
				throw new DBFormatException("Unexpected EOF");
			}
		}
	}
	
	private void debug(String msg) {
		if (fDebugEn) {
			System.out.println(msg);
		}
	}
}
