/****************************************************************************
 * Copyright (c) 2008-2014 Matthew Ballance and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Ballance - initial implementation
 ****************************************************************************/

package net.sf.sveditor.core.indent;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import net.sf.sveditor.core.Tuple;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;

/**
 * 
 * @author ballance
 *
 * Implementation pattern:
 * - <start_of_scope> should be called before the first token of a new scope
 *   - provisionally sets the new indent level
 *   - Any comments will use this indent level prior to the first real token
 *     in the scope
 * - <enter_scope> should be called on the first token of the new scope.
 *   - In adaptive indent mode, synchronizes the provisional indent level
 *     with the actual indent level
 * - <leave_scope> should be called to exit a scope. It typically should
 *     be called with the last token of the scope, provided that can be determined
 */
public class SVDefaultIndenter2 implements ISVIndenter {
	private ISVIndentScanner fScanner;
	private Stack<Tuple<String, Boolean>> fIndentStack;
	private List<SVIndentToken> fTokenList;
	private SVIndentToken fCurrent;
	private String fCurrentIndent = "";
	private LogHandle fLog;
	private int fQualifiers;
	private static final boolean fDebugEn = false;
	private int fNLeftParen, fNRightParen;
	private String fIndentIncr = "\t";
	private Pattern fTabReplacePattern;

	private int fAdaptiveIndentEnd;
	private boolean fTestMode;

	private boolean							pref_IndentIfdef = true;		// TODO: Add this to the preferences


	static private Map<String, Integer> fQualifierMap;
	static private Set<String> fPreProcDirectives;

	private class IndentEOFException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	static {
		fQualifierMap = new HashMap<String, Integer>();
		fQualifierMap.put("static", 1 << 0);
		fQualifierMap.put("virtual", 1 << 1);
		fQualifierMap.put("local", 1 << 2);
		fQualifierMap.put("protected", 1 << 3);
		fQualifierMap.put("public", 1 << 4);
		fQualifierMap.put("extern", 1 << 5);
		fQualifierMap.put("default", 1 << 6);
		fQualifierMap.put("export", 1 << 7);
		fQualifierMap.put("import", 1 << 8);

		fPreProcDirectives = new HashSet<String>();
		fPreProcDirectives.add("`define");
		fPreProcDirectives.add("`undef");
		fPreProcDirectives.add("`ifdef");
		fPreProcDirectives.add("`else");
		fPreProcDirectives.add("`elsif");
		fPreProcDirectives.add("`ifndef");
		fPreProcDirectives.add("`endif");
		fPreProcDirectives.add("`include");
		fPreProcDirectives.add("`timescale");
		fPreProcDirectives.add("`begin_keywords");
		fPreProcDirectives.add("`end_keywords");
	}

	public SVDefaultIndenter2() {
		fIndentStack = new Stack<Tuple<String, Boolean>>();
		fTokenList = new ArrayList<SVIndentToken>();
		fLog = LogFactory.getLogHandle("SVDefaultIndenter");
	}

	public void setAdaptiveIndent(boolean adaptive) {
		// fAdaptiveIndent = adaptive;
	}

	public void setIndentIncr(String incr) {
		fIndentIncr = incr;
		if (fIndentIncr.charAt(0) != '\t') {
			// Want to replace any existing tabs with
			// the indent increment
			fTabReplacePattern = Pattern.compile("\t");
		} else {
			fTabReplacePattern = null;
		}
	}

	public void setAdaptiveIndentEnd(int lineno) {
		fAdaptiveIndentEnd = lineno;
	}

	public void setTestMode(boolean tm) {
		fTestMode = tm;
	}

	public void init(ISVIndentScanner scanner) {
		fScanner = scanner;
		push_indent_stack("", true);
	}

	public String indent() {
		String thing = indent(-1, -1);
		return (thing);
	}

	public String indent(int start_line, int end_line) {
		StringBuilder sb = new StringBuilder();
		SVIndentToken tok;

		fNLeftParen = fNRightParen = 1;
		
		fTokenList.clear();

		while ((tok = next()) != null) {

			// Scan forward until the end of the token list or until we find
			// a keyword
			try {
				do {
					if (tok.getType() == SVIndentTokenType.Identifier &&
							fQualifierMap.containsKey(tok.getImage())) {
						fQualifiers |= fQualifierMap.get(tok.getImage());
						tok = next();
					} else if (tok.isId("class") || tok.isId("module") ||
							tok.isId("interface") || tok.isId("program") ||
							tok.isId("package") || tok.isId("sequence") ||
							tok.isId("property")|| tok.isId("clocking")) {
						tok = indent_ifc_module_class(tok.getImage());
						fQualifiers = 0;
					} else if (tok.isId("config")) {
						tok = indent_config(tok.getImage());
						fQualifiers = 0;
					} else if (tok.isId("covergroup")) {
						tok = indent_covergroup();
					} else if (tok.isId("function") || tok.isId("task")) {
						tok = indent_task_function(tok.getImage());
						fQualifiers = 0;
					} else if (tok.isId("typedef")) {
						tok = indent_typedef();
						fQualifiers = 0;
					} else if (tok.isOp(";")) {
						fQualifiers = 0;
						tok = next();
					} else {
						tok = next();
					}
				} while ((tok = current()) != null);
			} catch (IndentEOFException e) {
				// e.printStackTrace();
				break;
			} catch (RuntimeException e) {
				if (fTestMode) {
					throw e;
				}
			}
		}

		if (fTestMode) {
			if (fIndentStack.size() != 1) {
				debug("IndentStack size is " + 
						fIndentStack.size() + " rather than 1");
				throw new RuntimeException("IndentStack size is " + 
						fIndentStack.size() + " rather than 1");
			}
		}

		if (fDebugEn) {
			debug("Indent operation complete");
		}
		for (SVIndentToken t : fTokenList) {
			int token_start = t.getLineno();
			int token_end = token_start;
			// Extract Multi-Line comments if needs be
			if (t instanceof SVMultiLineIndentToken)  {
				SVMultiLineIndentToken cmt = (SVMultiLineIndentToken) t;
				token_end   = t.getLineno() + ((SVMultiLineIndentToken)t).getCommentLines().size();
			}

			// Single lines, and multi-lines that are entirely consumed by the start and end get handled here 
			if ((token_start >= start_line || start_line == -1) &&
					(token_end <= end_line || end_line == -1)) {
				if (fDebugEn) {
					debug("tok \"" + t.getType() + "\" line=" + t.getLineno() + " image=" + t.getImage());
				}
				// String leading_ws = t.getLeadingWS();
				String leading_ws = t.getLeadingNonNewLineWS();
				// Replace any tabs in the leading whitespace
				// if we're inserting spaces instead of tabs
				if (t.isStartLine() && fTabReplacePattern != null) {
					leading_ws = fTabReplacePattern.matcher(leading_ws).replaceAll(fIndentIncr);
				}
				sb.append(leading_ws + 
						t.getImage() +
						t.getTrailingWS() +
						((t.isEndLine())?"\n":""));
			}
			// The requested lines request a portion of the multi-line token
			else  {
				if (fDebugEn) {
					debug("tok \"" + t.getType() + "\" line=" + t.getLineno() + " image=" + t.getImage());
				}
				// String leading_ws = t.getLeadingWS();
				String leading_ws = t.getLeadingNonNewLineWS();
				// Replace any tabs in the leading whitespace
				// if we're inserting spaces instead of tabs
				if (t.isStartLine() && fTabReplacePattern != null) {
					leading_ws = fTabReplacePattern.matcher(leading_ws).replaceAll(fIndentIncr);
				}
				String lines[] = (leading_ws + 
						t.getImage() +
						t.getTrailingWS() +
						((t.isEndLine())?"\n":"")).split("[\r\n]+");
				
				for (int i=token_start; i<token_end; i++)  {
					if ((i >= start_line) && (i <= end_line))  {
						sb.append(lines[i-token_start] + "\n");
					}
				}
			}
		}

		return sb.toString();
	}

