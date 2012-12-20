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


package net.sf.sveditor.core.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import net.sf.sveditor.core.db.SVDBLocation;
import net.sf.sveditor.core.db.SVDBTypeInfo;
import net.sf.sveditor.core.db.expr.SVCoverageExpr;
import net.sf.sveditor.core.db.expr.SVDBArrayAccessExpr;
import net.sf.sveditor.core.db.expr.SVDBAssignExpr;
import net.sf.sveditor.core.db.expr.SVDBAssignmentPatternExpr;
import net.sf.sveditor.core.db.expr.SVDBAssignmentPatternRepeatExpr;
import net.sf.sveditor.core.db.expr.SVDBBinaryExpr;
import net.sf.sveditor.core.db.expr.SVDBCastExpr;
import net.sf.sveditor.core.db.expr.SVDBClockingEventExpr;
import net.sf.sveditor.core.db.expr.SVDBClockingEventExpr.ClockingEventType;
import net.sf.sveditor.core.db.expr.SVDBConcatenationExpr;
import net.sf.sveditor.core.db.expr.SVDBCondExpr;
import net.sf.sveditor.core.db.expr.SVDBCoverBinsExpr;
import net.sf.sveditor.core.db.expr.SVDBCoverpointExpr;
import net.sf.sveditor.core.db.expr.SVDBCtorExpr;
import net.sf.sveditor.core.db.expr.SVDBCtorExpr.CtorType;
import net.sf.sveditor.core.db.expr.SVDBCycleDelayExpr;
import net.sf.sveditor.core.db.expr.SVDBExpr;
import net.sf.sveditor.core.db.expr.SVDBFieldAccessExpr;
import net.sf.sveditor.core.db.expr.SVDBIdentifierExpr;
import net.sf.sveditor.core.db.expr.SVDBIncDecExpr;
import net.sf.sveditor.core.db.expr.SVDBInsideExpr;
import net.sf.sveditor.core.db.expr.SVDBLiteralExpr;
import net.sf.sveditor.core.db.expr.SVDBMinTypMaxExpr;
import net.sf.sveditor.core.db.expr.SVDBNameMappedExpr;
import net.sf.sveditor.core.db.expr.SVDBNamedArgExpr;
import net.sf.sveditor.core.db.expr.SVDBNullExpr;
import net.sf.sveditor.core.db.expr.SVDBParamIdExpr;
import net.sf.sveditor.core.db.expr.SVDBParenExpr;
import net.sf.sveditor.core.db.expr.SVDBRandomizeCallExpr;
import net.sf.sveditor.core.db.expr.SVDBRangeDollarBoundExpr;
import net.sf.sveditor.core.db.expr.SVDBRangeExpr;
import net.sf.sveditor.core.db.expr.SVDBStringExpr;
import net.sf.sveditor.core.db.expr.SVDBTFCallExpr;
import net.sf.sveditor.core.db.expr.SVDBTypeExpr;
import net.sf.sveditor.core.db.expr.SVDBUnaryExpr;
import net.sf.sveditor.core.parser.SVLexer.Context;
import net.sf.sveditor.core.scanner.SVKeywords;

public class SVExprParser extends SVParserBase {
//	private SVExprDump						fExprDump;
//	private boolean							fDebugEn = false;
	public static boolean					fUseFullExprParser = true;
	private Stack<Boolean>					fEventExpr;
	private Stack<Boolean>					fAssertionExpr;
	private Stack<Boolean>					fArglistExpr;
	private boolean						fEnableNameMappedPrimary = false;
	
