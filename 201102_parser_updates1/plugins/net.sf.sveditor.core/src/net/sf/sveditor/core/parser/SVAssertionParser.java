package net.sf.sveditor.core.parser;

import net.sf.sveditor.core.db.stmt.SVDBAssertStmt;

public class SVAssertionParser extends SVParserBase {
	
	public SVAssertionParser(ISVParser parser) {
		super(parser);
	}
	
	public SVDBAssertStmt parse() throws SVParseException {
		SVDBAssertStmt assert_stmt = new SVDBAssertStmt();
		
		fLexer.readKeyword("assert");
		debug("assertion_stmt - " + fLexer.peek());

		if (fLexer.peekKeyword("property")) {
			fLexer.eatToken();
			// TODO: properly implement property expressions 
			fLexer.readOperator("(");
			fLexer.skipPastMatch("(", ")");
		} else {
			fLexer.readOperator("(");
			assert_stmt.setExpr(parsers().exprParser().expression());
			fLexer.readOperator(")");
		}

		assert_stmt.setActionBlock(parsers().behavioralBlockParser().action_block());
		
		return assert_stmt;
	}

}