	public String getLineIndent(int lineno) {
		String ret = null;

		for (SVIndentToken t : fTokenList) {
			if (t.getLineno() == lineno) {
				ret = t.getLeadingNonNewLineWS();

				// Replace any tabs in the leading whitespace
				// if we're inserting spaces instead of tabs
				if (t.isStartLine() && fTabReplacePattern != null) {
					ret = fTabReplacePattern.matcher(ret).replaceAll(fIndentIncr);
				}
				break;
			}
		}

		return ret;
	}

	public boolean isQualifierSet(String key) {
		return ((fQualifierMap.get(key) & fQualifiers) != 0);
	}

	private SVIndentToken indent_if(boolean is_else_if) {
		return (indent_if(is_else_if, false));
	}

	/**
	 * 
	 * @param is_else_if
	 * @param dont_indent_first
	 *            - Won't indent the if. Typically set when already indented by
	 *            a "unique" or "priority" keyword
	 * @return
	 */
	private SVIndentToken indent_if(boolean is_else_if,
			boolean dont_indent_first) {
		SVIndentToken tok = current();
		if (fDebugEn) {
			debug("--> indent_if() tok=" + tok.getImage());
		}
		
		if (!dont_indent_first)
			start_of_scope(tok);

		tok = next_s();

		if (tok.isOp("(")) {
			tok = consume_expression();
		} else {
			// System.out.println("[ERROR] unsure what happened - tok=" +
			// tok.getImage());
			// bail out -- not sure what happened...
			return tok;
		}

		// An expect statement could have form:
		//   expect (property) else pass_case();
		// which would not have expect if statements 
		if (tok.isId("else"))  {
			// Need to un-indent on the else
			leave_scope(tok);
		}
		else  {
			tok = indent_if_stmts(null);
		}

		if (tok.isId("else")) {
			// Insert a dummy scope to prevent the token following the 'else'
			// from re-setting the adaptive indent
			start_of_scope(tok, true);
			
			if (fDebugEn) {
				debug("--> else next_s");
			}
			tok = next_s();
			if (fDebugEn) {
				debug("<-- else next_s");
			}
			if (tok.isId("if")) {
				leave_scope(tok);
//				start_of_scope(tok, true);
				tok = indent_if(true);
			} else {
				tok = indent_if_stmts(null);
			}
		}

		if (fDebugEn) {
			debug("<-- indent_if() tok=" + 
				((tok != null)?tok.getImage():"null"));
		}

		return tok;
	}

	private SVIndentToken indent_assert(boolean is_else_if) {
		SVIndentToken tok = current();

		if (fDebugEn) {
			debug("--> indent_if() tok=" + tok.getImage());
		}
		start_of_scope(tok);

		tok = next_s();

		// Just swallow the property keyword
		if (tok.isId("property")) {
			tok = next_s();
		}
		if (tok.isOp("(")) {
			tok = consume_expression();
			if (tok.isOp(";")) {
				leave_scope(tok);
				end_of_scope(tok);
				tok = next_s();
				return tok;
			}
		} else {
			// System.out.println("[ERROR] unsure what happened - tok=" +
			// tok.getImage());
			// bail out -- not sure what happened...
			return tok;
		}

		if (tok.isId("begin")) {
			tok = indent_if_stmts(null);
		}
		else if (tok.isId("else")) {
			leave_scope(tok);		// un-indent, we already indented before we came here, assuming it was a single-line bit of code
			// start_of_scope(tok);
		}
		else  {
			tok = indent_if_stmts(null);
		}
		if (tok.isId("else")) {
			// Insert a dummy scope to prevent the token following the 'else'
			// from re-setting the adaptive indent
			start_of_scope(tok, false);
			if (fDebugEn) {
				debug("--> else next_s");
			}
			tok = next_s();
			if (fDebugEn) {
				debug("<-- else next_s");
			}
			leave_scope();
			if (tok.isId("if")) {
				tok = indent_if(true);
			} else {
				start_of_scope(tok);
				tok = indent_if_stmts(null);
			}
		}

		if (fDebugEn) {
			debug("<-- indent_if() tok=" + 
					((tok != null)?tok.getImage():"null"));
		}

		return tok;
	}

	private SVIndentToken indent_if_stmts(String parent) {
		SVIndentToken tok = current_s();

		// Indent the statement or block inside the 'if' clause.
		if (tok.isId("begin")) {
			parent = "begin";
			boolean begin_is_start_line = tok.isStartLine();

			if (begin_is_start_line) {
				leave_scope(tok);		// un-indent, we already indented before we came here, assuming it was a single-line bit of code
				start_of_scope(tok);
			}
			// begin often has : <label> consume this if it is there
			tok = consume_labeled_block(next_s());
			enter_scope(tok);

			while (tok != null) {
				if (fDebugEn) {
					debug("top of begin/end loop: " + tok.getType() + " " + tok.getImage());
				}
				if (tok.isId("end")) {
					leave_scope(tok);
					if (fDebugEn) {
						debug("Setting indent \"" + peek_indent() + "\"");
					}
					tok = consume_labeled_block(next_s());
					break;
				} else {
					// Indent statement. Note that we're already in the
					// scope that the statement should be in
					tok = indent_block_or_statement(parent, true);
				}
			}
		} else {
			// No begin
			enter_scope(tok);
			// tok = indent_stmt(parent, true);
			tok = indent_stmt(parent, false);
			leave_scope(tok);

			// Fixup the indent on any comments that came before 'tok'
			fixupPreviousCommentIndent(tok);
		}

		return tok;
	}

	private SVIndentToken indent_fork() {
		SVIndentToken tok = current();

		start_of_scope(tok);
		tok = consume_labeled_block(next_s());	// Forks can be named fork : some_name
		enter_scope(tok);

		while (tok != null && 
				!tok.isId("join") && 
				!tok.isId("join_none") && 
				!tok.isId("join_any")) {

			tok = indent_block_or_statement("fork", true);
		}
		leave_scope(tok);

		tok = next_s();

		return tok;
	}

	/**
	 * Function: indent_delay
	 * 
	 * Parses #delay statements
	 * 
	 * Looks for:
	 * #[number]<time_unit ::= s | ms | us | ns | ps | fs | step> <;>
	 * @return
	 */
	private SVIndentToken indent_delay(String parent, boolean parent_is_block) {
		SVIndentToken tok = current();

		start_of_scope(tok);
		tok = next_s(); // Consume the #
		tok = next_s(); // Consume the number
		if (tok.isId("s") || tok.isId("ms") || tok.isId("us") || tok.isId("ns") || tok.isId("ps") || tok.isId("fs") || tok.isId("step"))  {
			tok = next_s(); // Consume the time_unit
		}
		enter_scope(tok);
		if (!tok.isId(";")) {
			tok = indent_stmt(parent, true);
		}
		leave_scope(tok); // restore scope previously set
		end_of_scope(tok);
		return tok;
	}

	private SVIndentToken indent_loop_stmt() {
		SVIndentToken tok, first;

		tok = first = current();

		start_of_scope(tok);

		if (fDebugEn) {
			debug("--> indent_loop_stmt() tok=" + tok.getImage());
		}

		if (!tok.isId("do") && !tok.isId("forever")) {
			tok = next_s();
			if (tok.isOp("(")) {
				tok = consume_expression();
			} else {
				return tok;
			}
		} else if (tok.isId("forever")) {
			// A forever COULD have a #(delay) after it... swallow it if exists
			// A forever COULD have a @(posedge XXX) after it... swallow it if exists - # (487)
			tok = next_s();
			if (tok.isOp("#") ||tok.isOp("@")) {
				tok = next_s();
				tok = consume_expression();
			}
		} else {
			tok = next_s();
		}

		// tok = indent_block_or_statement(null, true);
		tok = indent_if_stmts(null);

		if (first.isId("do")) {
			// Just read to end of statement
			while (!tok.isOp(";")) {
				tok = next_s();
			}
			tok = next_s();
		}

		if (fDebugEn) {
			debug("<-- indent_loop_stmt() tok=" + 
				((tok != null)?tok.getImage():"null"));
		}

		return tok;
	}