	public SVExprParser(ISVParser parser) {
		super(parser);
		fAssertionExpr = new Stack<Boolean>();
		fAssertionExpr.push(false);
		fEventExpr = new Stack<Boolean>();
		fEventExpr.push(false);
		fArglistExpr = new Stack<Boolean>();
		fArglistExpr.push(false);
//		fExprDump = new SVExprDump(System.out);
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////
	// This routine is used to parse an "@(xxx)"  
	// Formats supported are:
	// @*
	// @(*)
	// @(some_series_of_events)
	// @some event
	// Before calling fLexer.peekOperator("@") to prevent an exception
	///////////////////////////////////////////////////////////////////////////////////////////////
	public SVDBClockingEventExpr clocking_event() throws SVParseException {
		SVDBClockingEventExpr expr = new SVDBClockingEventExpr();
		fLexer.readOperator("@");
		// Check if there is an open brace - kill it if so
		if (fLexer.peekOperator("(")) {
			SVDBParenExpr p = new SVDBParenExpr();
			p.setLocation(fLexer.getStartLocation());
			fLexer.eatToken();
			// Handle @(*)
			if (fLexer.peekOperator("*"))  {
				// swallow the *
				fLexer.readOperator("*");
				expr.setClockingEventType(ClockingEventType.Any);
				// TODO: How do I set the expression?
			}
			// grab the event expression
			else  {
				expr.setClockingEventType(ClockingEventType.Expr);
				p.setExpr(event_expression());
				expr.setExpr(p);
			}
			fLexer.readOperator(")");
		}
		// handle @*
		else  if (fLexer.peekOperator("*"))  {
			expr.setClockingEventType(ClockingEventType.Any);
			// swallow the *
			fLexer.readOperator("*");
			// TODO: How do I set the expression?
//			expr.setExpr(idExpr());
		}
		// Handle @ some_event_name
		else  {
			expr.setClockingEventType(ClockingEventType.Expr);
			expr.setExpr(idExpr());
		}
		
		return expr;
	}
	
	private final static Set<String> fUnaryModulePathOperators;
	private final static Set<String> fBinaryModulePathOperators;
	static {
		fUnaryModulePathOperators = new HashSet<String>();
		fUnaryModulePathOperators.add("!");
		fUnaryModulePathOperators.add("~");
		fUnaryModulePathOperators.add("&");
		fUnaryModulePathOperators.add("~&");
		fUnaryModulePathOperators.add("|");
		fUnaryModulePathOperators.add("~|");
		fUnaryModulePathOperators.add("^");
		fUnaryModulePathOperators.add("~^");
		fUnaryModulePathOperators.add("^~");
		
		fBinaryModulePathOperators = new HashSet<String>();
		fBinaryModulePathOperators.add("==");
		fBinaryModulePathOperators.add("!=");
		fBinaryModulePathOperators.add("&&");
		fBinaryModulePathOperators.add("||");
		fBinaryModulePathOperators.add("&");
		fBinaryModulePathOperators.add("|");
		fBinaryModulePathOperators.add("^");
		fBinaryModulePathOperators.add("^~");
		fBinaryModulePathOperators.add("~^");
	}
	
	public SVDBExpr module_path_expression() throws SVParseException {
		SVDBExpr ret = null;
		if (fDebugEn) {
			debug("--> module_path_expression() " + fLexer.peek());
		}
		if (fLexer.peekOperator(fUnaryModulePathOperators)) {
			fLexer.eatToken();
			module_path_primary();
		} 
		
		if (fLexer.peekOperator(fBinaryModulePathOperators)) {
			String op = fLexer.eatToken();
			module_path_expression();
		}
		
		module_path_primary();
			
		if (fDebugEn) {
			debug("<-- module_path_expression() " + fLexer.peek());
		}
		return ret;
	}
	
	private SVDBExpr module_path_primary() throws SVParseException {
		SVDBExpr ret = null;
		if (fLexer.peekNumber()) {
			ret = literalExpr();
		} else if (fLexer.peekId()) {
			// id | function_subroutine_call
			ret = idExpr();
			// TODO: function_subroutine_call
			if (fLexer.peekOperator("(")) {
				error("function_subroutine_call unsupported");
			}
		} else if (fLexer.peekOperator("{")) {
			error("module_path_concatenation|module_path_multiple_concatenation unsupported");
			// module_path_concatenation | module_path_multiple_concatenation
			fLexer.eatToken(); // {
			if (fLexer.peekOperator("{")) {
				// module_path_multiple_concatenation
				// TODO:
			} else {
				
			}
		} else if (fLexer.peekOperator("(")) {
			// module_path_mintypmax_expression
			// TODO:
			error("module_path_mintypmax_expression unsupported");
		}
		return ret;
	}

	
	
	public SVDBExpr cycle_delay() throws SVParseException {
		SVDBCycleDelayExpr expr = new SVDBCycleDelayExpr();
		expr.setLocation(fLexer.getStartLocation());
		fLexer.readOperator("##");
		if (fLexer.peekNumber()) {
			expr.setExpr(literalExpr());
		} else if (fLexer.peekOperator("(")) {
			fLexer.readOperator("(");
			expr.setExpr(expression());
			fLexer.readOperator(")");
		} else {
			expr.setExpr(idExpr());
		}
		return expr;
	}
	
	// Checks for following:
	// #(delay)
	// #(rise_delay,fall_delay)
	// #(min_rise_delay:typ_rise_delay:max_rise_delay,min_fall_delay:typ_fall_delay:max_fall_delay)
	// #(min_delay:typ_delay:max_delay)
	// There are two delay types, delay 2 and delay 3.  The difference between them is that delay 2 has Rise and Fall, where delay 3 has rise, fall and tristate times
	public SVDBExpr delay_expr(int max_delays) throws SVParseException {
		SVDBExpr expr = null;
		if (fDebugEn) {debug("--> delay_expr - " + fLexer.peek());}

		if ((max_delays != 2) && (max_delays != 3))  {
			error ("delay_expr - should have either 2 or 3 as arguments");
		}
		fLexer.readOperator("#");
		if (fLexer.peekOperator("(")) {
			fLexer.eatToken();
			expr = fParsers.exprParser().expression();
			// TODO: save expression
			// Check for (min:typ:max) type of layout 
			if (fLexer.peekOperator(":"))  {
				fLexer.readOperator(":");
				expr = fParsers.exprParser().expression();
				// This should be another :, not going to test... going to assume that the parser will detect that this isn't a token
				fLexer.readOperator(":");
				expr = fParsers.exprParser().expression();
			}
			for (int i = 2; i<=max_delays; i++)  {
				// Check for "," which implies a falling edge delay
				if (fLexer.peekOperator(","))  {
					fLexer.eatToken();
					expr = fParsers.exprParser().expression();
					// TODO: save expression
					// Check for (min:typ:max) type of layout 
					if (fLexer.peekOperator(":"))  {
						fLexer.readOperator(":");
						expr = fParsers.exprParser().expression();
						// This should be another :, not going to test... going to assume that the parser will detect that this isn't a token
						fLexer.readOperator(":");
						expr = fParsers.exprParser().expression();
					}
				}
			}
			fLexer.readOperator(")");
		} else {
			expr = delay_value();
		}
		
		if (fDebugEn) {debug("<-- delay_expr - " + fLexer.peek());}
		return expr;
	}
	
	public SVDBExpr path_delay_value() throws SVParseException {
		boolean in_parens = fLexer.peekOperator("(");
		
		if (in_parens) {
			fLexer.eatToken();
		}

		while (fLexer.peek() != null) {
			expression();
			
			if (fLexer.peekOperator(",")) {
				fLexer.eatToken();
			} else {
				break;
			}
		}

		
		if (in_parens) {
			fLexer.readOperator(")");
		}
	
		// TODO:
		return null;
	}
	
	private SVDBExpr delay_value() throws SVParseException {
		SVDBExpr ret = null;
		if (fDebugEn) {debug("--> delay_value() : " + fLexer.peek());}
		
		if (fLexer.peekNumber()) {
			if (fDebugEn) {debug("  isNumber - " + fLexer.peek());}
			ret = new SVDBLiteralExpr(fLexer.eatToken());
		} else if (fLexer.peekKeyword("1step")) {
			ret = new SVDBLiteralExpr(fLexer.eatToken());
		} else if (fLexer.peekId()) {
			if (fDebugEn) {debug("  isIdExpression");}
			// expr = hierarchical_identifier(); // idExpr();
			ret = idExpr();
			
			if (fDebugEn) {debug("  postPrimary -- peek: " + fLexer.peek());}
			while (fLexer.peekOperator("::", ".", "[")) {
				SVToken t = fLexer.consumeToken();
				// Don't move forward if this is likely to be an assertion sequence
				if (fAssertionExpr.peek()) {
					if (!fLexer.peekOperator()) {
						fLexer.ungetToken(t);
						ret = selector(ret);
					} else {
						fLexer.ungetToken(t);
						break;
					}
				} else {
					fLexer.ungetToken(t);
					ret = selector(ret);
				}
			}
			
		} else {
			error("Expect number, '1step', or identifier ; receive " + fLexer.peek());
		}
		
		
		if (fDebugEn) {debug("<-- delay_value() : " + fLexer.peek());}

		return ret;
	}
	
	public SVDBExpr datatype_or_expression() throws SVParseException {
		if (fLexer.peekKeyword("virtual","const") || fLexer.peekKeyword(SVKeywords.fBuiltinTypes)) {
			// Know this is a type
			SVDBTypeExpr expr = new SVDBTypeExpr();
			expr.setLocation(fLexer.getStartLocation());
			
			SVDBTypeInfo info = fParsers.dataTypeParser().data_type(0);
			expr.setTypeInfo(info);
			
			return expr;
		} else {
			return expression();
		}
		
	}
	
	public SVDBExpr assert_expression() throws SVParseException {
		fAssertionExpr.push(true);
		fEventExpr.push(true);
		try {
			return expression();
		} finally {
			fAssertionExpr.pop();
			fEventExpr.pop();
		}
	}
	
	public SVDBExpr event_expression() throws SVParseException {
		if (fDebugEn) {debug("--> event_expression()");}
		fEventExpr.push(true);
		try {
			return expression();
		} finally {
			fEventExpr.pop();
		}
	}
	
	public SVDBExpr variable_lvalue() throws SVParseException {
		SVDBExpr lvalue;
		if (fDebugEn) {debug("--> variable_lvalue - " + fLexer.peek());}
		if (fLexer.peekOperator("{")) {
			lvalue = concatenation_or_repetition();
		} else {
			lvalue = unaryExpression();
		}
		
		if (fDebugEn) {debug("<-- variable_lvalue - " + fLexer.peek());}
		return lvalue;
	}
	
	public SVDBExpr const_or_range_expression() throws SVParseException {
		if (fDebugEn) {debug("--> const_or_range_expression - " + fLexer.peek());}
		SVDBExpr expr = expression();
		if (fLexer.peekOperator(":")) {
			fLexer.eatToken();
			expr = new SVDBRangeExpr(expr, expression());
		}
		if (fDebugEn) {debug("<-- const_or_range_expression - " + fLexer.peek());}
		return expr;
	}
	
	public SVDBExpr constant_mintypmax_expression() throws SVParseException {
		if (fDebugEn) {debug("<-- constant_mintypmax_expression - " + fLexer.peek());}
		SVDBExpr expr = expression();
		if (fLexer.peekOperator(":")) {
			fLexer.eatToken();
			SVDBExpr typ = expression();
			fLexer.readOperator(":");
			SVDBExpr max = expression();
			expr = new SVDBMinTypMaxExpr(expr, typ, max);
		}
		
		if (fDebugEn) {debug("<-- constant_mintypmax_expression - " + fLexer.peek());}
		return expr;
	}
	
	/**
	 * Expression := AssignmentExpression
	 * @param tok
	 * @return
	 * @throws SVParseException
	 */
	public SVDBExpr expression() throws SVParseException {
		SVDBExpr expr = null;
		
		Context saved_ctxt = fLexer.getContext();
		fLexer.setContext(Context.Expression);
		
		try {
			if (fDebugEn) {debug("--> expression() " + fLexer.peek());}
			expr = assignmentExpression();

			if (fEventExpr.peek() && fLexer.peekKeyword("iff")) {
				fLexer.eatToken();
				expr = new SVDBBinaryExpr(expr, "iff", expression());
			}

			if ((fEventExpr.peek() && !fArglistExpr.peek()) 
					&& fLexer.peekOperator(",")) {
				fLexer.eatToken();
				expr = new SVDBBinaryExpr(expr, ",", expression());
			}
			if (fDebugEn) {debug("<-- expression() after=" + fLexer.peek());}
		} finally {
			fLexer.setContext(saved_ctxt);
		}
		
		return expr; 
	}
	
	public SVDBExpr hierarchical_identifier() throws SVParseException {
		SVDBExpr ret;
		
		if (fDebugEn) {debug("--> hierarchical_identifier - " + fLexer.peek());}
		String id = fLexer.readId();
		
		if (fLexer.peekOperator(".","::")) {
			ret = new SVDBFieldAccessExpr(new SVDBIdentifierExpr(id), false, 
					hierarchical_identifier_int());
		} else {
			ret = new SVDBIdentifierExpr(id);
		}
		
		if (fDebugEn) {debug("<-- hierarchical_identifier - " + fLexer.peek());}
		
		return ret;
	}
	
	private SVDBExpr hierarchical_identifier_int() throws SVParseException {
		fLexer.readOperator(".","::");

		String id = fLexer.readId();
		
		if (fLexer.peekOperator(".","::")) {
			return new SVDBFieldAccessExpr(new SVDBIdentifierExpr(id), 
					false, hierarchical_identifier_int());
		} else {
			return new SVDBIdentifierExpr(id);
		}
	}

	/*
	@Deprecated
	public void init(final ITextScanner scanner) {
		fLexer.init(new ISVParser() {
			
			public void warning(String msg, int lineno) {
			}
			
			public SVLexer lexer() {
				// TODO Auto-generated method stub
				return lexer();
			}
			
			public boolean error_limit_reached() {
				return true;
			}
			
			public void error(SVParseException e) {}
			
			public void error(String msg) {}

			public SVParsers parsers() {
				return null;
			}
			
			public void debug(String msg, Exception e) {}
			
		}, scanner); 
	}
	 */
	
	/**
	 * Parse the description of the coverpoint target
	 * 
	 * @param coverpoint
	 */
	public void coverpoint_target(SVDBCoverpointExpr coverpoint) throws SVParseException {
		
		try {
			SVDBExpr target = expression();

			coverpoint.setTarget(target);

			if (fLexer.peekKeyword("iff")) {
				fLexer.eatToken();
				fLexer.readOperator("(");
				SVDBExpr iff_expr = expression();
				fLexer.readOperator(")");

				coverpoint.setIFFExpr(iff_expr);
			}
		} catch (EOFException e) {
			e.printStackTrace();
			// Ignore
		}
	}
	
	private static final Set<String> cp_body_items;
	static {
		cp_body_items = new HashSet<String>();
		cp_body_items.add("wildcard");
		cp_body_items.add("bins");
		cp_body_items.add("ignore_bins");
		cp_body_items.add("illegal_bins");
		cp_body_items.add("option");
		cp_body_items.add("type_option");
	}
	
	public void coverpoint_body(SVDBCoverpointExpr coverpoint) throws SVParseException {
		
		try {
			// "wildcard", "bins", "ignore_bins", "illegal_bins", "option", "type_option"
			while (fLexer.peekKeyword(cp_body_items)) {
				if (fLexer.peekKeyword("option", "type_option")) {
					String kw = fLexer.eatToken();

					fLexer.readOperator(".");

					String option = fLexer.readId();

					if (!fLexer.peekString() && !fLexer.peekNumber()) {
						error("unknown option value type \"" + fLexer.peek() + "\"");
					}

					if (kw.equals("option")) {
						coverpoint.addOption(option, fLexer.eatToken());
					} else {
						coverpoint.addTypeOption(option, fLexer.eatToken());
					}
				} else {
					if (fLexer.peekKeyword("wildcard")) {
						fLexer.eatToken();
					}

					String bins_kw = fLexer.readKeyword("bins", "ignore_bins", "illegal_bins");
					String bins_id = fLexer.readId();

					SVDBCoverBinsExpr bins = new SVDBCoverBinsExpr(bins_id, bins_kw);

					if (fLexer.peekOperator("[")) {
						fLexer.eatToken();

						bins.setIsArray(true);

						if (!fLexer.peekOperator("]")) {
							// read the inner expression
							bins.setArrayExpr(expression());
						}
						fLexer.readOperator("]");
					}

					fLexer.readOperator("=");

					if (fLexer.peekOperator("{")) {
						open_range_list(bins.getRangeList());
					} else if (fLexer.peekKeyword("default")) {
						fLexer.eatToken();
						bins.setIsDefault(true);
					} else {
						error("Unsupported coverage expression: " + fLexer.peek());
						// 'default' or 'default sequence'
						// ???
					}

					coverpoint.getCoverBins().add(bins);

					if (fLexer.peekOperator(";")) {
						fLexer.eatToken();
					}
				}
			}
		} catch (EOFException e ) {
			// Ignore
		}
	}
	
	public List<SVCoverageExpr> parse_covercross(InputStream in) throws SVParseException {
		return null;
	}
	
	
	/**
	 * p
	 * @return
	 * @throws SVExprParseException
	 * @throws SVParseException
	 */
	
	// constraint sc_mode_dist_c {sc_mode dist { 0 := 6, [1:2] := 2, [3:5] :/ 2};}
	
	/**
	 * AssignmentExpression :=
	 * 		ConditionalExpression [ AssignmentOperator AssignmentExpression]
	 * 
	 * @return
	 * @throws SVParseException
	 */
	public SVDBExpr assignmentExpression() throws SVParseException {
		if (fDebugEn) {debug("--> assignmentExpression()");}
		SVDBExpr a = conditionalExpression();
		
		if (fLexer.peekOperator(SVKeywords.fAssignmentOps)) {
			String op = fLexer.readOperator();
			SVDBExpr rhs = assignmentExpression();
			a = new SVDBAssignExpr(a, op, rhs);
		} else if (fLexer.peekKeyword("inside")) {
			fLexer.eatToken();
			SVDBInsideExpr inside = new SVDBInsideExpr(a);
			
			open_range_list(inside.getValueRangeList());
			
			a = inside;
			
			if (fLexer.peekOperator(SVKeywords.fBinaryOps)) {
				a = new SVDBBinaryExpr(a, fLexer.eatToken(), expression());
			}
		}

		if (fDebugEn) {debug("<-- assignmentExpression() " + fLexer.peek());}
		return a;
	}
	
	public void open_range_list(List<SVDBExpr> list) throws SVParseException {
		if (fDebugEn) {debug("--> open_range_list - " + fLexer.peek());}
		fLexer.readOperator("{");
		do {
			if (fLexer.peekOperator(",")) {
				fLexer.eatToken();
			}
			if (fLexer.peekOperator("[")) {
				list.add(parse_range());
			} else {
				list.add(expression());
			}
		} while (fLexer.peekOperator(","));
		fLexer.readOperator("}");
		if (fDebugEn) {debug("<-- open_range_list - " + fLexer.peek());}
	}
	
	public SVDBRangeExpr parse_range() throws SVParseException {
		if (fDebugEn) {debug("--> parse_range - " + fLexer.peek());}
		fLexer.readOperator("[");
		SVDBExpr left = expression();
		SVDBExpr right;
		fLexer.readOperator(":");
		if (fLexer.peekOperator("$")) {
			fLexer.eatToken();
			right = new SVDBRangeDollarBoundExpr();
		} else {
			right = expression();
		}
		fLexer.readOperator("]");
		
		if (fDebugEn) {debug("<-- parse_range - " + fLexer.peek());}
		return new SVDBRangeExpr(left, right);
	}
	
	/**
	 * conditionalExpression ::=
	 *     conditionalOrExpression [ '?' Expression ':' ConditionalExpression]
	 * @return
	 * @throws SVParseException
	 */
	public SVDBExpr conditionalExpression() throws SVParseException {
		if (fDebugEn) {debug("--> conditionalExpression()");}
		SVDBExpr a = conditionalOrExpression();
		
		if (fDebugEn) {debug("    post-conditionalOrExpression: " + fLexer.peek());}
		
		if (fLexer.peekOperator("?")) {
			fLexer.eatToken();

			SVDBExpr lhs = a;
			SVDBExpr mhs = expression();
			fLexer.readOperator(":");

			SVDBExpr rhs = conditionalExpression();
			a = new SVDBCondExpr(lhs, mhs, rhs);
		}
		if (fDebugEn) {debug("<-- conditionalExpression() ");}
		return a;
	}
	
	/**
	 * conditionalOrExpression ::=
	 * 		conditionalAndExpression { '||' conditionalAndExpression }
	 * @return
	 * @throws SVParseException
	 */
	public SVDBExpr conditionalOrExpression() throws SVParseException {
		if (fDebugEn) {debug("--> conditionalOrExpression()");}
		SVDBExpr a = conditionalAndExpression();
		
		while (fLexer.peekOperator("||") || (fEventExpr.peek() && fLexer.peekKeyword("or"))) {
			String op = fLexer.eatToken();
			a = new SVDBBinaryExpr(a, op, conditionalAndExpression());
		}
		
		if (fDebugEn) {debug("<-- conditionalOrExpression() ");}
		return a;
	}
	
	/**
	 * conditionalAndExpression ::=
	 * 	inclusiveOrExpression { '&&' inclusiveOrExpression }
	 * @return
	 * @throws SVParseException
	 */
	public SVDBExpr conditionalAndExpression() throws SVParseException {
		if (fDebugEn) {debug("--> conditionalAndExpression()");}
		SVDBExpr a = inclusiveOrExpression();
		
		while (fLexer.peekOperator("&&")) {
			fLexer.eatToken();
			a = new SVDBBinaryExpr(a, "&&", inclusiveOrExpression());
		}
		if (fDebugEn) {debug("<-- conditionalAndExpression()");}
		return a;
	}
	
	public SVDBExpr inclusiveOrExpression() throws SVParseException {
		if (fDebugEn) {debug("--> inclusiveOrExpression");}
		SVDBExpr a = exclusiveOrExpression();
		
		while (fLexer.peekOperator("|")) {
			fLexer.eatToken();
			a = new SVDBBinaryExpr(a, "|", exclusiveOrExpression());
		}
		
		if (fDebugEn) {debug("<-- inclusiveOrExpression");}
		return a;
	}
	
	public SVDBExpr exclusiveOrExpression() throws SVParseException {
		if (fDebugEn) {debug("--> exclusiveOrExpression");}
		SVDBExpr a = exclusiveNorExpression1();
		
		while (fLexer.peekOperator("^")) {
			fLexer.eatToken();
			a = new SVDBBinaryExpr(a, "^", exclusiveNorExpression1());
		}
		
		if (fDebugEn) {debug("<-- exclusiveOrExpression");}
		return a;
	}
	
	public SVDBExpr exclusiveNorExpression1() throws SVParseException {
		if (fDebugEn) {debug("--> exclusiveNorExpression1");}
		SVDBExpr a = exclusiveNorExpression2();
		
		while (fLexer.peekOperator("^~")) {
			fLexer.eatToken();
			a = new SVDBBinaryExpr(a, "^~", exclusiveNorExpression2());
		}
		
		if (fDebugEn) {debug("<-- exclusiveNorExpression1");}
		return a;
	}
	
	public SVDBExpr exclusiveNorExpression2() throws SVParseException {
		if (fDebugEn) {debug("--> exclusiveNorExpression2");}
		SVDBExpr a = andExpression();
		
		while (fLexer.peekOperator("~^")) {
			fLexer.eatToken();
			a = new SVDBBinaryExpr(a, "~^", andExpression());
		}
		
		if (fDebugEn) {debug("<-- exclusiveNorExpression2");}
		return a;
	}
	
	public SVDBExpr andExpression() throws SVParseException {
		if (fDebugEn) {debug("--> andExpression");}
		SVDBExpr a = equalityExpression();
		
		while (fLexer.peekOperator("&")) {
			fLexer.eatToken();
			a = new SVDBBinaryExpr(a, "&", equalityExpression());
		}
		
		if (fDebugEn) {debug("<-- andExpression");}
		return a;
	}
	
	public SVDBExpr equalityExpression() throws SVParseException {
		if (fDebugEn) {debug("--> equalityExpression");}
		SVDBExpr a = relationalExpression();
		
		while (fLexer.peekOperator("==", "!=", "===", "!==", "==?", "!=?")) {
			a = new SVDBBinaryExpr(a, fLexer.readOperator(), relationalExpression());
		}
		
		if (fDebugEn) {debug("<-- equalityExpression");}
		return a;
	}
	
	public SVDBExpr relationalExpression() throws SVParseException {
		if (fDebugEn) {debug("--> relationalExpression");}
		SVDBExpr a = shiftExpression();
		
		while (fLexer.peekOperator("<", ">", "<=", ">=")) {
			a = new SVDBBinaryExpr(a, fLexer.readOperator(), shiftExpression());
		}
		
		if (fDebugEn) {debug("<-- relationalExpression");}
		return a;
	}
	
	public SVDBExpr shiftExpression() throws SVParseException {
		if (fDebugEn) {debug("--> shiftExpression");}
		SVDBExpr a = additiveExpression();
		
		while (fLexer.peekOperator("<<", "<<<", ">>", ">>>")) {
			a = new SVDBBinaryExpr(a, fLexer.readOperator(), additiveExpression());
		}
		
		if (fDebugEn) {debug("<-- shiftExpression");}
		return a;
	}
	
	public SVDBExpr additiveExpression() throws SVParseException {
		if (fDebugEn) {debug("--> additiveExpression");}
		SVDBExpr a = multiplicativeExpression();
		
		while (fLexer.peekOperator("+", "-")) {
			a = new SVDBBinaryExpr(a, fLexer.readOperator(), multiplicativeExpression());
		}
		if (fDebugEn) {debug("<-- additiveExpression");}
		return a;
	}
	
	public SVDBExpr multiplicativeExpression() throws SVParseException {
		if (fDebugEn) {debug("--> multiplicativeExpression " + fLexer.peek());}
		SVDBExpr a = unaryExpression();
		
		while (fLexer.peekOperator("*", "/", "%", "**")) {
			a = new SVDBBinaryExpr(a, fLexer.readOperator(), unaryExpression());
		}
		if (fDebugEn) {debug("<-- multiplicativeExpression");}
		return a;
	}
	
	public SVDBExpr unaryExpression() throws SVParseException {
		if (fDebugEn) {debug("--> unaryExpression " + fLexer.peek());}
		if (fLexer.peekOperator("++", "--")) {
			return new SVDBIncDecExpr(fLexer.readOperator(), unaryExpression());
		} else if (fEventExpr.peek() && fLexer.peekKeyword("posedge", "negedge", "edge")) {
			SVDBExpr ret = new SVDBUnaryExpr(fLexer.eatToken(), expression());
			if (fLexer.peekKeyword("iff")) {
				fLexer.eatToken();
				ret = new SVDBBinaryExpr(ret, "iff", expression());
			}
			return ret;
		}
		if (fLexer.peekOperator("+", "-", "~", "!", "&", "~&", "|", "~|", "^", "~^", "^~") ||
				(fAssertionExpr.peek() && fLexer.peekOperator("*"))) {
			String op = fLexer.readOperator();
			SVDBUnaryExpr ret = new SVDBUnaryExpr(op, unaryExpression());
			
			if (fDebugEn) {debug("<-- unaryExpression " + op);}
			return ret; 
		} else if (fLexer.peekOperator("'")) {
			return assignment_pattern_expr();
		}
		
		SVDBExpr a = primary();
		
		if (fDebugEn) {debug("unaryExpr -- peek: " + fLexer.peek());}
		while (fLexer.peekOperator("::", ".", "[")) {
			SVToken t = fLexer.consumeToken();
			// Don't move forward if this is likely to be an assertion sequence
			if (fAssertionExpr.peek()) {
				if (!fLexer.peekOperator()) {
					fLexer.ungetToken(t);
					a = selector(a);
				} else {
					fLexer.ungetToken(t);
					break;
				}
			} else {
				fLexer.ungetToken(t);
				a = selector(a);
			}
		}

		if (fLexer.peekOperator("'")) {
			SVToken tok = fLexer.consumeToken();
			if (fLexer.peekOperator("{")) {
				fLexer.ungetToken(tok);
				a = assignment_pattern_expr();
			} else {
//				fLexer.ungetToken(tok);
				if (fDebugEn) {debug("    castExpr " + fLexer.peek());}
//				fLexer.eatToken();
				// MSB: new cast expression
				a = new SVDBCastExpr(a, expression());
			}
		}

		while (fLexer.peekOperator("++", "--")) {
			a = new SVDBIncDecExpr(fLexer.readOperator(), a);
		}
		
		return a;
	}
	
	private SVDBExpr assignment_pattern_expr() throws SVParseException {
		SVDBExpr ret_top;
		fLexer.readOperator("'");
		fLexer.readOperator("{");
		if (fDebugEn) {debug("    assignmentPattern");}
		
		if (fLexer.peekOperator("}")) {
			// empty_queue: '{}
			fLexer.eatToken();
			ret_top = new SVDBConcatenationExpr();
		} else {

			try {
				fEnableNameMappedPrimary = true;
				// This could be an associative-array initialization statement
				SVDBExpr expr1 = expression();
				if (fLexer.peekOperator("{")) {
					// repetition
					SVDBAssignmentPatternRepeatExpr ret = new SVDBAssignmentPatternRepeatExpr(expr1);

					fLexer.eatToken(); // {
					while (true) {
						SVDBExpr expr = expression();

						ret.getPatternList().add(expr);

						if (fLexer.peekOperator(",")) {
							fLexer.eatToken();
						} else {
							break;
						}
					}
					fLexer.readOperator("}");
					ret_top = ret;
				} else /* if (fLexer.peekOperator(":")) {
					// associative-array assignment
					SVDBAssignmentPatternExpr ret = new SVDBAssignmentPatternExpr();
					SVDBAssociativeArrayElemAssignExpr assign;

					while (fLexer.peek() != null) {
						assign = new SVDBAssociativeArrayElemAssignExpr();
						if (expr1 == null) {
							expr1 = expression();
						}
						assign.setKey(expr1);
						fLexer.readOperator(":");
						assign.setValue(expression());
						ret.getPatternList().add(assign);

						if (fLexer.peekOperator(",")) {
							fLexer.eatToken();
						} else {
							break;
						}
						expr1 = null;
					}
					ret_top = ret;
				} else */ {
					SVDBAssignmentPatternExpr ret = new SVDBAssignmentPatternExpr();
					ret.getPatternList().add(expr1);

					while (fLexer.peekOperator(",")) {
						fLexer.eatToken();
						SVDBExpr expr = expression();

						ret.getPatternList().add(expr);
					}
					ret_top = ret;
				}
				fLexer.readOperator("}");
			} finally {
				fEnableNameMappedPrimary = false;
			}
		}
		return ret_top;
	}
	
	public SVDBExpr primary() throws SVParseException {
		if (fDebugEn) {debug("--> primary() - " + fLexer.peek());}
		SVDBExpr ret = null;
		
		if (fLexer.peekOperator("(")) {
			if (fDebugEn) {debug("  Found paren in primary");}
			fLexer.eatToken();
			
			// if (isType) {
			// TODO
			//
			
			SVDBExpr a = expression();
			
			// TODO: save expression
			if (fLexer.peekOperator(":")) {
				fLexer.eatToken();
				SVDBExpr expr = fParsers.exprParser().expression();
				if (fLexer.peekOperator(":")) {
					fLexer.eatToken();
					expr = fParsers.exprParser().expression();
				}
			}
			
			fLexer.readOperator(")");
			
			// cast
			// '(' expression() ')' unaryExpression
			fLexer.peek();
			if (fLexer.isNumber() || fLexer.isIdentifier() ||
					fLexer.peekOperator("(", "~", "!") ||
					fLexer.peekKeyword("this", "super", "new")) {
				ret = new SVDBCastExpr(a, unaryExpression());
			} else {
				ret = new SVDBParenExpr(a);
			}
		} else {
			// TODO: must finish and figure out what's going on
			fLexer.peek();
			if (fLexer.isNumber()) {
				if (fDebugEn) {debug("-- primary is a number");}
				SVToken tmp = fLexer.consumeToken();
				if (fEnableNameMappedPrimary && fLexer.peekOperator(":")) {
					fLexer.eatToken();
					ret = new SVDBNameMappedExpr(tmp.getImage(), expression());
				} else {
					ret = new SVDBLiteralExpr(tmp.getImage());
				}
			} else if (fLexer.peekOperator("$")) {
				fLexer.eatToken();
				ret = new SVDBRangeDollarBoundExpr();
			} else if (fLexer.peekString()) {
				if (fDebugEn) {debug("-- primary is a string");}
				SVToken tmp = fLexer.consumeToken();
				if (fEnableNameMappedPrimary && fLexer.peekOperator(":")) {
					fLexer.eatToken();
					ret = new SVDBNameMappedExpr(tmp.getImage(), expression());
				} else {
					ret = new SVDBStringExpr(tmp.getImage());
				}
			} else if (fLexer.peekKeyword("null")) {
				if (fDebugEn) {debug("-- primary is 'null'");}
				fLexer.eatToken();
				ret = new SVDBNullExpr();
			} else if (fLexer.isIdentifier() || 
					SVKeywords.isBuiltInType(fLexer.peek()) ||
					fLexer.peekKeyword("new","default","local","const")) {
				if (fDebugEn) {
					debug("  primary \"" + fLexer.getImage() + "\" is identifier or built-in type");
				}
				String id = fLexer.eatToken();
				
				if (fLexer.peekOperator("(*")) {
					fParsers.attrParser().parse(null);
				}
				
				if (fLexer.peekOperator("#")) {
					if (fDebugEn) {
						debug("Parameterized identifier");
					}
					// Parameterized identifier
					ret = new SVDBParamIdExpr(id);
					fLexer.eatToken(); // #
					fLexer.readOperator("(");
					// Catch case where no parameters are specified in the parameter list
					while (fLexer.peek() != null && !fLexer.peekOperator(")")) {
						((SVDBParamIdExpr)ret).addParamExpr(datatype_or_expression());
						if (fLexer.peekOperator(",")) {
							fLexer.eatToken();
						} else {
							break;
						}
					}
					fLexer.readOperator(")");
				} else if (fLexer.peekOperator("(") || fLexer.peekKeyword("with")) {
					if (id.equals("randomize")) {
						ret = randomize_call(null);
					} else if (fLexer.peekOperator("(")) {
						ret = tf_args_call(null, id);
					} else {
						ret = tf_noargs_with_call(null, id);
					}
				} else if (id.equals("new")) {
					ret = ctor_call();
				} else if (fLexer.peekKeyword(SVKeywords.fBuiltinDeclTypes) ||
						fLexer.peekKeyword("const")) {
					fLexer.startCapture();
					fLexer.eatToken();
					if (fLexer.peekKeyword("signed","unsigned")) {
						fLexer.eatToken();
					}
					ret = new SVDBIdentifierExpr(fLexer.endCapture());
				} else {
					// ID or 'default'
					if (fEnableNameMappedPrimary && fLexer.peekOperator(":")) {
						fLexer.eatToken();
						if (fDebugEn) {debug("    nameMappedExpr");}
						ret = new SVDBNameMappedExpr(id, expression());
					} else {
						ret = new SVDBIdentifierExpr(id);
					}
					if (fDebugEn) {debug("  after id-read: " + fLexer.peek());}
				}
			} else if (fLexer.peekOperator("{")) {
				// concatenation
				ret = concatenation_or_repetition();
			} else if (fLexer.peekKeyword("this")) {
				fLexer.eatToken();
				ret = new SVDBIdentifierExpr("this");
			} else if (fLexer.peekKeyword("super")) {
				fLexer.eatToken();
				ret = new SVDBIdentifierExpr("super");
			} else if (fLexer.peekKeyword("void")) {
				fLexer.eatToken();
				ret = new SVDBIdentifierExpr("void");
			} else if (fEventExpr.peek() && fLexer.peekOperator("@")) {
				ret = clocking_event();
			} else if (fEventExpr.peek() && fLexer.peekOperator("##")) {
				ret = cycle_delay();
			} else {
				error("Unexpected token in primary: \"" + fLexer.getImage() + "\"");
			}
		}
		
		if (fDebugEn) {debug("<-- primary() ");}
		return ret;
	}
	
	private SVDBExpr concatenation_or_repetition() throws SVParseException {
		SVDBExpr expr = null;
		if (fDebugEn) {debug("--> concatenation_or_repetition()");}
		fLexer.readOperator("{");
		if (fLexer.peekOperator("}")) {
			// empty_queue
			fLexer.eatToken();
			expr = new SVDBConcatenationExpr();
		} else if (fLexer.peekOperator("<<", ">>")) {
			if (fDebugEn) {debug("streaming operator");}
			// TODO: preserve this portion of expression
			fLexer.eatToken();
			
			if (fLexer.peekKeyword(SVKeywords.fBuiltinTypes)) {
				expr = new SVDBTypeExpr(fParsers.dataTypeParser().data_type(0));
			} else if (!fLexer.peekOperator("{")) {
				expr = new SVDBLiteralExpr(fLexer.eatToken());
			}
			
			if (fDebugEn) {debug("post-datatype: " + fLexer.peek());}
			
			fLexer.readOperator("{");
			while (fLexer.peek() != null) {
				expression();
				
				if (fDebugEn) {debug("pre-with: " + fLexer.peek());}
				if (fLexer.peekKeyword("with")) {
					fLexer.eatToken();
					fLexer.readOperator("[");
					expression();
					if (fLexer.peekOperator(":", "+:", "-:")) {
						fLexer.eatToken();
						expression();
					}
					fLexer.readOperator("]");
				}
				
				if (fLexer.peekOperator(",")) {
					fLexer.eatToken();
				} else {
					break;
				}
			}
			fLexer.readOperator("}");
			fLexer.readOperator("}");
				
			expr = new SVDBConcatenationExpr();
		} else {
			try {
//				fEnableNameMappedPrimary = true;
				SVDBExpr expr0 = expression();

				// concatenation or repetition
				if (fLexer.peekOperator("{")) {
					if (fDebugEn) {debug("repetition");}
					fLexer.eatToken();
					// repetition
					SVDBAssignmentPatternRepeatExpr ret = new SVDBAssignmentPatternRepeatExpr(expr);
					ret.setRepeatExpr(expr0);

					while (fLexer.peek() != null) {
						ret.getPatternList().add(expression());
						if (fLexer.peekOperator(",")) {
							fLexer.eatToken();
						} else {
							break;
						}
					}
					fLexer.readOperator("}"); // end of inner expression
					fLexer.readOperator("}");
					expr = ret;
				} else {
					if (fDebugEn) {debug("concatenation");}
					SVDBConcatenationExpr ret = new SVDBConcatenationExpr();
					ret.getElements().add(expr0);

					while (fLexer.peekOperator(",")) {
						fLexer.eatToken();
						ret.getElements().add(expression());
					}

					fLexer.readOperator("}");

					expr = ret;
				}
			} finally {
//				fEnableNameMappedPrimary = false;
			}
		}
		if (fDebugEn) {debug("<-- concatenation_or_repetition()");}
		return expr;
	}
	
	public List<SVDBExpr> arguments() throws SVParseException {
		List<SVDBExpr> arguments = null;
		
		if (fDebugEn) {debug("--> arguments()");}
		fLexer.readOperator("(");
	
		// An argument list (tf-call) is neither an assertion nor an event context
		fAssertionExpr.push(false);
		fArglistExpr.push(true);
	
		try {
			if (fLexer.peekOperator(")")) {
				fLexer.eatToken();
				return new ArrayList<SVDBExpr>();
			}
		
			arguments = argumentList();
		
			fLexer.readOperator(")");
		} finally {
			fAssertionExpr.pop();
			fArglistExpr.pop();
		}
		if (fDebugEn) {debug("<-- arguments()");}
		return arguments;
	}
	
	private List<SVDBExpr>  argumentList() throws SVParseException {
		List<SVDBExpr> arguments = new ArrayList<SVDBExpr>();
		if (fDebugEn) {debug("--> argumentList() " + fLexer.peek());}
		
		for (;;) {
			if (fDebugEn) {debug("   argument: " + fLexer.peek());}
			if (fLexer.peekOperator(".")) {
				// named argument
				fLexer.eatToken();
				SVDBNamedArgExpr arg_expr = new SVDBNamedArgExpr();
				String name = fLexer.readId();
				arg_expr.setArgName(name);
				fLexer.readOperator("(");
				if (fLexer.peekOperator(")")) {
					// empty argument specifier
					arg_expr.setExpr(new SVDBLiteralExpr(""));
				} else {
					arg_expr.setExpr(expression());
				}
				fLexer.readOperator(")");
				arguments.add(arg_expr);
			} else if (fLexer.peekOperator(",", ")")) {
				// default value for this parameter
				arguments.add(new SVDBLiteralExpr(""));
			} else {
				if (fDebugEn) {debug("   --> argument_expr " + fLexer.peek());}
				arguments.add(expression());
				if (fDebugEn) {debug("   <-- argument_expr " + fLexer.peek());}
			}
			
			if (fLexer.peekOperator(",")) {
				fLexer.eatToken();
			} else {
				break;
			}
		}
		
		if (fDebugEn) {debug("<-- argumentList()");}
		return arguments;
	}
	
	public SVDBExpr selector(SVDBExpr expr) throws SVParseException {
		if (fDebugEn) {debug("--> selector() " + fLexer.peek());}
		if (fLexer.peekOperator(".", "::")) {
			String q = fLexer.eatToken();
			
			fLexer.peek();
			if (fLexer.isIdentifier() || fLexer.peekKeyword("new", "super", "this")) {
				SVToken id_tok = fLexer.consumeToken();
				String id = id_tok.getImage();
				
				if (fLexer.peekOperator("(*")) {
					fParsers.attrParser().parse(null);
				}

				if (fLexer.peekOperator("(") || fLexer.peekKeyword("with")) {
					if (id.equals("randomize")) {
						return randomize_call(expr);
					} else if (fLexer.peekOperator("(")){
						return tf_args_call(expr, id);
					} else {
						return tf_noargs_with_call(expr, id);
					}
				}
				if (q.equals(".")) {
					// '.' identifier
					if (fDebugEn) {debug("<-- selector() - IdentifierExpr");}
					return new SVDBFieldAccessExpr(expr, (q.equals("::")), 
							new SVDBIdentifierExpr(id));
				} else {
					// '::' identifier|parameterized_classtype
					
					if (fLexer.peekOperator("#")) {
						// Parameterized class type
						fLexer.ungetToken(id_tok);
						SVDBTypeExpr rhs = new SVDBTypeExpr();
						rhs.setLocation(fLexer.getStartLocation());
					
						SVDBTypeInfo info = fParsers.dataTypeParser().data_type(0);
						rhs.setTypeInfo(info);
						if (fDebugEn) {
							debug("  rhs=" + rhs);
							debug("<-- selector() - DataType");
						}
						return new SVDBFieldAccessExpr(expr, true, rhs);
					} else {
						if (fDebugEn) {debug("<-- selector() - IdentifierExpr(2)");}
						return new SVDBFieldAccessExpr(expr, (q.equals("::")), 
								new SVDBIdentifierExpr(id));
					}
				}
			}
		}

		// TODO: keyword new
		// TODO: keyword class`
		
		if (fLexer.peekOperator("[")) {
			if (fDebugEn) {debug("primary() -- operator " + fLexer.peek());}
			// '[' expression ']'
			fLexer.eatToken();
			SVDBExpr low = expression();
			SVDBExpr high = null;
			
			// TODO: should probably preserve array-bounds method
			if (fLexer.peekOperator(":", "+:", "-:")) {
				fLexer.eatToken();
				high = expression();
			}
			
			fLexer.readOperator("]");
			if (expr == null) {
				error("array expr == null");
			}
			if (fDebugEn) {debug("<-- selector()");}
			return new SVDBArrayAccessExpr(expr, low, high);
		}
		
		error("Unexpected token \"" + fLexer.getImage() + "\"");
		return null; // unreachable, since error always throws an exception
	}
	
	private SVDBRandomizeCallExpr randomize_call(SVDBExpr target) throws SVParseException {
		List<SVDBExpr> arguments = null;
		SVDBRandomizeCallExpr rand_call = null;
		
		// Body of a randomize() call should not user assertion semantics
		fAssertionExpr.push(false);
		fEventExpr.push(false);
		try {
			if (fLexer.peekOperator("(")) {
				arguments = arguments();
			}

			rand_call = new SVDBRandomizeCallExpr(target, "randomize", arguments);

			if (fLexer.peekKeyword("with")) {
				fLexer.eatToken();
				// constraint block
				rand_call.setWithBlock(fParsers.constraintParser().constraint_set(true));
			}
		} finally {
			fAssertionExpr.pop();
			fEventExpr.pop();
		}
		return rand_call;
	}
	
	private SVDBTFCallExpr tf_args_call(SVDBExpr target, String id) throws SVParseException {
		SVDBTFCallExpr tf = new SVDBTFCallExpr(target, id, arguments());
		
		if (fLexer.peekKeyword("with")) {
			fLexer.eatToken();
			fLexer.readOperator("(");
			tf.setWithExpr(expression());
			fLexer.readOperator(")");
		}
		
		return tf;
	}
	
	private SVDBTFCallExpr tf_noargs_with_call(SVDBExpr target, String id) throws SVParseException {
		SVDBTFCallExpr tf = new SVDBTFCallExpr(target, id, null);
		
		// TODO:
		if (fLexer.peekKeyword("with")) {
			fLexer.eatToken();
			if (fLexer.peekOperator("[")) {
				fLexer.readOperator("[");
				tf.setWithExpr(expression());
				if (fLexer.peekOperator(":", "+:","-:")) {
					fLexer.eatToken();
					expression();
				}
				fLexer.readOperator("]");
			} else {
				fLexer.readOperator("(");
				tf.setWithExpr(expression());
				fLexer.readOperator(")");
			}
		}
		
		return tf; 
	}
	
	private SVDBCtorExpr ctor_call() throws SVParseException {
		if (fDebugEn) {
			debug("--> ctor_call()");
		}
		SVDBCtorExpr ctor = new SVDBCtorExpr();
		if (fLexer.peekOperator("[")) {
			// array constructor
			fLexer.readOperator("[");
			ctor.setCtorType(CtorType.CtorType_Dim);
			ctor.setArg(expression());
			fLexer.readOperator("]");
		}
		if (fLexer.peekOperator("(")) {
			ctor.setCtorType(CtorType.CtorType_Args);
			ctor.setArgs(arguments());
		} else if (fLexer.peekKeyword() || fLexer.peekId()) {
			ctor.setCtorType(CtorType.CtorType_Expr);
			ctor.setArg(expression());
		}
		
		if (fDebugEn) {
			debug("--> ctor_call()");
		}
		return ctor;
	}

	public SVDBIdentifierExpr idExpr() throws SVParseException {
		SVDBLocation start = fLexer.getStartLocation();
		SVDBIdentifierExpr ret = new SVDBIdentifierExpr(fLexer.readId());
		ret.setLocation(start);
		
		return ret;
	}
	
	public SVDBLiteralExpr literalExpr() throws SVParseException {
		SVDBLocation start = fLexer.getStartLocation();
		SVDBLiteralExpr ret = new SVDBLiteralExpr(fLexer.readNumber());
		ret.setLocation(start);
		
		return ret;
	}
}
