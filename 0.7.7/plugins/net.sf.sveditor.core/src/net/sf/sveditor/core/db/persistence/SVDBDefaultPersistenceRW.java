package net.sf.sveditor.core.db.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.sveditor.core.db.ISVDBChildItem;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBLocation;
import net.sf.sveditor.core.db.attr.SVDBDoNotSaveAttr;
import net.sf.sveditor.core.db.attr.SVDBParentAttr;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;

@SuppressWarnings({"unchecked","rawtypes"})
public class SVDBDefaultPersistenceRW 
		implements ISVDBPersistenceRWDelegate, IDBPersistenceTypes {
	private LogHandle								fLog;
	private ISVDBPersistenceRWDelegateParent		fParent;
	private boolean									fDebugEn = false;
	private int										fLevel;
	private static Map<Class, Map<Integer, Enum>>	fIntToEnumMap;
	private static Map<Class, Map<Enum, Integer>>	fEnumToIntMap;
	private static Map<SVDBItemType, Class>			fClassMap;
	
	static {
		fIntToEnumMap = new HashMap<Class, Map<Integer,Enum>>();
		fEnumToIntMap = new HashMap<Class, Map<Enum,Integer>>();
	}
	
	public SVDBDefaultPersistenceRW() {
		fLog = LogFactory.getLogHandle("SVDBDefaultPersistenceRW");
	}
	
	public Set<Class> getSupportedObjects() {
		return null;
	}

	public Set<Class> getSupportedEnumTypes() {
		return null;
	}
	
	public Set<SVDBItemType> getSupportedItemTypes() {
		return null;
	}

	public void setDebugEn(boolean en) {
		fDebugEn = en;
	}
	
	public void init(ISVDBPersistenceRWDelegateParent parent) {
		fParent = parent;
		fLevel = 0;
		
		synchronized (getClass()) {
			if (fClassMap == null) {
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
		}
	}

	public void writeObject(Class cls, Object target) throws DBWriteException {
		try {
			accessObject(true, null, cls, target);
		} catch (DBFormatException e) {}
	}

	public void readObject(ISVDBChildItem parent, Class cls, Object target) throws DBFormatException {
		try {
			accessObject(false, parent, cls, target);
		} catch (DBWriteException e) {}
	}

	protected void accessObject(
			boolean			write,
			ISVDBChildItem 	parent, 
			Class 			cls, 
			Object 			target) throws DBWriteException, DBFormatException {
		if (fDebugEn) {
			debug("--> " + (++fLevel) + " accessObject: " + cls.getName());
		}
		
		if (cls.getSuperclass() != null && cls.getSuperclass() != Object.class) {
			if (write) {
				fParent.writeObject(cls.getSuperclass(), target);
			} else {
				fParent.readObject(parent, cls.getSuperclass(), target);
			}
		}
		
		Field fields[] = cls.getDeclaredFields();
		
		for (Field f : fields) {
			f.setAccessible(true);
			
			if (!Modifier.isStatic(f.getModifiers())) {
				
				if (f.getAnnotation(SVDBParentAttr.class) != null) {
					if (!write) {
						try {
							f.set(target, parent);
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						}
					}
					continue;
				}
				
				if (f.getAnnotation(SVDBDoNotSaveAttr.class) != null) {
					continue;
				}
				
				try {
					Class field_class = f.getType();
					Object field_value = null;
					
					if (write) {
						field_value = f.get(target);
					}

					if (Enum.class.isAssignableFrom(field_class)) {
						if (fDebugEn) {
							debug("  " + fLevel + " Field " + f.getName() + " is an enum " + field_class.getName());
						}
						if (write) {
							fParent.writeEnumType(field_class, (Enum)field_value);
						} else {
							f.set(target, fParent.readEnumType(field_class));
						}
					} else if (List.class.isAssignableFrom(field_class)) {
						Type t = f.getGenericType();
						if (t instanceof ParameterizedType) {
							ParameterizedType pt = (ParameterizedType)t;
							Type args[] = pt.getActualTypeArguments();
							if (args.length != 1) {
								throw new DBFormatException("" + args.length + "-parameter list unsupported");
							}
							Class c = (Class)args[0];
							if (c == String.class) {
								if (fDebugEn) {
									debug("  " + fLevel + " Field " + f.getName() + " is List<String>");
								}
								if (write) {
									fParent.writeStringList((List<String>)field_value);
								} else {
									Object o = fParent.readStringList();
									f.set(target, o);
								}
							} else if (c == Integer.class) {
								if (fDebugEn) {
									debug("  " + fLevel + " Field " + f.getName() + " is List<Integer>");
								}
								if (write) {
									fParent.writeIntList((List<Integer>)field_value);
								} else {
									f.set(target, fParent.readIntList());
								}
							} else if (c == Long.class) {
								if (fDebugEn) {
									debug("  " + fLevel + " Field " + f.getName() + " is List<Long>");
								}
								if (write) {
									fParent.writeLongList((List<Long>)field_value);
								} else {
									f.set(target, fParent.readLongList());
								}
							} else if (ISVDBItemBase.class.isAssignableFrom(c)) {
								if (fDebugEn) {
									debug("  " + fLevel + " Field " + f.getName() + " is List<ISVDBItemBase>");
								}
								if (write) {
									fParent.writeItemList((List<ISVDBItemBase>)field_value);
								} else {
									if (target instanceof ISVDBChildItem) {
										f.set(target, fParent.readItemList((ISVDBChildItem)target));
									} else {
										f.set(target, fParent.readItemList(null));
									}
								}
							} else {
								if (fDebugEn) {
									debug("  " + fLevel + " [ERROR] Field " + f.getName() + " is List<?>");
								}
								throw new DBFormatException("Type Arg: " + ((Class)args[0]).getName());
							}
						} else {
							if (fDebugEn) {
								debug("  " + fLevel + " [ERROR] Field " + f.getName() + " is an unparameterized List");
							}
							throw new DBFormatException("Non-parameterized list");
						}
					} else if (Map.class.isAssignableFrom(field_class)) {
						Type t = f.getGenericType();
						if (t instanceof ParameterizedType) {
							ParameterizedType pt = (ParameterizedType)t;
							Type args[] = pt.getActualTypeArguments();
							Class key_c = null;
							Class val_c = null;
							
							if (args[0] instanceof Class) {
								key_c = (Class)args[0];
							} else {
								throw new DBFormatException("Failed to deconstruct type for " +
										"field " + f.getName()); 
							}
							
							if (args[1] instanceof Class) {
								val_c = (Class)args[0];
							} else if (args[1] instanceof ParameterizedType) {
								val_c = (Class)((ParameterizedType)args[1]).getRawType();
							} else {
								throw new DBFormatException("Failed to deconstruct type for " +
										"field " + f.getName()); 
							}
							if (key_c == String.class && val_c == String.class) {
								if (fDebugEn) {
									debug("  " + fLevel + " Field " + f.getName() + " is Map<String,String>");
								}
								if (write) {
									fParent.writeMapStringString((Map<String, String>)field_value);
								} else {
									f.set(target, fParent.readMapStringString());
								}
							} else if (key_c == String.class && val_c.isAssignableFrom(List.class)) {
								Class c = (Class)((ParameterizedType)args[1]).getActualTypeArguments()[0];
								if (fDebugEn) {
									debug("  " + fLevel + " Field " + f.getName() + " is Map<String,List>");
								}
								if (write) {
									fParent.writeMapStringList((Map<String,List>)field_value, c);
								} else {
									f.set(target, fParent.readMapStringList(c));
								}
							} else {
								if (fDebugEn) {
									debug("  " + fLevel + " [ERROR] Field " + f.getName() + " is an unrecognized Map<?,?>");
								}
								throw new DBFormatException("Map<" + key_c.getName() + ", " + val_c.getName() + ">: Class " + cls.getName());
							}
						} else {
							if (fDebugEn) {
								debug("  " + fLevel + " [ERROR] Field " + f.getName() + " is an unparameterized map");
							}
							throw new DBFormatException("Non-parameterized map");
						}
					} else if (field_class == String.class) {
						if (fDebugEn) {
							debug("  " + fLevel + " Field " + f.getName() + " is a String");
						}
						if (write) {
							fParent.writeString((String)field_value);
						} else {
							f.set(target, fParent.readString());
						}
					} else if (field_class == int.class) {
						if (fDebugEn) {
							debug("  " + fLevel + " Field " + f.getName() + " is an Integer");
						}
						if (write) {
							fParent.writeInt((Integer)field_value);
						} else {
							f.setInt(target, fParent.readInt());
						}
					} else if (field_class == long.class) {
						if (fDebugEn) {
							debug("  " + fLevel + " Field " + f.getName() + " is a Long");
						}
						if (write) {
							fParent.writeLong((Long)field_value);
						} else {
							f.setLong(target, fParent.readLong());
						}
					} else if (field_class == boolean.class) {
						if (fDebugEn) {
							debug("  " + fLevel + " Field " + f.getName() + " is a Boolean");
						}
						if (write) {
							fParent.writeBoolean((Boolean)field_value);
						} else {
							f.setBoolean(target, fParent.readBoolean());
						}
					} else if (SVDBLocation.class == field_class) {
						if (fDebugEn) {
							debug("  " + fLevel + " Field " + f.getName() + " is an SVDBLocation");
						}
						if (write) {
							fParent.writeSVDBLocation((SVDBLocation)field_value);
						} else {
							f.set(target, fParent.readSVDBLocation());
						}
					} else if (ISVDBItemBase.class.isAssignableFrom(field_class)) {
						if (fDebugEn) {
							debug("  " + fLevel + " Field " + f.getName() + " is an ISVDBItemBase");
						}
						if (write) {
							fParent.writeSVDBItem((ISVDBItemBase)field_value);
						} else {
							f.set(target, fParent.readSVDBItem(parent));
						}
					} else {
						if (fDebugEn) {
							debug("  " + fLevel + " [ERROR] Field " + f.getName() + " is an unknown class type " + field_class.getName());
						}
						throw new DBFormatException("Unhandled class " + field_class.getName());
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					throw new DBFormatException("Generic Load Failure: " + e.getMessage());
				}
			}
		}

		if (fDebugEn) {
			debug("<-- " + (fLevel--) + " accessObject: " + cls.getName());
		}
	}

	public void writeEnumType(Class enum_type, Enum value) throws DBWriteException {
		synchronized (fEnumToIntMap) {
			if (!fEnumToIntMap.containsKey(enum_type)) {
				Enum vals[] = null;
				try {
					Method m = null;
					m = enum_type.getMethod("values");
					vals = (Enum[])m.invoke(null);
				} catch (Exception ex) {
					throw new DBWriteException("Enum class " + 
							enum_type.getName() + " does not have a values() method");
				}
				Map<Enum, Integer> em = new HashMap<Enum, Integer>();
				for (int i=0; i<vals.length; i++) {
					em.put(vals[i], i);
				}

				fEnumToIntMap.put(enum_type, em);
			}
			Map<Enum, Integer> em = fEnumToIntMap.get(enum_type);
			fParent.writeRawType(TYPE_ENUM);
			fParent.writeInt(em.get(value));
		}
	}

	public void writeSVDBItem(ISVDBItemBase item) throws DBWriteException {
		// Okay to make this a local call, since we already
		// know that we will handle dumping the fields
		try {
			accessObject(true, null, item.getClass(), item);
		} catch (DBFormatException e) {}
	}

	public Enum readEnumType(Class enum_type) throws DBFormatException {
		Enum ret;
		int val;
		synchronized (fIntToEnumMap) {
			if (!fIntToEnumMap.containsKey(enum_type)) {
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

				fIntToEnumMap.put(enum_type, em);
			}
			Map<Integer, Enum> enum_vals = fIntToEnumMap.get(enum_type);
			val = fParent.readInt();
			ret = enum_vals.get(val); 
		}
		
		if (ret == null) {
			throw new DBFormatException("Value " + val + " does not exist in Enum " + enum_type.getName());
		}
		
		return ret;
	}

	public ISVDBItemBase readSVDBItem(SVDBItemType item_type, ISVDBChildItem parent) throws DBFormatException {
		ISVDBItemBase ret = null;
		
		if (fClassMap.containsKey(item_type)) {
			Class cls = fClassMap.get(item_type);
			Object obj = null;
			try {
				obj = cls.newInstance();
			} catch (Exception e) {
				throw new DBFormatException("Failed to create object: " + item_type + " " + e.getMessage());
			}

			try {
				accessObject(false, parent, cls, obj);
			} catch (DBWriteException e) {}
			ret = (ISVDBItemBase)obj;
		} else {
			throw new DBFormatException("Unsupported SVDBItemType " + item_type);
		}

		return ret;
	}

	private void debug(String msg) {
		if (fDebugEn) {
			fLog.debug(msg);
		}
	}

}