	private SVIndentToken indent_typedef() {
		SVIndentToken tok = current();
		boolean enum_struct = false;

		start_of_scope(tok);

		if (fDebugEn) {
			debug("--> indent_typedef()");
		}

		tok = next_s();

		if (tok.isId("enum") || tok.isId("struct") || tok.isId("union")) {
			tok = indent_struct_union_enum("typedef");
			enum_struct = true;
		}

		// read to the end of the statement
		while (!tok.isOp(";")) {
			tok = next_s();
		}
		if (!enum_struct) {
			// We've already left the scope (adjusting the closing brace)
			// in the case of an enum or struct
			leave_scope(tok);
		}
		tok = next_s();

		if (fDebugEn) {
			debug("<-- indent_typedef()");
		}

		return tok;
	}

	private SVIndentToken indent_struct_union_enum(String parent) {
		SVIndentToken tok = next_s(); // advance beyond struct/union

		if (!parent.equals("typedef")) {
			start_of_scope(tok);
		}

		while (!tok.isOp("{", ";")) {
			tok = next_s();
		}

		if (tok.isOp("{")) {
			// Not a forward declaration
			tok = next_s();
			if (!tok.isOp("}")) {
				enter_scope(tok);
			}

			while (!tok.isOp("}")) {
				tok = next_s();
			}
		}
		// Leave on the closing brace
		// if (!parent.equals("typedef")) {
		leave_scope(tok);
		// }

		// tok = next_s();

		return tok;
	}

	/**
	 * indent_ifc_module_class()
	 * 
	 * Indents a class, module or interface and the items
	 * within the body
	 * 
	 * @param item
	 * @return
	 */
	private SVIndentToken indent_ifc_module_class(String item) {
		SVIndentToken tok = current_s();

		String end = get_end_kw(item);
		if (fDebugEn) {
			debug("--> indent_ifc_module_class(" + item + ")");
		}

		start_of_scope(tok); // establish the indent of this scope
		// for adaptive-indent purposes

		tok = next_s();
		
		// interface class ends with endclass
		if (tok.getImage().equals("class"))  {
			end = get_end_kw(tok.getImage());
			tok = next_s();
		}
		// push a new scope for ports and parameters
		start_of_scope(tok);


		// Reach the end of the declaration
		while (!tok.isOp(";")) {
			tok = next_s();
		}
		leave_scope(tok);

		tok = next_s();
		enter_scope(tok); // enter the body of the module

		fQualifiers = 0;

		// Now, read body items
		while (tok != null) {

			if (tok.isId(end)) {
				break;
			} else if (tok.isId("virtual")) {
				tok = next_s();

				// Swallow virtual interface
				if (tok.isId("interface")) {
					tok = next_s();
				}
			} else if (tok.getType() == SVIndentTokenType.Identifier &&
					fQualifierMap.containsKey(tok.getImage())) {
				fQualifiers |= fQualifierMap.get(tok.getImage());
				tok = next_s();
			} else if (tok.isId("function") || tok.isId("task")) {
				tok = indent_task_function(tok.getImage());
				fQualifiers = 0;
			} else if (tok.isId("class") || tok.isId("module") || tok.isId("interface") ||
					tok.isId("property") || tok.isId("sequence")|| tok.isId("clocking")) {
				tok = indent_ifc_module_class(tok.getImage());
				fQualifiers = 0;
			} else if (tok.isId("struct") || tok.isId("union") || tok.isId("enum")) {
				tok = indent_struct_union_enum("");
				fQualifiers = 0;
			} else if (is_always(tok)) {
				tok = indent_always();
			} else if (tok.isId("initial") || tok.isId("final")) {
				tok = next_s();
				tok = indent_block_or_statement(null, false);
				fQualifiers = 0;
			} else if (tok.isId("specify")) {
				tok = indent_specify();
			} else if (tok.isId("generate")) {
				tok = next_s();
				tok = indent_generate();
			} else if (tok.isId("covergroup")) {
				tok = indent_covergroup();
				fQualifiers = 0;
			} else if (tok.isId("constraint")) {
				tok = indent_constraint();
				fQualifiers = 0;
			} else if (tok.isPreProc() && tok.isStartLine()) {
				// Just read to the end of the line, since it's
				// unlikely this statement will end with a ';'
				while (!tok.isEndLine() && !tok.isOp(";")) {
					tok = next_s();
				}
				tok = next_s();
				fQualifiers = 0;
			} else {
				tok = indent_block_or_statement(item, true);
			}
		}

		leave_scope(tok);

		end_of_scope(tok); // restore scope previously set

		tok = consume_labeled_block(next_s());

		if (fDebugEn) {
			debug("--> indent_ifc_module_class(" + item + ") next=" + 
				((tok != null)?tok.getImage():"null"));
		}

		return tok;
	}

	/**
	 * indent_config (String item)
	 * 
	 * Indents a configuration
	 * 
	 * @param item - ???
	 * @return
	 */
	private SVIndentToken indent_config(String item) {
		SVIndentToken tok = current_s();

		String end = "endconfig";
		if (fDebugEn) {
			debug("--> indent_config(" + item + ")");
		}

		start_of_scope(tok); // establish the indent of this scope
		// for adaptive-indent purposes

		tok = next_s();

		// Reach the end of the declaration
		while (!tok.isOp(";")) {
			tok = next_s();
		}

		tok = next_s();
		enter_scope(tok); // enter the body of the module

		fQualifiers = 0;

		// Now, read body items
		while (tok != null) {

			if (tok.isId(end)) {
				break;
			} else {
				tok = indent_block_or_statement(item, true);
			}
		}

		leave_scope(tok);
		end_of_scope(tok); // restore scope previously set

		tok = consume_labeled_block(next_s());

		if (fDebugEn) {
			debug("--> indent_config (" + item + ") next=" + 
				((tok != null)?tok.getImage():"null"));
		}

		return tok;
	}

	/**
	 * indent_export_import ()
	 * 
	 * Indents a verilog import / export function
	 * 
	 * export "DPI-C" f_plus = function \f+ ; // "f+" exported as "f_plus"
	 * export "DPI-C" function f; // "f" exported under its own name
	 * import "DPI-C" init_1 = function void \init[1] (); // "init_1" is a linkage name
	 * import "DPI-C" \begin = function void \init[2] (); // "begin" is a linkage name
	 * 
	 * @return
	 */
	private SVIndentToken indent_import_export() {
		SVIndentToken tok = current_s();
		
		start_of_scope(tok); // establish the indent of this scope
		// for adaptive-indent purposes
		
		tok = next_s();
		
		// Ensure that any comments at the beginning of the
		// task/function are indented correctly
		enter_scope(tok);
		tok = next_s();

		while (tok != null) {
			tok = indent_block_or_statement(null, true);
		}
		leave_scope(tok);
		end_of_scope(tok); // restore scope previously set
		
		return tok;
	}
	
	private static boolean is_always(SVIndentToken tok) {
		return (tok.isId("always") || tok.isId("always_comb") ||
				tok.isId("always_latch") || tok.isId("always_ff") ||
				tok.isOp("@"));
	}

	/**
	 * This function will check to see if the given token is an open brace
	 * Currently checks for ({[
	 * 
	 * @param tok - token to be evaluated
	 * @return
	 */
	private static boolean is_open_brace(SVIndentToken tok) {
		return (tok.isOp("(") || tok.isOp("{") ||
				tok.isOp("["));
	}

	/**
	 * This function will check to see if the given token is an open brace
	 * Currently checks for ({[
	 * 
	 * @param tok - token to be evaluated
	 * @return
	 */
	private static boolean is_close_brace(SVIndentToken tok) {
		return (tok.isOp(")") || tok.isOp("}") ||
				tok.isOp("]"));
	}
	
	public static boolean is_close_brace(String str) {
		return (str.equals(")") || str.equals("}") ||
				str.equals("]"));
	}

