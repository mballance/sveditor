package net.sf.sveditor.core.templates;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TemplateFSOutStreamProvider implements ITemplateOutStreamProvider {
	
	private File						fRoot;
	
	public TemplateFSOutStreamProvider(File root) {
		fRoot = root;
	}

	public OutputStream openStream(String path) {
		File target = new File(fRoot, path);
		try {
			return new FileOutputStream(target);
		} catch (IOException e) {}

		return null;
	}

	public void closeStream(OutputStream out) {
		try {
			out.close();
		} catch (IOException e) {}
	}

}
