package net.sf.sveditor.core.argfile.open_decl;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.argfile.content_assist.SVArgFileExprContext;
import net.sf.sveditor.core.argfile.content_assist.SVArgFileExprScanner;
import net.sf.sveditor.core.argfile.parser.SVArgFileDefaultOptionProvider;
import net.sf.sveditor.core.argfile.parser.ISVArgFileOptionProvider.OptionType;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;
import net.sf.sveditor.core.scanutils.IBIDITextScanner;

public class SVArgFileOpenDeclaration {

	public static String openDecl(IBIDITextScanner scanner) {
		SVArgFileDefaultOptionProvider option_provider = new SVArgFileDefaultOptionProvider();
		LogHandle log = LogFactory.getLogHandle("SVargFileOpenDeclaration");
	
		String ret = null;
		SVArgFileExprScanner expr_scanner = new SVArgFileExprScanner();
		SVArgFileExprContext ctxt = expr_scanner.extractExprContext(scanner, true);
		
		log.debug("ctxt: root=" + ctxt.fRoot + " leaf=" + ctxt.fLeaf);
	
		if (ctxt.fRoot != null) {
			OptionType type = option_provider.getOptionType(ctxt.fRoot);
			if (type == OptionType.ArgFileInc) {
				ret = ctxt.fLeaf;
			}
		} else {
			// likely file path
			ret = ctxt.fLeaf;
		}
		
		return ret;
	}
}