	/**
	 * Checks to see if id is one of the "normal" case statements:
	 *  - case
	 *  - casez
	 *  - casex
	 * @param tok
	 * @return boolean, true if is a match
	 */
	private static boolean is_case(SVIndentToken tok) {
		// return (tok.isId("case"));
		return (tok.isId("case") || tok.isId("casez") ||
				tok.isId("casex"));
	}

	private SVIndentToken indent_covergroup() {
		int level = fIndentStack.size();
		SVIndentToken tok = current_s();

		start_of_scope(tok);

		if (fDebugEn) {
			debug("--> indent_covergroup()");
		}

		start_of_scope(tok); // push a new scope for elements in the arg list
		// Reach the end of the declaration
		while (tok != null && !tok.isOp(";")) {
			tok = next_s();
		}
		leave_scope(); // leave arg-list scope

		tok = next_s();
		enter_scope(tok);

		// Now, read body items
		while (tok != null) {

			if (tok.isId("endgroup")) {
				leave_scope(tok);
				
				tok = next_s();
				if (tok != null && tok.getImage().equals(":")) {
					// labled covergroup
					tok = next_s(); // label
					tok = next_s(); // token after label
				}
				break;
			} else {
				tok = indent_covergroup_item();
			}
		}

		if (fDebugEn) {
			debug("<-- indent_covergroup() next=" +
				((tok != null)?tok.getImage():"null"));
		}

		if (fTestMode) {
			if (level != fIndentStack.size()) {
				throw new RuntimeException("Indent stack out-of-sync: " + 
						"entry=" + level + " exit=" + fIndentStack.size());
			}
		}

		return tok;
	}

	
	private SVIndentToken indent_specify() {
		int level = fIndentStack.size();
		SVIndentToken tok = current_s();

		if (fDebugEn) {
			debug("--> indent_specify() tok=" + tok.getImage());
		}

		start_of_scope(tok);
		tok = next_s();
		enter_scope(tok);

		while (tok != null) {
			if (tok.isId("endspecify")) {
				break;
			} else {
				tok = indent_specify_stmt();
			}
		}

		leave_scope(tok);
		tok = next_s();

		if (fDebugEn) {
			debug("--> indent_specify() next=" +
					((tok != null)?tok.getImage():"null"));
		}

		if (fTestMode) {
			if (level != fIndentStack.size()) {
				throw new RuntimeException("Indent stack out-of-sync: " + 
						"entry=" + level + " exit=" + fIndentStack.size());
			}
		}

		return tok;
	}

	private SVIndentToken indent_specify_stmt() {
		SVIndentToken tok = current_s();

		if (fDebugEn) {
			debug("--> indent_specify_stmt tok=" + tok.getImage());
		}

		if (tok.isId("if")) {
			start_of_scope(tok);

			tok = next_s();

			if (tok.isOp("(")) {
				tok = consume_expression();
			} else {
				return tok;
			}

			tok = indent_specify_stmt();

			leave_scope(tok);
		} else if (tok.isOp("(")) {

			tok = consume_expression();

			if (fDebugEn) {
				debug(" -- post-consume_expression: tok=" + tok.getImage());
			}

			if (tok.isOp("=")) {
				tok = next_s();

				if (fDebugEn) {
					debug(" -- post-=: tok=" + tok.getImage());
				}

				if (tok.isOp("(")) {
					tok = consume_expression();
				}

				if (fDebugEn) {
					debug(" -- post-consume_expression(2)=: tok=" + tok.getImage());
				}

				if (tok.isOp(";")) {
					tok = next_s();
				}
			} else {
				tok = next_s();
			}
		} else {
			// We must consume a token even if we don't know what we're indenting
			while ((tok = next_s()) != null && !tok.isOp(";") &&
					!tok.isId("endspecify")) {
				;
			}
		}

		if (fDebugEn) {
			debug("<-- indent_specify_stmt tok=" + tok.getImage());
		}

		return tok;
	}

	private SVIndentToken indent_generate() {
		int level = fIndentStack.size();
		SVIndentToken tok = current_s();

		if (fDebugEn) {
			debug("--> indent_generate()");
		}

		while (tok != null) {

			if (tok.isId("endgenerate")) {
				break;
			} else {
				tok = indent_block_or_statement(null, false);
			}
		}

		tok = next_s();

		if (fDebugEn) {
			debug("--> indent_generate() next=" +
					((tok != null)?tok.getImage():"null"));
		}

		if (fTestMode) {
			if (level != fIndentStack.size()) {
				throw new RuntimeException("Indent stack out-of-sync: " + 
						"entry=" + level + " exit=" + fIndentStack.size());
			}
		}

		return tok;
	}

	private SVIndentToken indent_constraint() {
		SVIndentToken tok = current_s();

		start_of_scope(tok);

		tok = next_s(); // name of constraint

		tok = next_s(); // Should be '{'

		if (!tok.isOp("{")) {
			// Not sure what happened...
			return tok;
		}

		tok = next_s();
		enter_scope(tok);

		while (!tok.isOp("}")) {
			tok = indent_constraint_stmt();
		}

		leave_scope(tok);

		tok = next_s();

		return tok;
	}

	private SVIndentToken indent_covergroup_item() {
		int level = fIndentStack.size();
		SVIndentToken tok = current();
		int lb_count = 0, rb_count = 0;

		if (fDebugEn) {
			debug("--> indent_covergroup_item: " + tok.getImage());
		}

		// Scan to the end of the statement / beginning of the block
		start_of_scope(tok);
		tok = next_s();
		// MSB: enter_scope(tok);
		boolean keep_going = true;
		boolean block_leave_scope = false;
		while (keep_going) {

			if (tok.isOp(";") && (lb_count == rb_count)) {
				keep_going = false;
				leave_scope(); // leave coverpoint-declaration indent scope
				tok = next_s();
			} else if (tok.isOp("{")) {
				// This is the opening brace for the coverpoint. We need to
				// leave the scope for the second-line indent of the coverpoint
				if (lb_count == 0 && !block_leave_scope) {
					leave_scope(tok); // leave coverpoint-declaration indent scope
					block_leave_scope = true;
				}
				lb_count++;
				start_of_scope(tok);
				tok = next_s();
				enter_scope(tok);
			} else if (tok.isOp("}")) {
				rb_count++;
				leave_scope(tok);
				tok = next_s();
				// Check to see if we have a case where we have an } {
				if (tok.isOp("{")) {
					lb_count++;
					start_of_scope(tok);
					tok = next_s();
				}
				if (lb_count == rb_count) {
					keep_going = false;
					// leave_scope(tok);
				}
			}
			else  {
				tok = next_s();
			}
		}
		// MSB: leave_scope(tok);

		if (fDebugEn) {
			debug("<-- indent_covergroup_item: " + tok.getImage());
		}

		if (fTestMode) {
			if (level != fIndentStack.size()) {
				throw new RuntimeException("Indent stack out-of-sync: " + 
						"entry=" + level + " exit=" + fIndentStack.size());
			}
		}

		return tok;
	}

	private SVIndentToken indent_task_function(String item) {
		SVIndentToken tok = current_s();
		start_of_scope(tok);

		String end = get_end_kw(item);
		if (fDebugEn) {
			debug("--> indent_task_function(" + item + ")");
		}

		while (tok != null && !tok.isOp(";")) {
			tok = next_s();
		}

		// If this is an extern function or task, we're done
		// If this is an export function or task, we're done
		// If this is an import function or task, we're done
		if (!isQualifierSet("extern") && !isQualifierSet("import") && !isQualifierSet("export")) {
			// Ensure that any comments at the beginning of the
			// task/function are indented correctly
			enter_scope(tok);
			tok = next_s();

			while (tok != null) {
				if (tok.isId(end)) {
					break;
				} else {
					tok = indent_block_or_statement(item, true);
				}
			}
			leave_scope(tok);

			tok = consume_labeled_block(next_s());
		} else {
			leave_scope();
			tok = next_s();
		}

		if (fDebugEn) {
			debug("--> indent_task_function(" + item + ") " +
				((tok != null)?tok.getImage():"null"));
		}

		end_of_scope();

		return tok;
	}

