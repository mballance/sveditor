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


package net.sf.sveditor.core.argfile.content_assist;

import net.sf.sveditor.core.argfile.content_assist.SVArgFileExprContext.ArgFileContextType;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;
import net.sf.sveditor.core.scanner.SVCharacter;
import net.sf.sveditor.core.scanutils.IBIDITextScanner;

public class SVArgFileExprScanner {

	private boolean 						fDebugEn = true;
	private LogHandle						fLog;

	public SVArgFileExprScanner() {
		fLog = LogFactory.getLogHandle("SVArgFileExprScanner");
	}
	
	/**
	 * Extracts an expression surrounding the current scan position.
	 * 
	 * @param scanner
	 * @param leaf_scan_fwd	- Scan forward from the start point for Leaf. 
	 * @return
	 */
	public SVArgFileExprContext extractExprContext(
			IBIDITextScanner 		scanner,
			boolean					leaf_scan_fwd) {
		SVArgFileExprContext ret = new SVArgFileExprContext();
		debug("--> extractExprContext()");

		int c = -1;
		
		boolean scan_fwd = scanner.getScanFwd();
		scanner.setScanFwd(false);
		c = scanner.get_ch();
		debug("    First Ch (non-adjusted): \"" + (char)c + "\"");
		scanner.unget_ch(c);
		scanner.setScanFwd(scan_fwd);

		// We'll start by scanning backwards. On entry, the
		// cursor has been placed to read going forward
		if (scanner.getScanFwd() && scanner.getPos() > 0) {
			debug("    Scanning forward");

			long pos = scanner.getPos();

			// If the previous character is whitespace, then 
			// the cursor is likely positioned at the beginning
			// of the token
			// In this case, we want to begin processing at the
			// cursor position, not one previous
			scanner.seek(pos-1);
			int prev_ch = scanner.get_ch();
			
			if (Character.isWhitespace(prev_ch) || prev_ch == '"' || 
					(SVCharacter.isSVIdentifierPart(c) && 
							!SVCharacter.isSVIdentifierPart(prev_ch))) {
				scanner.seek(pos);
			} else {
				scanner.seek(pos-1);
			}
		}
		
		scanner.setScanFwd(false);
	
		c = scanner.get_ch();
		debug("    First Ch (adjusted): \"" + (char)c + "\"");
		scanner.unget_ch(c);

		// Check whether we're currently in a string
		if (isInString(scanner)) {
			debug("isInString()");
			// It's most likely that we're looking at an include
			
			ret.fType = ArgFileContextType.String;

			ret.fLeaf = readString(scanner, leaf_scan_fwd);
			
			long seek = scanner.getPos();
			scanner.setScanFwd(true);
			while ((c = scanner.get_ch()) != -1 && c != '"') {
			}
			
			if (c == '"') {
				ret.fStart = (int)scanner.getPos();
			} else {
				ret.fStart = (int)seek;
			}
			scanner.seek(seek);
			
			if (ret.fLeaf == null) {
				ret.fLeaf = "";
			}
			
			// Now, continue scanning backwards to determine how to
			// deal with this
			scanner.setScanFwd(false);
			c = scanner.skipWhite(scanner.get_ch());

			debug("string=\"" + ret.fLeaf + "\" next=\"" + (char)c + "\"");

			if (SVCharacter.isSVIdentifierPart(c)) {
				String id = new StringBuilder(scanner.readIdentifier(c)).reverse().toString();
				debug("id=\"" + id + "\"");
				
				c = scanner.skipWhite(scanner.get_ch());
				
				debug("next=\"" + (char)c + "\"");
				
				if (c == '`' && id.equals("include")) {
					ret.fTrigger = "`";
					ret.fRoot = "include";
				}
			}
		} else { // Not string
			if (isPathPart(c = scanner.get_ch())) {
				debug("notInString c=\"" + (char)c + "\"");
				scanner.unget_ch(c);
				String id = readPathOrOption(scanner, leaf_scan_fwd);
				ret.fStart = (int)scanner.getPos()+1; // compensate for begin in scan-backward mode
				ret.fLeaf = id;
				
				debug("id=\"" + id + "\"");
				
				// Now, see if an dash-option or plusarg option preceeds
				scanner.setScanFwd(false);
			
				c = scanner.get_ch();
				debug("c=" + (char)c);
				c = scanner.skipWhite(c);
				debug("post-ws c=" + (char)c);
				
				boolean is_plusarg = (c == '+');
			
				if (c == '+' || Character.isJavaIdentifierPart(c)) {
					ret.fRoot = readOption(scanner, c);
					
					debug("option root=" + ret.fRoot);
		
					if (is_plusarg) {
						if (!ret.fRoot.startsWith("+")) {
							// Not really a plusarg
							ret.fRoot = null;
						}
					} else if (!ret.fRoot.startsWith("-")) {
						// Not really an option
						ret.fRoot = null;
					}
				}

				/*
				// See if we're working with a triggered expression
				ret.fTrigger = readTriggerStr(scanner, true);
				debug("trigger=\"" + ret.fTrigger + "\"");
				
				if (ret.fTrigger != null && !ret.fTrigger.equals("`")) {
					// Read an expression
					ret.fType = ArgFileContextType.Triggered;
					ret.fRoot = readExpression(scanner);
					
					if (ret.fRoot != null && ret.fRoot.trim().equals("")) {
						ret.fRoot = null;
					}
				} else if (ret.fTrigger == null) {
					ret.fType = ArgFileContextType.Untriggered;
						
					// Just process the identifier
					c = scanner.skipWhite(scanner.get_ch());
					
					if (c == '=') {
						int c2 = scanner.get_ch();
						if (c2 != '=' && c2 != '>' &&
								c2 != '<' && c2 != '&' &&
								c2 != '|' && c2 != '+' &&
								c2 != '-') {
							c = scanner.skipWhite(c2);
							ret.fTrigger = "=";
						}
					}
					
					if (SVCharacter.isSVIdentifierPart(c)) {
						scanner.unget_ch(c);
						ret.fRoot = readIdentifier(scanner, false);
					}
				}
				 */
			} else {
				// backup and try for a triggered identifier
				debug("notInId: ch=\"" + (char)c + "\"");
				
				scanner.unget_ch(c);
				
				ret.fStart = (int)scanner.getPos()+1; // compensate for begin in scan-backward mode
				
				if ((ret.fTrigger = readTriggerStr(scanner, true)) != null) {
					ret.fType = ArgFileContextType.Triggered;
					
					if (scan_fwd) {
						scanner.setScanFwd(true);
						c = scanner.get_ch();
						fLog.debug("post-trigger c=\"" + (char)c + "\"");
						ret.fLeaf = readIdentifier(scanner, true);
						
						// Now, back up to ensure that we get the pre-trigger portion
						scanner.setScanFwd(false);
						c = scanner.get_ch();
						fLog.debug("post-leaf c=\"" + (char)c + "\"");
					} else {
						ret.fLeaf = "";
					}
					ret.fRoot = readExpression(scanner);
				}
			}
		}
		
		/*
		if (ret.fType != ArgFileContextType.String) {
			if (ret.fRoot != null && ret.fRoot.equals("import")) {
				ret.fType = ArgFileContextType.Import;
			} else {
				// Read preceeding token. It's possible we need to change this type
				c = scanner.skipWhite(scanner.get_ch());

				if (SVCharacter.isSVIdentifierPart(c)) {
					scanner.unget_ch(c);
					String tmp = readIdentifier(scanner, false);
					
					fLog.debug("preceeding token: " + tmp.toString());
						
					if (tmp.equals("import")) {
						ret.fType = ArgFileContextType.Import;
					} else if (tmp.equals("extends")) {
						ret.fType = ArgFileContextType.Extends;
					}
				}
			}
		}
		 */
		
		debug("<-- extractExprContext()");
		
		if (ret.fRoot != null && ret.fRoot.trim().equals("")) {
			ret.fRoot = null;
		}
		
		if (ret.fRoot == null && ret.fTrigger == null && ret.fLeaf == null) {
			ret.fLeaf = "";
		}
		
		return ret;
	}
	
