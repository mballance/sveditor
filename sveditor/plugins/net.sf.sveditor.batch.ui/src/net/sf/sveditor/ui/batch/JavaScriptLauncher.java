/* 
 * Copyright (c) 2008-2020 Matthew Ballance and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 */
package net.sf.sveditor.ui.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.ui.console.MessageConsoleStream;

import net.sf.sveditor.core.ILineListener;
import net.sf.sveditor.core.SVFileUtils;
import net.sf.sveditor.core.Tuple;
import net.sf.sveditor.core.argfile.parser.SVArgFileLexer;
import net.sf.sveditor.core.batch.SVBatchPlugin;
import net.sf.sveditor.core.batch.jscript.JavaScriptRunner;
import net.sf.sveditor.core.scanutils.StringTextScanner;
import net.sf.sveditor.ui.SVUiPlugin;

public class JavaScriptLauncher implements ILaunchConfigurationDelegate {

	public void launch(
			ILaunchConfiguration 	configuration, 
			String 					mode,
			ILaunch 				launch, 
			IProgressMonitor 		monitor) throws CoreException {
		final MessageConsoleStream out = SVUiPlugin.getDefault().getStdoutStream();
		final MessageConsoleStream err = SVUiPlugin.getDefault().getStderrStream();
		String script = configuration.getAttribute(JavaScriptLauncherConstants.SCRIPT_LIST, "");
		String wd = configuration.getAttribute(JavaScriptLauncherConstants.WORKING_DIR, System.getProperty("user.dir"));
		String args_str = configuration.getAttribute(JavaScriptLauncherConstants.ARGUMENTS, "");
		
		SubMonitor subMonitor = SubMonitor.convert(monitor, "Running " + script, 1000);
		
		JavaScriptRunner runner = new JavaScriptRunner();
	
		runner.setOutLineListener(new ILineListener() {
			public void line(String l) {
				out.print(l);
			}
		});
		
		runner.setErrLineListener(new ILineListener() {
			public void line(String l) {
				err.print(l);
			}
		});
	
		InputStream in = null;

		File wd_f = SVFileUtils.getFile(wd);
		
		try {
			if (script.startsWith("${workspace_loc}")) {
				String path = script.substring("${workspace_loc}".length());
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				in = root.getFile(new Path(path)).getContents();
			} else {
				in = new FileInputStream(script);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		try {
			List<String> argv = parse_arguments(args_str);
			runner.run(new Tuple<String, InputStream>(script, in), argv, wd_f);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Clean and reset the state of the batch plugin
			SVBatchPlugin.getDefault().reset();
		}
		
		// Finally, refresh if needed
		IContainer f = SVFileUtils.findWorkspaceFolder(wd_f.getAbsolutePath());
		if (f != null && f.exists()) {
			f.refreshLocal(IResource.DEPTH_INFINITE, subMonitor.newChild(1));
		}
		subMonitor.done();
	}

	private List<String> parse_arguments(String args) {
		List<String> ret = new ArrayList<String>();
		SVArgFileLexer lexer = new SVArgFileLexer();
		lexer.init(null, new StringTextScanner(args));
	
		while (lexer.peek() != null) {
			ret.add(lexer.eatToken());
		}
		
		return ret;
	}
}
