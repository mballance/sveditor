package net.sf.sveditor.core.parser;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.db.ISVDBAddChildItem;
import net.sf.sveditor.core.db.SVDBLocation;
import net.sf.sveditor.core.db.SVDBSequence;
import net.sf.sveditor.core.db.SVDBTypeInfo;
import net.sf.sveditor.core.db.SVDBTypeInfoBuiltin;
import net.sf.sveditor.core.db.stmt.SVDBParamPortDecl;
import net.sf.sveditor.core.db.stmt.SVDBVarDeclItem;
import net.sf.sveditor.core.scanner.SVKeywords;

public class SVSequenceParser extends SVParserBase {
	
	public SVSequenceParser(ISVParser parser) {
		super(parser);
	}
	
	public void sequence(ISVDBAddChildItem parent) throws SVParseException {
		SVDBSequence seq = new SVDBSequence();
		seq.setLocation(fLexer.getStartLocation());
		fLexer.readKeyword("sequence");
		
		seq.setName(fLexer.readId());
		
		if (fLexer.peekOperator("(")) {
			// sequence_port_list
			fLexer.eatToken();
			if (!fLexer.peekOperator(")")) {
				while (fLexer.peek() != null) {
					seq.addPort(sequence_port_item());
					if (fLexer.peekOperator(",")) {
						fLexer.eatToken();
					} else {
						break;
					}
				}
			}
			fLexer.readOperator(")");
		}
		fLexer.readOperator(";");
		
		parent.addChildItem(seq);
		
		// data declarations
		while (fLexer.peekKeyword(SVKeywords.fBuiltinDeclTypes) || fLexer.peekKeyword("var") || fLexer.isIdentifier()) {
			SVDBLocation start = fLexer.getStartLocation();
			if (fLexer.peekKeyword("var") || fLexer.peekKeyword(SVKeywords.fBuiltinDeclTypes)) {
				// Definitely a declaration
				parsers().blockItemDeclParser().parse(seq, null, start);
			} else {
				// May be a declaration. Let's see
				// pkg::cls #(P)::field = 2; 
				// pkg::cls #(P)::type var;
				// field.foo
				SVToken tok = fLexer.consumeToken();
				
				if (fLexer.peekOperator("::","#") || fLexer.peekId()) {
					// Likely to be a declaration. Let's read a type
					fLexer.ungetToken(tok);
					final List<SVToken> tok_l = new ArrayList<SVToken>();
					ISVTokenListener l = new ISVTokenListener() {
						public void tokenConsumed(SVToken tok) {
							tok_l.add(tok);
						}
						public void ungetToken(SVToken tok) {
							tok_l.remove(tok_l.size()-1);
						}
					}; 
					SVDBTypeInfo type = null;
					try {
						fLexer.addTokenListener(l);
						type = parsers().dataTypeParser().data_type(0);
					} finally {
						fLexer.removeTokenListener(l);
					}
					
					// Okay, what's next?
					if (fLexer.peekId()) {
						// Conclude that this is a declaration
						debug("Assume a declaration @ " + fLexer.peek());
						parsers().blockItemDeclParser().parse(seq, type, start);
					} else {
						debug("Assume a typed reference @ " + fLexer.peek());
						// Else, this is probably a typed reference
						fLexer.ungetToken(tok_l);
						break;
					}
				} else {
					// More likely to not be a type
					fLexer.ungetToken(tok);
					break;
				}
			}
		}
		
		// Expression
		seq.setExpr(fParsers.propertyExprParser().sequence_expr());
		
		fLexer.readOperator(";");
		
		fLexer.readKeyword("endsequence");
		if (fLexer.peekOperator(":")) {
			fLexer.eatToken();
			fLexer.readId();
		}
	}

	private SVDBParamPortDecl sequence_port_item() throws SVParseException {
		int attr = 0;
		SVDBParamPortDecl port = new SVDBParamPortDecl();
		port.setLocation(fLexer.getStartLocation());
		if (fLexer.peekKeyword("local")) {
			fLexer.eatToken();
			// TODO: save local as an attribute
			if (fLexer.peekKeyword("input","inout","output")) {
				String dir = fLexer.eatToken();
				if (dir.equals("input")) {
					attr |= SVDBParamPortDecl.Direction_Input;
				} else if (dir.equals("inout")) {
					attr |= SVDBParamPortDecl.Direction_Inout;
				} else {
					attr |= SVDBParamPortDecl.Direction_Output;
				}
			}
		}
		port.setAttr(attr);
		
		if (fLexer.peekKeyword("sequence","event","untyped")) {
			port.setTypeInfo(new SVDBTypeInfoBuiltin(fLexer.eatToken()));
		} else {
			if (fLexer.peekId()) {
				SVToken t = fLexer.consumeToken();
				if (fLexer.peekId()) {
					fLexer.ungetToken(t);
					port.setTypeInfo(fParsers.dataTypeParser().data_type(0));
				} else {
					// implicit type
					fLexer.ungetToken(t);
				}
			} else {
				// data_type_or_implicit
				port.setTypeInfo(fParsers.dataTypeParser().data_type(0));
			}
		}
		SVDBVarDeclItem vi = new SVDBVarDeclItem();
		vi.setLocation(fLexer.getStartLocation());
		vi.setName(fLexer.readId());
		port.addChildItem(vi);
		
		if (fLexer.peekOperator("[")) {
			vi.setArrayDim(fParsers.dataTypeParser().var_dim());
		}
		
		if (fLexer.peekOperator("=")) {
			fLexer.eatToken();
			vi.setInitExpr(fParsers.exprParser().expression());
		}
		
		return port;
	}
}