	private boolean isPathPart(int ch) {
		return (ch == '/' || SVCharacter.isSVIdentifierPart(ch) || 
				ch == '{' || ch == '}');
	}

	private boolean isInString(IBIDITextScanner scanner) {
		boolean ret = false;
		long sav_pos = scanner.getPos();
		boolean scan_fwd = scanner.getScanFwd();
		int ch;
		
		// Continue scanning backwards
		scanner.setScanFwd(false);
		while ((ch = scanner.get_ch()) != -1 && 
				ch != '"' && ch != '\n') {
		}
		
		if (ch == '"') {
			ret = true;
			
			// Just to be sure, continue scanning backwards to
			// be sure we don't find another matching quite
			while ((ch = scanner.get_ch()) != -1 &&
					ch != '"' && ch != '\n') { }
			
			if (ch == '"') {
				ret = false;
			}
		}
		
		scanner.seek(sav_pos);
		scanner.setScanFwd(scan_fwd);
		return ret;
	}
	
	private String readExpression(IBIDITextScanner scanner) {
		int ch;
		String trigger = null;
		
		fLog.debug("--> readExpression");
		// Continue moving backwards
		scanner.setScanFwd(false);
		
		ch = scanner.skipWhite(scanner.get_ch());
		scanner.unget_ch(ch);
		long end_pos = scanner.getPos(), start_pos;
		
		do {
			ch = scanner.skipWhite(scanner.get_ch());
			fLog.debug("    trigger=\"" + trigger + "\" ch=\"" + (char)ch + "\"");
			
			if (ch == ')') {
				scanner.skipPastMatch(")(");
				// Could be a function
				fLog.debug("    post ')(' char is: " + (char)ch);
				ch = scanner.skipWhite(scanner.get_ch());
				if (SVCharacter.isSVIdentifierPart(ch)) {
					scanner.readIdentifier(ch);
				} else {
					scanner.unget_ch(ch);
				}
			} else if (ch == ']') {
				// Skip what's in an array reference
				ch = scanner.skipPastMatch("][");
				ch = scanner.skipWhite(scanner.get_ch());
				if (SVCharacter.isSVIdentifierPart(ch)) {
					scanner.readIdentifier(ch);
				} else {
					scanner.unget_ch(ch);
				}
			} else if (SVCharacter.isSVIdentifierPart(ch)) {
				scanner.readIdentifier(ch);
			} else {
				fLog.debug("end readExpression: unknown ch \"" + (char)ch + "\"");
				start_pos = (scanner.getPos()+2);
				break;
			}
			start_pos = (scanner.getPos()+1);
		} while ((trigger = readTriggerStr(scanner, false)) != null);
		
		fLog.debug("<-- readExpression");
		
		return scanner.get_str(start_pos, (int)(end_pos-start_pos+1)).trim();
	}

