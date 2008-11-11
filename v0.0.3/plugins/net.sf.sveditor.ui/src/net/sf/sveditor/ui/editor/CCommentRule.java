package net.sf.sveditor.ui.editor;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

public class CCommentRule implements IPredicateRule {
	private IToken			fToken;
	private boolean         fInComment;
	
	public CCommentRule(IToken tok) {
		fToken = tok;
		fInComment = false;
	}

	public IToken evaluate(ICharacterScanner scanner, boolean resume) {
		boolean in_comment = resume;
		
		if (!resume) {
			if (scanner.read() == '/') {
				if (scanner.read() == '*') {
					in_comment = true;
				}
				scanner.unread();
			} else {
				scanner.unread();
			}
		}
		
		if (in_comment) {
			scanToEnd(scanner);
			return fToken;
		}
		
		return Token.UNDEFINED;
	}
	
	private void scanToEnd(ICharacterScanner scanner) {
		int ch_a[] = {-1, -1};
		
		int ch;
		while ((ch = scanner.read()) != ICharacterScanner.EOF) {
			ch_a[0] = ch_a[1];
			ch_a[1] = ch;
			
			if (ch_a[0] == '*' && ch_a[1] == '/') {
				break;
			}
		}
	}
	
	public IToken evaluate(ICharacterScanner scanner) {
		return evaluate(scanner, false);
	}

	public IToken getSuccessToken() {
		return fToken;
	}


}
