package net.sf.sveditor.core.parser;

import java.io.IOException;
import java.io.InputStream;

public class SVInputStream {
	private SystemVerilogParser 			fParser;
	private int								fLineno;
	private int								fLinepos;
	private InputStream						fInput;
	private StringBuffer					fUngetBuffer;
	
	public SVInputStream(InputStream in) {
		fInput = in;
		fUngetBuffer = new StringBuffer();
	}
	
	public void init(SystemVerilogParser p) {
		fParser = p;
	}
	
	public int get_ch() {
		int ch = -1;
		
		if (fUngetBuffer.length() > 0) {
			ch = fUngetBuffer.charAt(fUngetBuffer.length()-1);
			fUngetBuffer.setLength(fUngetBuffer.length()-1);
		} else {
			try {
				ch = fInput.read(); 
			} catch (IOException e) {
			}
		}
		
		return ch;
	}
	
	public void unget_ch(int ch) {
		if (ch != -1) {
			fUngetBuffer.append((char)ch);
		}
	}

}