	/**
	 * 
	 * @param scanner
	 * @return
	 */
	private String readTriggerStr(IBIDITextScanner scanner, boolean allow_colon) {
		long start_pos = scanner.getPos();
		scanner.setScanFwd(false);
		int ch = scanner.skipWhite(scanner.get_ch());
		
		if (ch == '.' || ch == '`') {
			return "" + (char)ch;
		} else if (ch == ':') {
			int ch2 = scanner.get_ch();
			
			if (ch2 == ':') {
				return "::";
			} else if (allow_colon) {
				return ":";
			}
		}
		
		// If we didn't identify a trigger, then restore the
		// previous position
		scanner.seek(start_pos);
		return null;
	}

	/**
	 * Read a string surrounding the current position. The scanner will
	 * be left at the beginning of the string.
	 * 
	 * @param scanner
	 * @param scan_fwd
	 * @return
	 */
	private String readString(IBIDITextScanner scanner, boolean scan_fwd) {
		int ch;
		
		long end_pos = scanner.getPos();
		long start_pos = -1, seek;
		
		// First, scan back to the string beginning
		scanner.setScanFwd(false);
		while ((ch = scanner.get_ch()) != -1 && 
				ch != '\n' && ch != '"') {
			debug("readString: ch=\"" + (char)ch + "\"");
		}
		
		start_pos = scanner.getPos();
		
		if (ch == '"') {
			seek = start_pos-1;
			start_pos += 2;
		} else {
			seek = start_pos;
		}
		
		if (scan_fwd) {
			scanner.setScanFwd(true);
			scanner.seek(start_pos);
			
			while ((ch = scanner.get_ch()) != -1 &&
					ch != '"' && ch != '\n') { 
			}
			
			end_pos = (scanner.getPos()-1);
			if (ch == '"') {
				end_pos--;
			}
		}
		
		scanner.seek(seek);
		
		if (start_pos >= 0 && (end_pos-start_pos) > 0) {
			return scanner.get_str(start_pos, (int)(end_pos-start_pos+1));
		} else {
			return "";
		}
	}

