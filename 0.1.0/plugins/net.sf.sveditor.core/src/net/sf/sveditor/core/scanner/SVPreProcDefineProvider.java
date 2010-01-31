package net.sf.sveditor.core.scanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.SVDBItemPrint;
import net.sf.sveditor.core.db.SVDBMacroDef;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;
import net.sf.sveditor.core.scanutils.StringTextScanner;

public class SVPreProcDefineProvider implements IDefineProvider {
	private boolean						fDebugEn  = false;
	private boolean						fDebugUndefinedMacros = true;
	private String						fFilename;
	private int							fLineno;
	private Stack<String>				fExpandStack;
	private LogHandle					fLog;
	private IPreProcMacroProvider		fMacroProvider;


	public SVPreProcDefineProvider(IPreProcMacroProvider macro_provider) {
		fExpandStack = new Stack<String>();
		fLog = LogFactory.getDefault().getLogHandle("PreProcDefineProvider");

		fMacroProvider = macro_provider;
	}
	
	public void setMacroProvider(IPreProcMacroProvider provider) {
		fMacroProvider = provider;
	}
	
	public IPreProcMacroProvider getMacroProvider() {
		return fMacroProvider;
	}
	
	public void addDefines(Map<String, String> defs) {
		for (String key : defs.keySet()) {
			fMacroProvider.setMacro(key, defs.get(key));
		}
	}
	
	public boolean isDefined(String name, int lineno) {
		SVDBMacroDef m = fMacroProvider.findMacro(name, lineno);
		
		return (m != null && m.getLocation().getLine() <= lineno);
	}

	public synchronized String expandMacro(
			String 			str, 
			String 			filename, 
			int 			lineno) {
		StringTextScanner scanner = new StringTextScanner(
				new StringBuilder(str));
		
		fFilename = filename;
		fLineno   = lineno;
		

		debug("--> expandMacro(str): " + str);
		
		if (fMacroProvider == null) {
			fLog.error("No macro provider specified");
			debug("<-- expandMacro(str): " + str);
			debug("    Result: \"\"");
			return "";
		}

		fMacroProvider.setMacro("__FILE__", fFilename);
		fMacroProvider.setMacro("__LINE__", "" + fLineno);
		fExpandStack.clear();

		expandMacro(scanner);
		
		if (fDebugEn) {
			debug("<-- expandMacro(str): " + str);
			debug("    Result: " + scanner.getStorage().toString());
		}
		
		return scanner.getStorage().toString();
	}