	/**
	 * 
	 * @param parent - if parent is begin, will do appropriate things with begin statement
	 * @param parent_is_block - If this is false, the statements will be indented, else treated as previously indented
	 * @return
	 */
	private SVIndentToken indent_block_or_statement(String parent, boolean parent_is_block) {
		SVIndentToken tok = current();
		if (fDebugEn) {
			debug("--> indent_block_or_statement() parent_is_block_stmt=" + parent_is_block + " tok=" + tok.getImage());
		}

		if (tok.isId("begin")) {
			parent = "begin";

			// Ensure that any comments at the beginning of the
			// block are indented correctly
			start_of_scope(tok);
			// begin often has : <label> consume this if it is there
			tok = consume_labeled_block(next_s());

			enter_scope(tok);

			while (tok != null) {
				if (fDebugEn) {
					debug("top of begin/end loop: " + tok.getType() + " " + tok.getImage());
				}
				if (tok.isId("end")) {
					leave_scope(tok);
					if (fDebugEn) {
						debug("Setting indent \"" + peek_indent() + "\"");
					}
					tok = consume_labeled_block(next_s());
					break;
				} else {
					tok = indent_block_or_statement(parent, true);
				}
			}
		} else {
			if (!parent_is_block) {
				start_of_scope(tok);
				enter_scope(tok);
			}
			tok = indent_stmt(parent, !parent_is_block);

			if (!parent_is_block) {
				leave_scope(tok);
			}
		}

		if (fDebugEn) {
			debug("<-- indent_block_or_statement() tok=" + 
				((tok != null)?tok.getImage():"null") + " parent=" + parent);
		}

		return tok;
	}

	/**
	 * This routine will handle pre-processor directives as follows: 
	 * - `ifdef, ifndef - indent 
	 * - `else - un-indent, then re-indent 
	 * - `endif - unindent 
	 * 
	 * The remaining directives in fPreProcDirectives will skip to end of line
	 * 
	 * @param parent
	 * @return
	 */
	private SVIndentToken indent_preproc() {
		SVIndentToken tok = fScanner.current();

		if (fDebugEn) {
			debug("indent_preproc: " + tok.getImage() + " " + pref_IndentIfdef);
		}

		// Test to see if we want the indenter to indent on ifdef's
		if (pref_IndentIfdef) {
			if (tok.isId("`ifdef") || tok.isId("`ifndef")) {
				start_of_scope(tok);
				addToken(tok);
				SVIndentToken n = fScanner.next();
				if (n != null) {
					addToken(n);
				}
				tok = fScanner.next(); // Add the next token being checked for

			} else if (tok.isId("`elsif")) {
				leave_scope(tok);
				addToken(tok);
				SVIndentToken n = fScanner.next();
				if (n != null) {
					addToken(n);
				}
				tok = fScanner.next(); // Add the next token being checked for
				start_of_scope(tok);
			} else if (tok.isId("`else")) {
				leave_scope(tok);
				addToken(tok);
				tok = fScanner.next(); // Swallow ifdef/ifndef
				start_of_scope(tok);
			} else if (tok.isId("`endif")) {
				leave_scope(tok);
				addToken(tok);
				tok = fScanner.next(); // Swallow ifdef/ifndef
			}
			// All other preprocessor directives run to end of line
			else {
				addToken(tok);
				tok = skip_to_end_of_line();
			}
		}
		// don't indent non-ifdef's ... just swallow this stuff
		else {
			addToken(tok);
			tok = skip_to_end_of_line();
		}

		return (tok);

	}

	/**
	 * This thing figures out what kind of statement we have to deal with, and
	 * calls the appropriate indenter (if/for/case etc)
	 * 
	 * @param parent
	 * @return
	 */
	private SVIndentToken indent_stmt(String parent, boolean parent_is_block) {
		SVIndentToken tok = current_s();
		boolean found_ifdef = false;
		boolean fix_previous_comment = false;

		if (fDebugEn) {
			debug("--> indent_stmt parent=" + parent + " tok=" + tok.getImage() + " parent_is_block=" + parent_is_block);
		}

		// Just indent the statement
		if (tok.isId("if")) {
			tok = indent_if(false);
		} else if (tok.isId("assert")) {
			tok = indent_assert(false);
		} else if (tok.isId("fork")) {
			tok = indent_fork();
		} else if (is_case(tok) || tok.isId("randcase")) {
			tok = indent_case();
		} else if (is_always(tok)) {
			tok = indent_always();
		} else if (tok.isId("initial") || tok.isId("final")) {
			// enter_scope(tok);
			tok = next_s();
			tok = indent_block_or_statement(null, false);
		} else if (tok.isId("assign")) {
			tok = next_s();
			tok = indent_block_or_statement(null, true);
		} else if (tok.isId("typedef")) {
			tok = indent_typedef();
		} else if (tok.isId("while") || tok.isId("do") ||
				tok.isId("repeat") || tok.isId("forever") ||
				tok.isId("for") || tok.isId("foreach")) {
			tok = indent_loop_stmt();
		} else if (tok.isOp("#")) {
			tok = indent_delay(parent, parent_is_block);
		} else if (tok.isId("unique") || tok.isId("priority")) {
			tok = indent_unique_priority();
		} else if (tok.isId("expect")) {
			tok = indent_if(false);
		} else if (tok.isId("randsequence")) {
			tok = indent_randsequence(false);
		}
		// Not seeing an if etc, just loop till we hit our next begin/end/fork/joinetc.]
		else {
			if (tok.fImage.startsWith("`"))  {
				found_ifdef = true;
				tok = next_s();
			}
			if (!parent_is_block) {
				// Check and consume first token, we need to indent after this because
				// user could have multi line code such as:
				// a =
				// b + c;
				if (is_open_brace(tok)) {
					tok = indent_braced_subexpression();
				}
				else  {
					tok = next_s(); // grab the next token, this was probably the first token of a new statement
				}
				// Next token will be indented by default in case multi-line comment
				start_of_scope(tok);
				enter_scope(tok);

			}
			// Need to check if we are at a label... for example a labeled property
			// ap_thing:
			// assert property (p_property (thevar));
			if (tok.getImage().equals(":")) {
				tok = next_s(); // grab the next token, this is the first token after the :
				tok = indent_stmt(parent, parent_is_block);
				leave_scope(tok); // Out-dent post - probable assert
				fixupPreviousCommentIndent(tok);
			}
			// Just a regular statement "a = b;" etc
			else {

				boolean do_next = true;
				while (!tok.isOp(";"))  {
					// In this section we deal with `uvm_macro () type things where a ; found at the end 
					// of a typical end of line is not present.
					// 
					// Simply put, the rule here is that if you find a `MACRO check the next token
					// - If it is followed by an operator, keep looking for a ;
					// - If not an operator, and the token is at the beginning of a new line, treat this token as a ;
					if (found_ifdef)  {
						// if we have an operator, reset the flag
						if (tok.isOp("") && !is_open_brace(tok))  {
							found_ifdef = false;
						}
						// New line ... treat like a ;
						else if (tok.fStartLine)  {
							// Have an ifdef, and next token is at the start of a new line, we are done
							do_next = false;				// Not a ;, don't skip forward
							fix_previous_comment = true;	// Have passed a comment, need to fix
							break;
						}
					}
					// Haven't found an ifdef... check if this token is an ifdef
					else if (tok.fImage.startsWith("`"))  {
						found_ifdef = true;
					}
					
					
					if (parent != null) {
						if ((parent.equals("begin") && tok.isId("end")) ||
								tok.isId("end" + parent)) {
							do_next = false;
							break;
						} else if (parent.equals("fork") && 
								(tok.isId("join") || tok.isId("join_any") || tok.isId("join_none"))) {
							do_next = false;
							break;
						}
					}

					// Check for and property indent things that are in brackets
					if (is_open_brace(tok)) {
						tok = indent_braced_subexpression();
					}
					else  {
						tok = next_s();
					}
				}
				// Un-indent after we indented
				if (!parent_is_block) {
					leave_scope(tok);
					if (fix_previous_comment)
						fixupPreviousCommentIndent(tok);
				}

				if (do_next) {
					tok = next_s();
				}
			}
		}

		if (fDebugEn) {
			debug("<-- indent_stmt parent=" + parent + " tok=" + tok.getImage());
		}

		return tok;
	}

