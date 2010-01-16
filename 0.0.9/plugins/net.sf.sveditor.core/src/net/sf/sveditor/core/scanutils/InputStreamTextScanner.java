package net.sf.sveditor.core.scanutils;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamTextScanner extends AbstractTextScanner {
	private InputStream					fInput;
	private String						fFilename;
	private int							fUngetCh;
	private byte						fBuffer[];
	private int							fBufferIdx;
	private int							fBufferMax;
	
	public InputStreamTextScanner(InputStream in, String filename) {
		super();
		
		fInput    = in;
		fFilename = filename;
		fUngetCh  = -1;
		fBuffer   = new byte[1024*64]; // 64K buffer
		fBufferIdx = 0;
		fBufferMax = 0;
	}

	public ScanLocation getLocation() {
		return new ScanLocation(fFilename, fLineno, 0);
	}

	public int get_ch() {
		int ch = -1;
		
		if (fUngetCh != -1) {
			ch = fUngetCh;
			fUngetCh = -1;
			return ch;
		}

		if (fBufferIdx >= fBufferMax) {
			fBufferIdx = 0;
			fBufferMax = 0;
			try {
				fBufferMax = fInput.read(fBuffer, 0, fBuffer.length);
			} catch (IOException e) {}
		}
		
		if (fBufferIdx < fBufferMax) {
			ch = fBuffer[fBufferIdx++];
		}
		
		if (fLastCh == '\n') {
			fLineno++;
		}
		fLastCh = ch;
		
		return ch;
	}

	public void unget_ch(int ch) {
		fUngetCh = ch;
	}
}
