package net.sf.sveditor.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

public class StringInputStream extends InputStream {
	private StringReader			fReader;
	private int						fLastC;
	
	public StringInputStream(String content) {
		fReader = new StringReader(content);
		fLastC = 0;
	}

	@Override
	public int read() throws IOException {
		int ret = -1;
		try {
			ret = fReader.read();
		} catch (IOException e) { }
		
		fLastC = ret;
		
		return ret;
	}
	
	public void close() throws IOException {
		fReader.close();
	}
	
	public synchronized void mark(int readLimit) {
		try {
			fReader.mark(readLimit);
		} catch (IOException e) {
		}
	}
	
	public boolean markSupported() {
		return fReader.markSupported();
	}

	public synchronized void reset() throws IOException {
		fReader.reset();
	}

	@Override
	public int available() throws IOException {
		return (fLastC != -1)?1:0;
	}
	
}