	private SVIndentToken indent_constraint_block_or_stmt() {
		SVIndentToken tok = current_s();

		if (tok.isOp("{")) {
			start_of_scope(tok);
			tok = next_s();
			enter_scope(tok);

			while (!tok.isOp("}")) {
				tok = indent_constraint_block_or_stmt();
			}
			leave_scope(tok);

			tok = next_s();
		} else {
			tok = indent_constraint_stmt();
		}

		return tok;
	}

	private SVIndentToken indent_constraint_if_block_or_stmt() {
		SVIndentToken tok = current_s();

		if (tok.isOp("{")) {
			boolean begin_is_start_line = tok.isStartLine();

			if (begin_is_start_line) {
				enter_scope(tok);
			}
			tok = next_s();
			if (!begin_is_start_line) {
				enter_scope(tok);
			}

			while (!tok.isOp("}")) {
				tok = indent_constraint_block_or_stmt();
			}
			leave_scope(tok);

			tok = next_s();
		} else {
			enter_scope(tok);
			tok = indent_constraint_stmt();
			leave_scope(tok);
		}

		return tok;
	}

	private SVIndentToken indent_constraint_stmt() {
		SVIndentToken tok = current_s();

		if (tok.isId("if") || tok.isId("foreach")) {
			tok = indent_constraint_if(false);
		} 
		else if (tok.isId("unique")) {
			tok = next_s();
			tok = consume_expression();
			tok = next_s();	// will be a ;
		}
		else if (tok.isOp("(")) {
			// very likely an implication statement
			tok = consume_expression();

			// (expr) -> [stmt | stmt_block]
			if (tok.isOp("->") || tok.isOp("->>")) {
				tok = next_s();
				tok = indent_constraint_block_or_stmt();
			}
			// Otherwise, really don't know what's being specified
		} 
		else {
			// expression_or_dist ::= expression [ dist { dist_list } ];
			// An expression can be without '(' which makes this bit a little tricky
			// If we have an operator... consume it and the next token too
			tok = next_skip_over_hier();
			if (tok.isOp())  {
				tok = next_s(); // Eat the operator
				tok = next_skip_over_hier();	// Eat the next token
			}
			// (expr) -> [stmt | stmt_block]
			if (tok.isOp("->") || tok.isOp("->>")) {
				tok = next_s();
				tok = indent_constraint_block_or_stmt();
			}
			// check if we have a distribution here
			else if (tok.isId("dist") | tok.isId("inside")) {
				tok = next_s(); // move onto '{'
				tok = consume_expression();
				tok = next_s(); // consume trailing ;
			}
			// Expression
			else {
				// Read a statement up to a semi-colon
				while (!tok.isOp(";")) {
						tok = next_s();
				}

				tok = next_s();
			}
		}

		return tok;
	}

	private SVIndentToken indent_constraint_if(boolean is_else_if) {
		SVIndentToken tok = current();
		if (fDebugEn) {
			debug("--> indent_constraint_if() tok=" + tok.getImage());
		}

		start_of_scope(tok);

		tok = next_s();
		if (tok.isOp("(")) {
			tok = consume_expression();
		} else {
			// Doesn't seem right for an if
			return tok;
		}
		enter_scope(tok);

		tok = indent_constraint_if_block_or_stmt();

		if (tok.isId("else")) {
			tok = next_s();
			if (tok.isId("if")) {
				tok = indent_constraint_if(true);
			} else {
				tok = indent_constraint_block_or_stmt();
			}
		}

		if (fDebugEn) {
			debug("<-- indent_constraint_if() tok=" + 
				((tok != null)?tok.getImage():"null"));
		}

		return tok;
	}

	/**
	 * This will take a look at the various always... constructs and move through them, 
	 * hopefully working through the rest of the line to either a begin or a statement...
	 * Rules:
	 * always_latch, always_comb : jump straight to parsing statement or block
	 * always, always_ff, or @: make way through the @(...) and then parse statement or block
	 * @return
	 */
	private SVIndentToken indent_always() {
		SVIndentToken tok = current();
		// return (tok.isId("always") || tok.isId("always_comb") ||
		// tok.isId("always_latch") || tok.isId("always_ff"));
		if (tok.isId("always") || (tok.isId("always_ff") || tok.isId("always_latch") || tok.isId("always_comb")))  {
			tok=next_s();	// on always, always_ff this will be an @, otherwise will be begin or start of statement
		}
		// If we have an @, make way to the end of the expression
		if (tok.isOp("@")) {
			// swallow the expression (...) or @*
			tok = next_s();
			tok = consume_expression();
		}
		// By this point we should have reached a begin or statement
		return (indent_block_or_statement(null, false));
	}

	/**
	 * Used for <unique|priority> <if|case|casex|casez>
	 * 
	 * @param dont_indent_first
	 *            - Use when already indented... typicall in a unique or
	 *            priority case statement
	 * @return
	 */
	private SVIndentToken indent_unique_priority() {
		SVIndentToken tok = current();
		String type = tok.getImage();
		// Synchronize the indent on 'case'
		enter_scope(tok);

		// Push a new scope for the body of the case
		start_of_scope(tok);
		tok = next();

		if (tok.isId("if")) {
			tok = indent_if(false, true);
		} else if (is_case(tok)) {
			tok = indent_case(true);
		}
		return (tok);
	}

	private SVIndentToken indent_case() {
		return (indent_case(false));
	}

	/**
	 * 
	 * @param dont_indent_first
	 *            - Use when already indented... typically in a unique or
	 *            priority case statement
	 * @return
	 */
	private SVIndentToken indent_case(boolean dont_indent_first) {
		SVIndentToken tok = current();
		String type = tok.getImage();
		// Check if we need to indent the case
		if (!dont_indent_first) {
			// Synchronize the indent on 'case'
			enter_scope(tok);

			// Push a new scope for the body of the case
			start_of_scope(tok);
		}

		// randcase does not have an expression
		if (is_case(tok)) {
			tok = next_s(); // should be expression
			// Continue on till we get a closed brace.  We could have case (some_vec[3:2]).  
			// This is important because below we just check for : to determine indentation, and
			// a : in the indenter was throwing the indenter off
			if (tok.isOp("(")) {
				while (!tok.isOp(")"))  { // should be expression
					tok = next_s();
				}
			}
		}

		tok = next_s();

		// Synchronize indent
		enter_scope(tok);

		while (!tok.isId("endcase")) {
			while (!tok.isOp(":") && !tok.isId("endcase")) {
				tok = next_s();
			}

			if (tok.isOp(":")) {
				tok = next_s();
				tok = indent_block_or_statement("case", false);
			}
		}

		leave_scope();
		if (tok.isId("endcase")) {
			set_indent(tok, false, true);
		}

		tok = next_s();

		// leave_scope();

		return tok;
	}

	/**
	 * 
	 * @param dont_indent_first
	 *            - Use when already indented... typically in a unique or
	 *            priority case statement
	 * @return
	 */
	private SVIndentToken indent_randsequence(boolean dont_indent_first) {
		SVIndentToken tok = current();
		String type = tok.getImage();
		// Check if we need to indent the case
		if (!dont_indent_first) {
			// Synchronize the indent on 'case'
			enter_scope(tok);

			// Push a new scope for the body of the case
			start_of_scope(tok);
		}

		// randcase does not have an expression
		if (tok.isId("randsequence")) {
			tok = next_s(); // should be expression
			// Continue on till we get a closed brace.  We could have case (some_vec[3:2]).  
			// This is important because below we just check for : to determine indentation, and
			// a : in the indenter was throwing the indenter off
			if (tok.isOp("(")) {
				do  {
					tok = next_s();
				} while (!tok.isOp(")")); // should be expression
			}
		}

		tok = next_s();

		// Synchronize indent
		enter_scope(tok);

		while (!tok.isId("endsequence")) {
			// Label :
			while (!tok.isOp(":") && !tok.isId("endsequence")) {
				tok = next_s();
			}

			// Found a label, now parse the label contents
			if (tok.isOp(":")) {
				tok = next_s();
				tok = indent_randsequence_production_item();
			}
		}

		leave_scope();
		if (tok.isId("endsequence")) {
			set_indent(tok, false, true);
		}

		tok = next_s();

		// leave_scope();

		return tok;
	}