	/**
	 * Expand the text in 'scanner', inserting the new text into
	 * the scanner storage buffer
	 * 
	 * @param scanner
	 * @return
	 */
	private int expandMacro(StringTextScanner scanner) {
		debug("--> expandMacro(scanner)");
		int macro_start = scanner.getOffset();

		int ch = scanner.get_ch();
		
		// Expect this to be '`'
		if (ch != '`') {
			System.out.println("[ERROR] Expect macro to start " +
					"with '`', not \"" + (char)ch + "\" @ " +
					fFilename + ":" + fLineno);
			try {
				throw new Exception();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Read the macro name
		String key = scanner.readPreProcIdentifier(scanner.get_ch());
		
		if (key == null) {
			ch = scanner.get_ch();
			fLog.error("Failed to read macro name starting with " + (char)ch);
			scanner.unget_ch(ch);
		}
		
		fExpandStack.push(key);
		
		ch = scanner.skipWhite(scanner.get_ch());
		
		SVDBMacroDef m = fMacroProvider.findMacro(key, fLineno);
		
		if (m == null) {
			if (fDebugUndefinedMacros) {
				fLog.error("macro \"" + key + "\" undefined @ " +
						fFilename + ":" + fLineno);
			}
			
			/*
			if (fDebugUndefinedMacros) {
				fLog.debug("macro \"" + key + "\" undefined @ " +
						fFilename + ":" + fLineno);
				walkStack();
				boolean tmp = fDebugEnS;
				fDebugEnS = true;
				fIndent = 0;
				if (fContext != null) {
					searchContext(fContext, key);
				}
				fDebugEnS = tmp;
			}
			 */

			if (ch == '(') {
				ch = scanner.skipPastMatch("()");
				scanner.unget_ch(ch);
			}
			
			// TODO: ?
			return 0;
		}
		
		List<String> params = null;
		
		// If the macro has parameters, scan them now
		if (ch == '(') {
			// Before parsing the parameters, make all macro substitutions
			StringTextScanner scanner_s;

			if (fDebugEn) {
				debug("pre-expand: str=" + scanner.getStorage());
			}
			expandMacroRefs(new StringTextScanner(scanner, scanner.getOffset()));
			if (fDebugEn) {
				debug("post-expand: str=" + scanner.getStorage());
			}
			
			params = parse_params(m, scanner);
			
			scanner_s = new StringTextScanner(scanner, 
					macro_start, scanner.getOffset());
			expandMacro(scanner_s, m, params);
			
			if (fDebugEn) {
				debug("Expansion of " + m.getName() + " is: " +
						scanner.getStorage().toString());
			}
			
			ch = scanner.get_ch();

		} else {
			// Simple -- just replace the text range with the macro text
			StringTextScanner scanner_s = new StringTextScanner(
					scanner, macro_start, scanner.getOffset());
			
			expandMacro(scanner_s, m, null);
			
			if (fDebugEn) {
				debug("Expansion Result: " + scanner_s.getStorage().toString());
			}
			/*
			scanner.replace(macro_start, scanner.getOffset(), 
					scanner_s.getStorage().toString());
			 */
		}
		
		fExpandStack.pop();
		debug("<-- expandMacro(scanner)");
		return 0;
	}

	/****************************************************************
	 * expandMacro()
	 *
	 * Pre-conditions:  scanner from getOffset to getLimit is the
	 *                  macro to expand
	 *                  
	 *                  At this point, no macro parameters have been
	 *                  expanded. Macro calls within the macro definition
	 *                  body are also still unexpanded.
	 *                 
	 * Post-conditions: 
	 ****************************************************************/
	private void expandMacro(
			StringTextScanner		scanner,
			SVDBMacroDef 			m, 
			List<String> 			params) {
		boolean expand_params = (params != null);
		debug("--> expandMacro(" + m.getName() + ")");
		if (fDebugEn) {
			debug("    text=" + scanner.substring(scanner.getOffset(), scanner.getLimit()));
		}
		
		if (expand_params) {
			expand_params = (params.size() == m.getParameters().size());
			if (params.size() != m.getParameters().size()) {
				System.out.println("[ERROR] param count for macro \"" + 
						m.getName() + "\" doesn't match: " + 
						params.size() + " vs " + m.getParameters().size());
				System.out.println("    Location: " + fFilename + ":" + fLineno);
				try {
					throw new Exception();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		if (m.getDef() == null) {
			System.out.println("[ERROR] macro definition for key \"" + 
					m.getName() + "\" is null");
			SVDBItem top = m;
			
			while (top.getParent() != null) {
				top = top.getParent();
			}
			System.out.println("Dumping null-macro provider");
			SVDBItemPrint.printItem(top);
			walkStack();
		}
		
		debug("def=" + m.getDef());
		
		if (m.getDef() == null) {
			System.out.println("Macro \"" + m.getName() + "\" @ " + 
					m.getLocation().getFile().getFilePath() + ":" +
					m.getLocation().getLine() + " is null");
			// Replace the text with ""
			scanner.replace(scanner.getOffset(), scanner.getLimit(), "");
		} else {
			// Replace the text and adjust the limits
			scanner.replace(scanner.getOffset(), scanner.getLimit(), m.getDef());
		}

		// Expand the parameter references within the replacement
		if (expand_params) {
			expandParameterRefs(new StringTextScanner(scanner), 
					m.getParameters(), params);
		}

		// Expand pre-processor references within the replacement
		expandMacroRefs(new StringTextScanner(scanner));
		
		if (fDebugEn) {
			debug("    text=" + scanner.getStorage().toString());
		}
		debug("<-- expandMacro(" + m.getName() + ")");
	}
	
	private List<String> parse_params(
			SVDBMacroDef			m,
			StringTextScanner 		scanner) {
		if (fDebugEn) {
			debug("--> parse_params(" + m.getName() + ")");
			debug("    str=" + scanner.getStorage().substring(scanner.getOffset()));
		}
		List<String> params = new ArrayList<String>();
		int ch = scanner.get_ch();
		
		for (int i=0; i<m.getParameters().size(); i++) {
			ch = scanner.skipWhite(ch);
			int p_start = scanner.getOffset()-1;
			
			if (ch == -1) {
				break;
			}
			
			scanner.unget_ch(ch);
			
			// Skip an argument, including () 
			do {
				ch = scanner.get_ch();

				if (ch == '(') {
					ch = scanner.skipPastMatch("()");
					
					debug("    post-skip (): ch=" + (char)ch);
				}
			} while (ch != -1 && ch != ',' && ch != ')');
			
			int p_end = scanner.getOffset();
			
			String param;
			
			if (scanner.getStorage().charAt(p_start) == '`') {
				// Need to sub-expand this parameter
				StringTextScanner scanner_s = new StringTextScanner(
						new StringBuilder(scanner.substring(p_start, p_end-1)));
				if (fDebugEn) {
					debug("String passed to expandMacro(1): \"" + 
							scanner.substring(p_start, p_end-1) + "\"");
					/*
					debug("First char=" + (char)scanner_s.get_ch());
					debug("Second char=" + (char)scanner_s.get_ch());
					 */
				}
				
				if (Character.isJavaIdentifierStart(scanner.getStorage().charAt(p_start+1))) {
					expandMacro(scanner_s);
				} else {
					// TODO: probably a string
					while ((ch = scanner_s.get_ch()) != -1) {
						if (ch == '`') {
							int ch2 = scanner_s.get_ch();
							
							if (ch2 == '`') {
								scanner_s.delete(scanner_s.getOffset()-2, scanner_s.getOffset());
							} else {
								scanner_s.delete(scanner_s.getOffset()-2, scanner_s.getOffset()-1);
							}
						}
					}
				}

				param = scanner_s.getStorage().toString();
				
				if (fDebugEn) {
					debug("Parameter (with params): " + param);
				}
			} else {
				param = scanner.getStorage().substring(p_start, p_end-1);
				
				if (fDebugEn) {
					debug("Parameter (no params): " + param);
				}
			}
			
			params.add(param);
			
			if (ch == ',') {
				ch = scanner.get_ch();
			}
		}
		
		if (ch != -1) {
			ch = scanner.skipWhite(ch);
		}
		
		if (fDebugEn) {
			for (String s : params) {
				debug("Param: \"" + s + "\"");
			}
		}
		
		debug("<-- parse_params(" + m.getName() + ") => " + params.size());
		return params;
	}

	/**
	 * Expand parameter references by name in the identified 
	 * text block
	 * 
	 * @param scanner
	 * @param param_names
	 * @param param_vals
	 */
	private void expandParameterRefs(
			StringTextScanner			scanner,
			List<String>				param_names,
			List<String>				param_vals) {
		int ch;

		if (fDebugEn) {
			for (int i=0; i<param_names.size(); i++) {
				System.out.println("Param[" + i + "] " + param_names.get(i) + " = " +
						param_vals.get(i));
			}
			
			debug("--> expandParameterRefs");
			debug("pre=" + scanner.getStorage());
		}

		// Read individual identifiers. Ignore un-escaped strings
		int last_ch = -1;
		while ((ch = scanner.get_ch()) != -1) {
			if (ch == '"' && last_ch != '`') {
				// un-escaped string
				while ((ch = scanner.get_ch()) != -1 && ch != '"') { }
			} else if (Character.isJavaIdentifierStart(ch)) {
				int p_start = scanner.getOffset()-1;
				
				String key = scanner.readPreProcIdentifier(ch);

				int index = param_names.indexOf(key);
				if (index != -1 && index < param_vals.size()) {
					debug("Replacing parameter \"" + key + "\" with \"" +
							param_vals.get(index) + "\"");
					scanner.replace(p_start, scanner.getOffset()-1, 
							param_vals.get(index));
				}
			}
			
			last_ch = ch;
		}

		if (fDebugEn) {
			debug("<-- expandParameterRefs");
			debug("post=" + scanner.getStorage());
		}
	}
	
	private void expandMacroRefs(StringTextScanner scanner) {
		int ch;
		
		if (fDebugEn) {
			debug("--> expandMacroRefs");
			debug("pre=" + scanner.getStorage().substring(scanner.getOffset(), scanner.getLimit()));
		}
		
		while ((ch = scanner.get_ch()) != -1) {
			if (fDebugEn) {
				debug("ch=\"" + (char)ch + "\"");
			}
			if (ch == '`') {
				int ch2 = scanner.get_ch();
				if (fDebugEn) {
					debug("    ch2=\"" + (char)ch2 + "\"");
				}
				if (ch2 == '`') {
					// Nothing -- `` is the same as ## in C++ pre-proc
					scanner.delete(scanner.getOffset()-2, scanner.getOffset());
					debug("        delete ``");
				} else if (ch2 == '"' || ch2 == '\\') {
					scanner.delete(scanner.getOffset()-2, scanner.getOffset()-1);
				} else {
					
					// Expect an identifier
					int m_start = scanner.getOffset()-2;
					
					ch = scanner.skipWhite(ch2);
					String key = scanner.readPreProcIdentifier(ch);
					
					if (key == null) {
						System.out.println("Failed to read macro name starting with " +	(char)ch);
					}

					SVDBMacroDef sub_m = fMacroProvider.findMacro(key, fLineno);
					List<String> sub_p = null;
					ch = scanner.get_ch();
					
					debug("    ref=\"" + key + "\"");
					
					int m_end = scanner.getOffset();
					
					if (hasParameters(key)) {
						// TODO: Need to expand parameter references in this macro call
						
						debug("    \"" + key + "\" has parameters");
						ch = scanner.skipWhite(ch);
						
						if (ch == '(') {
							// expand macros in parameter list
							debug("    calling expandMacroRefs");
							expandMacroRefs(new StringTextScanner(scanner, scanner.getOffset()));
							sub_p = parse_params(sub_m, scanner);
//							scanner.get_ch();
						}
						m_end = scanner.getOffset();
					} else {
						debug("    \"" + key + "\" doesn't have parameters");
					}
					
					// Now, expand this macro
					if (ch != -1) {
						// Back up if we didn't end by exhausing the buffer. 
						// We don't want to replace the character following the macro
						scanner.unget_ch(ch);
						m_end--;
					} 
					
					debug("    text for expansion ends with " + 
							scanner.getStorage().charAt(m_end-1));
					StringTextScanner scanner_s = new StringTextScanner(
							scanner, m_start, m_end);
					
					if (sub_m != null) {
						expandMacro(scanner_s, sub_m, sub_p);
					} else {
						fLog.debug("Macro \"" + key + 
								"\" undefined @ " + fFilename + ":" + fLineno);
						scanner.delete(m_start, m_end-1);
						walkStack();
					}
				}
			}
		}
		
		if (fDebugEn) {
			debug("post=" + scanner.getStorage());
			debug("<-- expandMacroRefs");
		}
	}

	public boolean hasParameters(String key) {
		
		SVDBMacroDef m = fMacroProvider.findMacro(key, -1);
		
		if (m != null) {
			return (m.getParameters().size() != 0);
		} else {
			return false;
		}
	}

	private void walkStack() {
		String key;
		Stack<String> tmp = new Stack<String>();
		tmp.addAll(fExpandStack);
		
		fLog.debug("walkStack");
		while (tmp.size() > 0) {
			key = tmp.pop();
			fLog.debug("    " + key);
		}
	}
	
	private void debug(String str) {
		if (fDebugEn) {
			fLog.debug(str);
		}
	}
	

}
