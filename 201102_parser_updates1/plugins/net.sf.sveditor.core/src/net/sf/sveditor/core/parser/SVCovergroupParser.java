package net.sf.sveditor.core.parser;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.db.ISVDBChildItem;
import net.sf.sveditor.core.db.ISVDBScopeItem;
import net.sf.sveditor.core.db.SVDBCovergroup;
import net.sf.sveditor.core.db.SVDBCovergroup.BinsKW;
import net.sf.sveditor.core.db.SVDBCoverpoint;
import net.sf.sveditor.core.db.SVDBCoverpointBins;
import net.sf.sveditor.core.db.SVDBCoverpointBins.BinsType;
import net.sf.sveditor.core.db.SVDBCoverpointCross;
import net.sf.sveditor.core.db.SVDBLocation;
import net.sf.sveditor.core.db.expr.SVDBBinaryExpr;
import net.sf.sveditor.core.db.expr.SVDBCrossBinsSelectConditionExpr;
import net.sf.sveditor.core.db.expr.SVDBExpr;
import net.sf.sveditor.core.db.expr.SVDBFieldAccessExpr;
import net.sf.sveditor.core.db.expr.SVDBIdentifierExpr;
import net.sf.sveditor.core.db.expr.SVDBParenExpr;
import net.sf.sveditor.core.db.expr.SVDBUnaryExpr;
import net.sf.sveditor.core.db.stmt.SVDBCoverageCrossBinsSelectStmt;
import net.sf.sveditor.core.db.stmt.SVDBCoverageOptionStmt;

public class SVCovergroupParser extends SVParserBase {
	
	public SVCovergroupParser(ISVParser parser) {
		super(parser);
	}
	
	public void parse(ISVDBScopeItem parent) throws SVParseException {
		SVDBLocation start = fLexer.getStartLocation();
		fLexer.readKeyword("covergroup");
		String cg_name = fLexer.readId();

		SVDBCovergroup cg = new SVDBCovergroup(cg_name);
		cg.setLocation(start);
		

		while (fLexer.peekOperator("(")) {
			cg.setParamPort(parsers().tfPortListParser().parse());
		}

		if (fLexer.peekOperator("@@")) {
			// block_event_expression
			error("block_event_expression not supported for covergroup sampling");
		} else if (fLexer.peekOperator("@")) {
			cg.setCoverageEvent(parsers().exprParser().clocking_event());
		} else if (fLexer.peekKeyword("with")) {
			// with function sample
			error("with_function_sample not supported for covergroup sampling");
		}
		
		fLexer.readOperator(";");
		parent.addChildItem(cg);

		// Skip statements
		while (fLexer.peek() != null && !fLexer.peekKeyword("endgroup")) {
			ISVDBChildItem cov_item;

			if (isOption()) {
				cov_item = coverage_option();
			} else {
				cov_item = coverage_spec();
			}
			cg.addItem(cov_item);
		}

		cg.setEndLocation(fLexer.getStartLocation());
		fLexer.readKeyword("endgroup");
		
		if (fLexer.peekOperator(":")) {
			fLexer.eatToken();
			fLexer.readId(); // labeled group
		}
	}
	
	private SVDBCoverageOptionStmt coverage_option() throws SVParseException {
		// option or type_option
		String type = fLexer.eatToken();
		fLexer.readOperator(".");
		String name = fLexer.readId();
		
		SVDBCoverageOptionStmt opt = new SVDBCoverageOptionStmt(name, type.equals("type_option"));
		fLexer.readOperator("=");
		opt.setExpr(parsers().exprParser().expression());
		
		fLexer.readOperator(";");
		
		return opt;
	}
	
	private ISVDBChildItem coverage_spec() throws SVParseException {
		ISVDBChildItem ret = null;
		String name = "";
		SVDBLocation start = fLexer.getStartLocation();
		if (fLexer.peekId()) {
			name = fLexer.readId();
			fLexer.readOperator(":");
		}
		
		String type = fLexer.readKeyword("coverpoint", "cross");
		if (type.equals("coverpoint")) {
			SVDBCoverpoint cp = new SVDBCoverpoint(name);
			cp.setLocation(start);
			cover_point(cp);
			ret = cp;
		} else {
			SVDBCoverpointCross cp = new SVDBCoverpointCross(name);
			cp.setLocation(start);
			cover_cross(cp);
			ret = cp;
		}
		
		return ret;
	}
	