	/**
	 * 
	 * @param dont_indent_first
	 *            - Use when already indented... typically in a unique or
	 *            priority case statement
	 * @return
	 */
	private SVIndentToken indent_randsequence_production_item () {
		SVIndentToken tok = current();
		if (tok.isId("if")) {
			start_of_scope(tok);
			enter_scope(tok);
			tok = indent_if(false);
			leave_scope(tok);
		} else if (is_case(tok)) {
			start_of_scope(tok);
			enter_scope(tok);
			tok = indent_case(false);
			leave_scope(tok);
		} else if (tok.isOp("{"))  {
			tok = next_s();
			do  {
				tok = indent_randsequence_production_item();
			} while (!tok.isOp("}"));
			tok = next_s();		// Trailing ';'
			tok = next_s();
			
		}
		// Some string that terminates with a ;
		else  {
			start_of_scope(tok);
			enter_scope(tok);
			while (!tok.isOp(";"))  {
				tok = next_s();
			}
			leave_scope(tok);
			// Return after ;
			tok = next_s();
		}
		return (tok);
	}
	private void start_of_scope(SVIndentToken tok) {
		start_of_scope(tok, true);
	}

	/**
	 * start_of_scope()
	 * 
	 * Called before entry into a new scope. Increases the indent to apply
	 * to any comments between the current token and the first token in the
	 * new scope
	 * @param tok
	 */
	private void start_of_scope(SVIndentToken tok, boolean incr) {
		if (incr) {
			incr_indent(true);
		} else {
			push_indent_stack(peek_indent(), true);
		}
		/*
		if (isAdaptiveTraining(tok)) {
		} else {
			push_indent_stack(peek_indent(), false);
		}
		 */

		// if (is_top_scope) {
		// set_indent(tok, false);
		// }

		if (fDebugEn) {
			debug("[" + fIndentStack.size() + "] start_of_scope() - indent=\"" + peek_indent() + "\"");
		}
	}

	private void end_of_scope() {
		end_of_scope(null);
	}

	private void end_of_scope(SVIndentToken tok) {
		// leave_scope(tok);
	}

	/**
	 * enter_scope()
	 * 
	 * Called with the first token of the new scope.
	 * In adaptive-indent mode, updates the current indent level
	 * 
	 * @param tok
	 */
	private void enter_scope(SVIndentToken tok) {
		// boolean is_top_scope = (fIndentStack.size() == 1);
		// incr_indent(isAdaptiveTraining(tok));

		set_indent(tok, false, true);

		if (fDebugEn) {
			debug("enter_scope() - indent=\"" + peek_indent() + "\"");
		}
	}

	private void leave_scope() {
		leave_scope(null);
	}

	/**
	 * leave_scope()
	 * 
	 * @param tok
	 */
	private void leave_scope(SVIndentToken tok) {
		pop_indent(tok);
		if (fDebugEn) {
			debug("[" + fIndentStack.size() + "] leave_scope() - indent=\"" + peek_indent() + "\"");
		}
	}

	private void push_indent_stack(String indent, boolean provisional) {
		if (fDebugEn) {
			debug("[" + (fIndentStack.size() + 1) + 
					"] push_indent_stack: \"" + indent + "\" provisional=" + provisional);
		}
		fIndentStack.push(new Tuple<String, Boolean>(indent, provisional));
	}

	private String peek_indent() {
		return fIndentStack.peek().first();
	}

	private void incr_indent(boolean provisional) {
		push_indent_stack(fIndentStack.peek().first() + fIndentIncr, provisional);
		if (fDebugEn) {
			debug("incr_indent (provisional=" + provisional + ") - " +
					"indent=\"" + peek_indent() + "\"");
		}
	}

	private void pop_indent(SVIndentToken tok) {
		if (fDebugEn) {
			String img = (tok != null) ? tok.getImage() : "";
			int level = fIndentStack.size() - 1;
			if (level > 0) {
				debug("[" + level + "] pop_indent: \"" + fIndentStack.get(level-1).first() + "\" (" + img + ")");
			} else {
				debug("[" + level + "] pop_indent: (no previous level indent) (" + img + ")");
			}
		}
		if (fIndentStack.size() > 1) {
			fIndentStack.pop();
		} else {
			if (fTestMode) {
				throw new RuntimeException("Indent stack out of sync");
			}

			if (fDebugEn) {
				debug("[ERROR] indent stack out of sync");
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream(bos);

				try {
					throw new Exception();
				} catch (Exception e) {
					e.printStackTrace(ps);
				}
				ps.flush();
				debug(bos.toString());
			}
		}

		if (tok != null) {
			set_indent(tok, false, true);
		}

		if (fDebugEn) {
			debug("pop_indent - indent=\"" + peek_indent() + "\"");
		}
	}

	private void set_indent(SVIndentToken tok, boolean implicit, boolean ok_to_reset_indent) {
		if (tok.isStartLine()) {
			// We handle blank lines, comments, and pre-processor tokens outside
			// of the normal indenter flow. Consequently, we the normal indenter
			// flow must not interfere with these tokens
			boolean is_exceptional_tok = 
					(tok.isBlankLine() || tok.isComment() || tok.isPreProc());

			if (ok_to_reset_indent &&
					isAdaptiveTraining(tok) && 
					fIndentStack.peek().second() && !is_exceptional_tok) {
				// If we are in the training period, the indent
				// level is provisional, and this is not a blank
				// line, then sample the indent
				if (fDebugEn) {
					debug("[" + fIndentStack.size() + "] set_indent: convert implicit indent \"" + 
							fIndentStack.peek().first() + "\" to \"" +
							fCurrentIndent + "\" tok=\"" + tok.getImage() + "\"");
				}
				fIndentStack.peek().setFirst(fCurrentIndent);
			}
			debug("Set indent implicit=" + implicit + " ok_to_reset=" + 
					ok_to_reset_indent + " isAdativeTraining=" + 
					isAdaptiveTraining(tok) + " is_exceptional=" + 
					is_exceptional_tok + " \"" + tok.getImage()
					+ "\" \"" + peek_indent() + "\"");
			tok.setLeadingWS(peek_indent());
		}
	}

	private void indent_multi_line_comment(SVIndentToken tok) {
		SVMultiLineIndentToken ml_comment = (SVMultiLineIndentToken) tok;

		if (tok.isStartLine()) {
			set_indent(tok, false, true);
			for (SVIndentToken line : ml_comment.getCommentLines()) {
				if (line.getImage().startsWith("*")) {
					line.setLeadingWS(peek_indent() + " ");
				}
			}
		} else {
			if (fDebugEn) {
				debug("Multi-line comment is not start-line");
			}
		}
	}

	/**
	 * Call this function after begin, or end, endmodule etc. The function will check for a : some_label and consume it and move to the next token
	 * 
	 * Examples
	 * begin : some_label
	 * endmodule : module_name
	 * 
	 * @param tok
	 * @return
	 */
	private SVIndentToken consume_labeled_block(SVIndentToken tok) {
		if (tok.isOp(":")) {
			tok = next_s(); // consume the label
			tok = next_s(); // now move token to the next identifier ...
		}
		return tok;
	}

