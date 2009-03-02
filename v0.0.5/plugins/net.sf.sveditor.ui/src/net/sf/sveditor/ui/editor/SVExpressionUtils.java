package net.sf.sveditor.ui.editor;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBModIfcClassDecl;
import net.sf.sveditor.core.db.SVDBScopeItem;
import net.sf.sveditor.core.db.SVDBTaskFuncScope;
import net.sf.sveditor.core.db.SVDBVarDeclItem;
import net.sf.sveditor.core.db.utils.SVDBIndexSearcher;
import net.sf.sveditor.core.db.utils.SVDBSearchUtils;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

public class SVExpressionUtils {
	
	private static boolean fDebugEn = false;

	
	/**
	 * Scans backwards (from idx) in the text viewer 
	 * to extract a SystemVerilog reference expression.
	 * 
	 * It is expected that all elements of the expression
	 * will be joined with '.' or '::'
	 * 
	 * eg: a.b().c::d;
	 * 
	 * if search_forward is true, then 
	 * 
	 * @param viewer
	 * @param idx
	 * @param search_forward
	 * @return
	 */
	public static String extractPreTriggerPortion(
			IDocument				doc,
			int						idx,
			boolean					search_forward) {
		StringBuffer tmp = new StringBuffer();
		int last_nws_ch = -1;
		String end_match[] = { "nde", // end
				"nigeb", // begin
				";" };

		try {
			while (idx >= 0) {
				int ch = -1;
				// Skip whitespace
				while (idx >= 0
						&& Character.isWhitespace((ch = doc.getChar(idx)))) {
					idx--;
				}

				if (idx < 0) {
					break;
				}

				if (ch == '(' || ch == ',') {
					break;
				} else if (ch == ')') {
					if (last_nws_ch != '.' && last_nws_ch != ':'
							&& last_nws_ch != -1) {
						break;
					}

					int matchLevel = 1;

					tmp.append((char) ch);

					// Otherwise, skip matching braces
					while (matchLevel > 0 && --idx >= 0) {
						// next char
						ch = doc.getChar(idx);
						if (ch == ')') {
							matchLevel++;
						} else if (ch == '(') {
							matchLevel--;
						}
						tmp.append((char) ch);
					}
				} else {
					tmp.append((char) ch);
				}

				last_nws_ch = ch;
				idx--;

				boolean found_end = false;
				for (int i = 0; i < end_match.length; i++) {
					if (tmp.toString().endsWith(end_match[i])) {
						tmp.setLength(tmp.length() - end_match[i].length());
						found_end = true;
						break;
					}
				}

				if (found_end) {
					break;
				}
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		}

		return tmp.reverse().toString();
	}
	
	
	/**
	 * Process the pre-trigger portion of a context string
	 * 
	 * @param searcher
	 * @param context
	 * @param preTrigger
	 * @return
	 */
	public static List<SVDBItem> processPreTriggerPortion(
			SVDBIndexSearcher			searcher,
			SVDBScopeItem				context,
			String						preTrigger,
			boolean						allowNonClassLastElem) {
		List<SVDBItem> item_list = new ArrayList<SVDBItem>();
		
		int ret = processPreTriggerPortion(0, null, item_list, 
				searcher, context, preTrigger, allowNonClassLastElem);
		
		if (ret == -1) {
			return null;
		} else {
			return item_list;
		}
	}
	
	/**
	 * 
	 * The procedure here is pretty simple:
	 * - Read an identifier
	 *   - Determine whether it is a function, variable, or cast
	 *       - If a cast, lookup the type
	 *       - else if first lookup, lookup starting from current scope 
	 *           and look through hierarchy
	 *       - else lookup id in last-located type  
	 *       
	 * TODO: need to resolve parameterized types
     *
	 * @param idx
	 * @param preceeding_activator
	 * @param item_list
	 * @param searcher
	 * @param context
	 * @param preTrigger
	 * @param resolveFinalReturnType -- 
	 *    Causes the algorithm to resolve the return type of the
	 *    final function in the string
	 * @return
	 */
	private static int processPreTriggerPortion(
			int							idx,
			String						preceeding_activator,
			List<SVDBItem>				item_list,
			SVDBIndexSearcher			searcher,
			SVDBScopeItem				context,
			String						preTrigger,
			boolean						resolveFinalReturnType) {
		String id = null;
		
		debug("processPreTriggerPortion(idx=" + idx + " preceeding_activator=" + preceeding_activator);
		// Need to make some changes to track the preceeding character, not the
		// next. For example, if the preceeding character was '.', then we should
		// look for id() within typeof(ret.item)
		while (idx < preTrigger.length()) {
			int ch = preTrigger.charAt(idx);
			idx++;

			if (ch == '(') {
				// recursively expand
				idx = processPreTriggerPortion(
						idx, preceeding_activator, item_list,
						searcher, context, preTrigger, resolveFinalReturnType);
				
				// Give up because the lower-level parser did...
				if (idx == -1) {
					break;
				}
			} else if (ch == '.') {
				// use the preceeding
				preceeding_activator = ".";
			} else if (ch == ':' && 
					idx < preTrigger.length() && 
					preTrigger.charAt(idx) == ':') {
				idx++;
				preceeding_activator = "::";
			} else if (Character.isJavaIdentifierPart(ch)) {
				// Read an identifier
				StringBuffer tmp = new StringBuffer();

				tmp.append((char) ch);
				while (idx < preTrigger.length()
						&& Character.isJavaIdentifierPart(
								(ch = preTrigger.charAt(idx)))) {
					tmp.append((char) ch);
					idx++;
				}
				id = tmp.toString();
				
				debug("id=" + id + " ch after id=" + (char)ch);

				if (ch == '(' || ch == '\'') {
					String type_name = null;
					int elem_type = ch;
					
					if (ch == '(') {
						// lookup id as a function
						debug("Look for function \"" + id + "\"");

						// TODO: must use scoped lookup by whatever preceeded
						List<SVDBItem> matches = null;
						SVDBItem search_ctxt = null;
						SVDBItem func = null;

						// If we have a previous lookup match earlier in the completion
						// string, then we should use that information to lookup this 
						// portion
						
						if (item_list.size() == 0) {
							// This is the first item.
							// - Search the class/module parent of the active scope

							// Browse up the search context to reach the class scope
							// TODO: Also want to support package-scope items (?)
							matches = searcher.findByNameInScopes(id, context, true);

							debug("first-item search for \"" + id + 
									"\" returned " + matches.size() + " matches");
							
							if (matches.size() > 0) {
								func = matches.get(0);
							}
						} else {
							search_ctxt = item_list.get(item_list.size()-1);
							
							matches = searcher.findByNameInClassHierarchy(
										id, (SVDBScopeItem)search_ctxt, 
										SVDBItemType.Function);

							debug("next-item search for \"" + id + 
									"\" in \"" + search_ctxt.getName() + 
									"\" returned " + matches.size() + " matches");
							
							if (matches.size() > 0) {
								func = matches.get(0);
							}
						}
						
						if (func == null) {
							return -1;
						}
						
						item_list.add(func);
						
						// Now, compute the return type of this function
						debug("return type is: " + ((SVDBTaskFuncScope)func).getReturnType());
						type_name = ((SVDBTaskFuncScope)func).getReturnType(); 
					} else {
						// ' indicates this is a type name
						// TODO: We don't yet have the infrastructure to deal with types
						type_name = id;
						List<SVDBItem> result = searcher.findByName(id,
								SVDBItemType.Class, SVDBItemType.Struct);

					}

					// Skip '()'
					// In both the case of a function/task call or a cast, 
					// we can ignore what's in the parens
					int matchLevel = 1;

					debug("pre-skip =" + (char)preTrigger.charAt(idx));
					while (matchLevel > 0 && ++idx < preTrigger.length()) {
						ch = preTrigger.charAt(idx);
						if (ch == '(') {
							matchLevel++;
						} else if (ch == ')') {
							matchLevel--;
						}
					}
					idx++;

					/*
					if (idx < preTrigger.length()) {
						System.out.println("post-consume () - idx=" + idx + 
								" len=" + preTrigger.length() + "(" + 
								preTrigger.charAt(idx) + ")");
					} else {
						System.out.println("post-consume () - exceeded length");
					}
					 */

					// Only resolve this function return type of
					// it's not the last element in the string or
					// we want the type resolved
					if (idx < preTrigger.length() || resolveFinalReturnType) {
						List<SVDBItem> result = searcher.findByName(type_name, 
								SVDBItemType.Struct, SVDBItemType.Class);

						if (result.size() > 0) {
							item_list.add(result.get(0));

							if (result.size() > 1) {
								System.out.println("[WARN] Lookup of \"" + type_name
										+ "\" resulted in " + result.size()
										+ " matches");
							}
						} else {
							idx = -1;

							// No point in continuing. We've failed to find a
							// match
							break;
						}
					}
				} else {
					// 
					SVDBItem field = null;
					List<SVDBItem> matches = null;
					
					if (item_list.size() == 0) {
						// First element, so we need to lookup the item
						debug("Searching up scope for id=\"" + id + "\"");
						
						if (id.equals("this") || id.equals("super")) {
							SVDBModIfcClassDecl class_type = 
								SVDBSearchUtils.findClassScope(context);
							
							if (class_type != null) {
								matches = new ArrayList<SVDBItem>();
							
								if (id.equals("this")) {
									matches.add(class_type);
								} else {
									class_type = searcher.findSuperClass(class_type);
									if (class_type != null) {
										matches.add(class_type);
									}
								}
							}
						} else {
							matches = searcher.findVarsByNameInScopes(
									id, context, true);

							if (matches == null || matches.size() == 0) {
								SVDBModIfcClassDecl class_type = 
									SVDBSearchUtils.findClassScope(context);

								if (class_type != null) {
									matches = searcher.findByNameInClassHierarchy(
											id, class_type);
								}
							}
						}
					} else {
						// Lookup what follows based on the trigger
						SVDBItem search_ctxt = item_list.get(
								item_list.size()-1);

						debug("Searching type \"" + 
								search_ctxt.getName() + "\" for id=\"" + id + "\"");
						matches = searcher.findByNameInClassHierarchy(
								id, (SVDBScopeItem)search_ctxt);
					}
					
//					debug("    result is " + matches.size() + " elements");
					
					if (matches == null || matches.size() == 0) {
						return -1;
					}
					
					field = matches.get(0);
					item_list.add(field);
					
					SVDBItem type = null;
					
					if (field instanceof SVDBModIfcClassDecl) {
						type = field;
					} else {
						type = searcher.findNamedModClassIfc(((SVDBVarDeclItem)field).getTypeName());
					}
							
					
					if (type == null) {
						System.out.println("cannot find type \"" + 
								((SVDBVarDeclItem)field).getTypeName() + "\"");
						return -1;
					}
					
					// TODO: lookup type of field
					item_list.add(type);
				}
			} else {
				// TODO: This is actually an error (?)
				System.out.println("CHECK: ch=" + (char)ch + " is this an error?");
				return -1;
			}
		}

		return idx;
	}

	private static void debug(String msg) {
		if (fDebugEn) {
			System.out.println(msg);
		}
	}
}