	private void cover_point(SVDBCoverpoint cp) throws SVParseException {
		cp.setTarget(parsers().exprParser().expression());
		
		if (fLexer.peekKeyword("iff")) {
			fLexer.eatToken();
			fLexer.readOperator("(");
			cp.setIFF(parsers().exprParser().expression());
			fLexer.readOperator(")");
		}
		
		if (fLexer.peekOperator("{")) {
			fLexer.eatToken();
			while (fLexer.peek() != null && !fLexer.peekOperator("}")) {
				if (isOption()) {
					cp.addItem(coverage_option());
				} else {
					boolean wildcard = fLexer.peekKeyword("wildcard");
					if (wildcard) {
						fLexer.eatToken();
					}
					
					String type = fLexer.readKeyword("bins", "illegal_bins", "ignore_bins");
					BinsKW kw = (type.equals("bins"))?BinsKW.Bins:
						(type.equals("illegal_bins"))?BinsKW.IllegalBins:BinsKW.IgnoreBins;
					String id = fLexer.readId();

					SVDBCoverpointBins bins = new SVDBCoverpointBins(wildcard, id, kw);

					boolean is_array = fLexer.peekOperator("[");
					bins.setIsArray(is_array);
					if (is_array) {
						fLexer.eatToken();
						if (fLexer.peekOperator("]")) {
							fLexer.eatToken();
						} else {
							bins.setArrayExpr(parsers().exprParser().expression());
						}
					}
					
					fLexer.readOperator("=");
					
					if (fLexer.peekKeyword("default")) {
						// Some sort of default bin
						fLexer.eatToken();
						boolean is_sequence = fLexer.peekKeyword("sequence");
						if (is_sequence) {
							fLexer.eatToken();
							bins.setBinsType(BinsType.DefaultSeq);
						} else {
							bins.setBinsType(BinsType.Default);
						}
					} else {
						if (fLexer.peekOperator("{")) {
							List<SVDBExpr> l = new ArrayList<SVDBExpr>();
							bins.setBinsType(BinsType.OpenRangeList);
							// TODO:
							parsers().exprParser().open_range_list(l);
						} else if (fLexer.peekOperator("(")) {
							bins.setBinsType(BinsType.TransList);
						} else {
							fLexer.readOperator("{", "(");
						}
					}
					
					if (fLexer.peekKeyword("iff")) {
						fLexer.eatToken();
						fLexer.readOperator("(");
						bins.setIFF(parsers().exprParser().expression());
						fLexer.readOperator(")");
					}
					cp.addItem(bins);
					fLexer.readOperator(";");
				}
			}
			fLexer.readOperator("}");
		} else {
			fLexer.readOperator(";");
		}
	}
	
	private void cover_cross(SVDBCoverpointCross cp) throws SVParseException {
		while (fLexer.peek() != null) {
			SVDBIdentifierExpr id = fParsers.exprParser().idExpr();
			cp.getCoverpointList().add(id);
		
			if (fLexer.peekOperator(",")) {
				fLexer.eatToken();
			} else {
				break;
			}
		}
		
		if (fLexer.peekKeyword("iff")) {
			fLexer.readOperator("(");
			cp.setIFF(parsers().exprParser().expression());
			fLexer.readOperator(")");
		}
		
		if (fLexer.peekOperator("{")) {
			fLexer.eatToken();
			while (fLexer.peek() != null && !fLexer.peekOperator("}")) {
				if (isOption()) {
					cp.addItem(coverage_option());
				} else {
					SVDBCoverageCrossBinsSelectStmt select_stmt = new SVDBCoverageCrossBinsSelectStmt();
					String type = fLexer.readKeyword("bins", "illegal_bins", "ignore_bins");
					select_stmt.setBinsType(type);
					select_stmt.setBinsName(fParsers.exprParser().idExpr());
					fLexer.readOperator("=");
					select_stmt.setSelectCondition(select_expression());
					
					if (fLexer.peekKeyword("iff")) {
						fLexer.eatToken();
						fLexer.readOperator("(");
						select_stmt.setIffExpr(fParsers.exprParser().expression());
						fLexer.readOperator(")");
					}
					fLexer.readOperator(";");
					cp.addItem(select_stmt);
				}
			}
			fLexer.readOperator("}");
		} else {
			fLexer.readOperator(";");
		}
	}
	
	private SVDBExpr select_expression() throws SVParseException {
		SVDBExpr expr = or_select_expression();
		
		return expr;
	}
	
	private SVDBExpr or_select_expression() throws SVParseException {
		SVDBExpr expr = and_select_expression();
		
		while (fLexer.peekOperator("||")) {
			fLexer.eatToken();
			expr = new SVDBBinaryExpr(expr, "||", and_select_expression());
		}
		
		return expr;
	}
	
	private SVDBExpr and_select_expression() throws SVParseException {
		SVDBExpr expr = unary_select_condition();
		
		while (fLexer.peekOperator("&&")) {
			fLexer.eatToken();
			expr = new SVDBBinaryExpr(expr, "&&", unary_select_condition());
		}
	
		return expr;
	}
	
	private SVDBExpr unary_select_condition() throws SVParseException {
		if (fLexer.peekOperator("!")) {
			return new SVDBUnaryExpr("!", select_condition());
		} else if (fLexer.peekOperator("(")) {
			fLexer.eatToken();
			SVDBParenExpr ret = new SVDBParenExpr(select_expression());
			fLexer.readOperator(")");
			return ret;
		} else {
			return select_condition();
		}
	}
	
	private SVDBExpr select_condition() throws SVParseException {
		SVDBLocation start = fLexer.getStartLocation();
		SVDBCrossBinsSelectConditionExpr select_c = new SVDBCrossBinsSelectConditionExpr();
		select_c.setLocation(start);
		
		fLexer.readKeyword("binsof");
		fLexer.readOperator("(");
		SVDBExpr bins_expr = fParsers.exprParser().idExpr();
		if (fLexer.peekOperator(".")) {
			fLexer.eatToken();
			bins_expr = new SVDBFieldAccessExpr(bins_expr, false, 
					fParsers.exprParser().idExpr());
		}
		select_c.setBinsExpr(bins_expr);
		fLexer.readOperator(")");
		
		if (fLexer.peekKeyword("intersect")) {
			fLexer.eatToken();
			fParsers.exprParser().open_range_list(select_c.getIntersectList());
		}
		
		return select_c;
	}
	
	private boolean isOption() throws SVParseException {
		if (fLexer.peekId()) {
			String id = fLexer.peek();
			return (id.equals("option") || id.equals("type_option"));
		} else {
			return false;
		}
	}
}