	/**
	 * readIdentifier()
	 * 
	 * Reads the identifier surrounding the current location. 
	 * 
	 * @param scanner
	 * @param scan_fwd
	 * @return
	 */
	private String readIdentifier(IBIDITextScanner scanner, boolean scan_fwd) {
		int ch;
		fLog.debug("--> readIdentifier(scan_fwd=" + scan_fwd + ")");
		
		long end_pos = (scanner.getScanFwd())?scanner.getPos():(scanner.getPos()+1);
		long start_pos = -1, seek;
		
		// First, scan back to the string beginning
		scanner.setScanFwd(false);
		while ((ch = scanner.get_ch()) != -1 &&
				SVCharacter.isSVIdentifierPart(ch)) { }
		
		start_pos = scanner.getPos() + 2;
		seek = scanner.getPos() + 1;
		
		if (scan_fwd) {
			scanner.setScanFwd(true);
			scanner.seek(start_pos);
			
			while ((ch = scanner.get_ch()) != -1 &&
					SVCharacter.isSVIdentifierPart(ch)) { }
			
			end_pos = scanner.getPos() - 1;
		}
		
		scanner.seek(seek);

		fLog.debug("<-- readIdentifier(scan_fwd=" + scan_fwd + ")");
		return scanner.get_str(start_pos, (int)(end_pos-start_pos));
	}

	private String readPathOrOption(IBIDITextScanner scanner, boolean scan_fwd) {
		int ch;
		fLog.debug("--> readPathOrIdentifier(scan_fwd=" + scan_fwd + ")");
		
		long end_pos = (scanner.getScanFwd())?scanner.getPos():(scanner.getPos()+1);
		long start_pos = -1, seek;
		long first_plus_pos = -1;
		int first_ch = -1;
		
		// First, scan back to the path or identifier beginning
		scanner.setScanFwd(false);
		// The path ends when we reach whitespace
		while ((ch = scanner.get_ch()) != -1 && !Character.isWhitespace(ch)) { 
			fLog.debug("ch=" + (char)ch);
			if (ch == '+' && first_plus_pos == -1) {
				first_plus_pos = scanner.getPos();
			}
			first_ch = ch;
		}

		start_pos = scanner.getPos() + 2;
		seek = scanner.getPos() + 1;
		
		fLog.debug("first_ch=" + (char)first_ch + " first_plus_pos=" + first_plus_pos + " seek=" + seek);
		
		if (first_ch == '+') {
			if (first_plus_pos != -1 && first_plus_pos != seek) {
				// example: +incdir+/my/path
				//                       ^
				start_pos = first_plus_pos + 2;
				seek = first_plus_pos+1;
				fLog.debug("Change start_pos to " + start_pos);
			} else {
				// example: +incdir+
				//              ^
				
			}
		}
		
		if (scan_fwd) {
			scanner.setScanFwd(true);
			scanner.seek(start_pos);
			
			while ((ch = scanner.get_ch()) != -1 && !Character.isWhitespace(ch)) {
				
			}
			
			end_pos = scanner.getPos() - 1;
		}
		
		scanner.seek(seek);

		fLog.debug("<-- readIdentifier(scan_fwd=" + scan_fwd + ")");
		return scanner.get_str(start_pos, (int)(end_pos-start_pos));
	}

	private String readOption(IBIDITextScanner scanner, int c) {
		boolean is_plusarg = (c == '+');
		long start_pos = scanner.getPos()+2;
		long end_pos;
	
		scanner.setScanFwd(false);
		while ((c = scanner.get_ch()) != -1) {
			if ((is_plusarg && c == '+') || 
				c == '-' || Character.isWhitespace(c)) {
				// Unget the last character to ensure that it
				// is included in the final option
				if (!Character.isWhitespace(c)) {
					scanner.unget_ch(c);
				}
				break;
			}
		}

		fLog.debug("scanner.getPos=" + scanner.getPos() + " c=" + (char)c);
		end_pos = (scanner.getPos() < 0)?0:scanner.getPos();
		
		fLog.debug("end_pos=" + end_pos);
		return scanner.get_str(end_pos, (int)(start_pos-end_pos));
	}
	
	private void debug(String msg) {
		if (fDebugEn) {
			fLog.debug(msg);
		}
	}
}
