package net.sf.sveditor.ui;

import java.net.URI;

import net.sf.sveditor.core.db.index.plugin_lib.PluginFileStore;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public class PluginFileEditorInputFactory implements IElementFactory {
	
	public static final String		ID = "net.sf.sveditor.ui.PluginFileEditorInputFactory";
	
	static void saveState(IMemento memento, PluginPathEditorInput input) {
		memento.putString("plugin_path", input.getURI().toString());
	}

	public IAdaptable createElement(IMemento memento) {
		String plugin_path = memento.getString("plugin_path");
		System.out.println("createElement: " + plugin_path);

		if (plugin_path == null) {
			return null;
		}

		URI uri = null;
		
		try {
			uri = new URI(plugin_path);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		IFileSystem fs = null;
		IFileStore store = null;
		try {
			fs = EFS.getFileSystem("plugin");
			store = fs.getStore(uri);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			return new PluginPathEditorInput((PluginFileStore)store);
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}
}