	/**
	 * consume_expression
	 * 
	 * This function will parse through an expression such as (a | b)
	 * If a start of line is encountered, subsequent code is indented
	 * The function will return if the end of expression is reached, typically
	 * a matching closing brace is reached.
	 * 
	 * Call this on the following tokens
	 * always @*
	 *         ^ Call on the token after an @
	 * if (...
	 *    ^ Call on the token after an if
	 * 
	 * @return Next Token
	 */
	private SVIndentToken consume_expression() {
		SVIndentToken tok = current();
		
		if (fDebugEn) {
			fLog.debug("--> consume_expression tok=" + tok.getImage());
		}
		boolean is_indent = false;
		boolean search_for_close_brace = false;
		boolean scope_started = false;
		// Get next token
		if (is_open_brace(tok)) {
			
			// Open brace is just before the new scope
			// start_of_scope captures the indent for this line, 
			// since we'll want to restore that indent on exit
			if (tok.isEndLine()) {
				start_of_scope(tok);
				scope_started = true;
			}
			
			tok = next_s();
			search_for_close_brace = true;
		}
		
		do {
			// braces on a new line get indented
			if (tok.isStartLine() && (is_indent == false)) {
				is_indent = true;
				if (!scope_started) {
					start_of_scope(tok);
				}
				enter_scope(tok);
			}
			// If we have an open brace, check if we need to indent, and call this function again to evaluate the expression
			if (is_open_brace(tok)) {
				// recursively call this function, checking for nested braces
				tok = consume_expression();
				// If we come back (will be on a brace, and we had just indented, 
			} else if (is_close_brace(tok)) {
				// Allow for ()
			} else {
				tok = next_s();
			}
		} while (!is_close_brace(tok) && search_for_close_brace);

		// If we come back (will be on a brace, and we had just indented,
		if (is_indent) {
			leave_scope(tok);
//			end_of_scope(tok);
		}

		if (is_close_brace(tok)) {
			tok = next_s();
		}

		if (fDebugEn) {
			fLog.debug("<-- consume_expression tok=" + tok.getImage());
		}
		return tok;
	}

	/**
	 * indent_braced_subexpression
	 * 
	 * This function will indent a sub-expression that has braces in it.  Typically
	 * this will be if statements with (), assign statements with complex assign terms etc.
	 * Check for and property indent things that are in brackets for example:
	 * assign a = (
	 *    b + (
	 *    (d + e)
	 *    )
	 *    );
	 * 
	 * Call this on when is_open_brace() returns true
	 * 
	 * @return Next Token
	 */
	private SVIndentToken indent_braced_subexpression() {
		SVIndentToken tok = current();
		int brace_level = 0;
		do {
			if (is_open_brace(tok)) {
				start_of_scope(tok);
				brace_level++;
				// Out-dent on successive )
			} else if (is_close_brace(tok)) {
				leave_scope(tok);
				brace_level--;
			}
			tok = next_s();
		} while (brace_level > 0);
		return (tok);
	}

	private boolean isAdaptiveTraining(SVIndentToken tok) {
		return (fAdaptiveIndentEnd != -1 && tok.getLineno() <= fAdaptiveIndentEnd);
	}

	private SVIndentToken next() {
		SVIndentToken tok = null;
		boolean stay_in_while = true;
		tok = fScanner.next();

		// Loop here while we are working through comments & preprocessor elements (`xxx)
		while (stay_in_while) {
			if (fDebugEn) {
				if (tok != null) {
					debug("next: tok \"" + tok.getType() + "\" \"" + tok.getImage() + "\"");
				} else {
					debug("next: null");
				}
			}
			
			if (tok == null) {
				stay_in_while = false;
			} else if (tok.getType() == SVIndentTokenType.SingleLineComment) {
				stay_in_while = true;
				// Only sample or set the indent if the comment is 
				// at the beginning of a line.
				if (tok.isStartLine()) {
					set_indent(tok, true, true);
				}
				addToken(tok);
				tok = fScanner.next();
			} else if (tok.getType() == SVIndentTokenType.MultiLineComment) {
				stay_in_while = true;
				indent_multi_line_comment(tok);
				addToken(tok);
				tok = fScanner.next();
			} else if (tok.getType() == SVIndentTokenType.BlankLine) {
				stay_in_while = true;
				// Uncomment these if we want to set blank-line whitespace ... tok.fLeadingWS = peek_indent();
				// Uncomment these if we want to set blank-line whitespace ... tok.fImage = "";
				addToken(tok);
				tok = fScanner.next();
			} else if (fPreProcDirectives.contains(tok.getImage())) {
				set_indent(tok, true, true);
				stay_in_while = true;
				tok = indent_preproc();
			} else {
				stay_in_while = false;
			}
		}

		if (tok != null) {
			if (tok.isOp("(")) {
				if (fNLeftParen == fNRightParen) {
					// TODO: enter_scope(tok);
				}
				fNLeftParen++;
			} else if (tok.isOp(")")) {
				fNRightParen++;
				if (fNLeftParen == fNRightParen) {
					// TODO: leave_scope(tok);
					fNLeftParen = fNRightParen = 0;
				}
			}
			if (tok.isStartLine()) {
				fCurrentIndent = tok.getLeadingNonNewLineWS();
				set_indent(tok, true, true);
			}
			addToken(tok);
		}

		fCurrent = tok;

		return tok;
	}

	private SVIndentToken current() {
		return fCurrent;
	}

	private SVIndentToken current_s() {
		if (current() == null) {
			throw new RuntimeException();
		}

		return current();
	}

	private SVIndentToken next_s() {
		SVIndentToken ret = next();

		if (fDebugEn) {
			if (ret != null) {
				debug("next_s: tok \"" + ret.getType() + "\" \"" + ret.getImage() + "\"");
			} else {
				debug("next_s: null");
			}
		}

		if (ret == null) {
			throw new IndentEOFException();
		}
		// Return the last
		return ret;
	}
	
	private void addToken(SVIndentToken tok) {
		fTokenList.add(tok);
	}

	/**
	 * This function was written to replace next_s when we are expecting an identifier with a hierarchy in it
	 * for example:
	 * top.bob = ... the function will run past the ., and return bob
	 * 
	 * @return The identifier in the hierarchy
	 */
	private SVIndentToken next_skip_over_hier() {
		SVIndentToken tok = current();
		tok = next_s(); // get the next id (possibly a hierarchy delimiter '.')
		while (tok.isOp(".")) {
			tok = next_s(); // identifier
			tok = next_s(); // possible hierarchy delimiter '.'
			// Check for bus values []
			if (tok.isOp("[")) {
				tok = consume_expression();
			}
		}
		return (tok); // return the last token in hierarchy
	}

	/**
	 * This function was written to replace next_s we want to run through to the end of the line
	 * and return at that point
	 * 
	 * @return First token on next line
	 */
	private SVIndentToken skip_to_end_of_line() {
		SVIndentToken tok = fScanner.current();
		while (tok != null && !tok.isEndLine()) {
			tok = fScanner.next();
			if (tok != null) {
				addToken(tok);
			}
		}

		tok = fScanner.next();

		return (tok);
	}

	private static String get_end_kw(String kw) {
		if (kw.equals("covergroup")) {
			return "endgroup";
		} else {
			return "end" + kw;
		}
	}

	private void debug(String msg) {
		if (fDebugEn) {
			fLog.debug(msg);
			// System.out.println("[INDENT] " + msg);
		}
	}

	private void fixupPreviousCommentIndent(SVIndentToken tok) {
		int i = fTokenList.size() - 1;

		// Find the target token
		while (i > 0 && fTokenList.get(i) != tok) {
			i--;
		}

		// Now, apply the indent of the token to any
		i--;
		while (i > 0 && 
				(fTokenList.get(i).getType() == SVIndentTokenType.SingleLineComment ||
				fTokenList.get(i).getType() == SVIndentTokenType.MultiLineComment)) {
			
			// Don't set leading whitespace for single-line non-start-of-line
			// comments
			if (fTokenList.get(i).getType() == SVIndentTokenType.MultiLineComment ||
					fTokenList.get(i).isStartLine()) {
				fTokenList.get(i).setLeadingWS(tok.getLeadingWS());
			}
			i--;
		}
	}
}
