package net.sf.sveditor.core.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import net.sf.sveditor.core.parser.SVToken.Type;

public class SVLexer {
	private SVInputStream			fInput;
	private Stack<SVToken>			fUngetStack;
	private StringBuffer			fBuffer;
	private final Map<Integer, SVToken.Type>		fSingleCharOpMap;
	
	public SVLexer(SVInputStream in) {
		fInput = in;
		fUngetStack = new Stack<SVToken>();
		fBuffer = new StringBuffer();
		
		fSingleCharOpMap = new HashMap<Integer, SVToken.Type>();
		fSingleCharOpMap.put((int)',', SVToken.Type.Comma);
		fSingleCharOpMap.put((int)';', SVToken.Type.Semicolon);
		fSingleCharOpMap.put((int)':', SVToken.Type.Colon);
	}
	
	public SVToken next_token() {
		if (fUngetStack.size() > 0) {
			return fUngetStack.pop();
		}
		
		SVToken ret = null;
		int ch = fInput.get_ch();
		
		fBuffer.setLength(0);
		
		// First, skip whitespace and comments
		System.out.println("call skipCommentsWs: " + (char)ch);
		ch = skipCommentsWS(ch);
		
		// Now, see what we have...
		
		System.out.println("ch=" + (char)ch);
		if (ch == '"') {
			// String
			while ((ch = fInput.get_ch()) != -1 && ch != '"') {
				fBuffer.append((char)ch);
			}
			ret = new SVToken(Type.String, fBuffer.toString());
		} else if (fSingleCharOpMap.containsKey(ch)) {
			System.out.println("single-char op: " + (char)ch);
			return new SVToken(fSingleCharOpMap.get(ch), "" + (char)ch);
		} else if (ch == '!' || ch == '&') {
			// single-character operators
		} else if (ch == '|' || ch == '+' || ch == '-') {
			// one- or two-character operators that have same char repeat
			int ch2 = fInput.get_ch();
			
			if (ch2 == ch) {
				
			} else {
				fInput.unget_ch(ch2);
				// single-char
			}
		} else if (ch == '>' || ch == '<') {
			// one- two- or three-character operators that have same-char
			// repeat
			int ch2 = fInput.get_ch();
			
			if (ch2 == ch) {
				int ch3 = fInput.get_ch();
				if (ch2 == ch3) {
					
				} else {
					fInput.unget_ch(ch3);
				}
			} else {
				fInput.unget_ch(ch2);
			}
		} else if (ch == '=') {
			return new SVToken(Type.Equals, "=");
		} else if (ch == '~' || ch == '^') {
			// one- or two-character operators
		} else if (ch == '(') {
			int ch2 = fInput.get_ch();
			
			if (ch2 == '*') {
				// attribute begin
				
			} else {
				fInput.unget_ch(ch2);
				// single paren
			}
		} else if (ch == '*') {
			int ch2 = fInput.get_ch();
			
			if (ch2 == ')') { 
				// attribute end
			} else {
				fInput.unget_ch(ch2);
				// just '*'
			}
		} else if (isIdentifierStart(ch) || ch == '$') {
			fBuffer.append((char)ch);
			
			while ((ch = fInput.get_ch()) != -1 && isSimpleIdentifierPart(ch)) {
				fBuffer.append((char)ch);
			}
			fInput.unget_ch(ch);
			String image = fBuffer.toString();
			
			if (SVKeywords.isKeyword(image)) {
				ret = new SVToken(SVToken.Type.Keyword, fBuffer.toString());
			} else {
				ret = new SVToken(SVToken.Type.Id, fBuffer.toString());
			}
		}
			
		return ret;
	}
	
	private static boolean isIdentifierStart(int ch) {
		return ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ||
				ch == '_');
	}
	
	private static boolean isSimpleIdentifierPart(int ch) {
		return ((ch >= 'a' && ch <='z') || (ch >= 'A' && ch <= 'Z') ||
				ch == '$' || ch == '_' || (ch >= '0' && ch <= '9'));
	}
	
	private int skipCommentsWS(int ch) {
		do {
			while (ch != -1 && Character.isWhitespace(ch)) { 
				ch = fInput.get_ch();
			}

			if (ch == '/') {
				int ch2 = fInput.get_ch();
				if (ch2 == '/') {
					// scan forward to end-of-line
					while ((ch = fInput.get_ch()) != -1 && ch != '\n') { }
					if (ch != -1) {
						ch = ' '; // ensure that we go around the loop another time
					}
				} else if (ch2 == '*') {
					int match[] = {-1, -1};
					
					do {
						match[0] = match[1];
						match[1] = fInput.get_ch();
					} while (match[1] != -1 && 
							(match[0] != '*' || match[1] != '/'));
					if (ch != -1) {
						ch = ' '; // ensure we go 'round the look another time
					}
				} else {
					fInput.unget_ch(ch2);
				}
			}
		} while (Character.isWhitespace(ch));
		
		return ch;
	}

}
