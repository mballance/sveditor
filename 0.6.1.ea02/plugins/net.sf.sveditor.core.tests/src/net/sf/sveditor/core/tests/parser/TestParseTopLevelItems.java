/****************************************************************************
 * Copyright (c) 2008-2011 Matthew Ballance and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Ballance - initial implementation
 ****************************************************************************/


package net.sf.sveditor.core.tests.parser;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.tests.SVDBTestUtils;
import junit.framework.TestCase;

public class TestParseTopLevelItems extends TestCase {

	public void testOvmPrinter() {
		SVCorePlugin.getDefault().enableDebug(false);
		String content =
				"function automatic int ovm_num_characters (ovm_radix_enum radix, ovm_bitstream_t value,\n" + 
				"		int size, string radix_str=\"\");\n" +
				"		int chars;\n" +
				"		int r;\n" +
				"		ovm_bitstream_t mask;\n" +
				"		if(radix==OVM_NORADIX)\n" +
				"			radix = OVM_HEX;\n" +
				"\n" +
				"		mask = {OVM_STREAMBITS{1'b1}};\n" +
				"		mask <<= size;\n" +
				"		mask = ~mask;\n" +
				"		value &= mask;\n" +
				"		if((^value) !== 1'bx) begin\n" +
    			"		case(radix)\n" +
    			"			OVM_BIN: r = 2;\n" +
    			"			OVM_OCT: r = 8;\n" +
    			"			OVM_UNSIGNED: r = 10;\n" +
    			"			OVM_DEC: r = 10;\n" +
    			"			OVM_HEX: r = 16;\n" +
    			"			OVM_TIME: r = 10;\n" +
    			"			OVM_STRING: return size/8;\n" +
      			"			default:  r = 16;\n" +
      			"		endcase\n" +
      			"		chars = radix_str.len() + 1;\n" +
      			"		if((radix == OVM_DEC) && (value[size-1] === 1)) begin\n" +
      			"			mask = ~mask;\n" +
      			"			value |= mask;\n" +
      			"			value = ~value + 1;\n" +
      			"			chars++;\n" +
      			"		end\n" +
      			"		for(ovm_bitstream_t i=r; value/i != 0; i*=r)\n" + 
      			"			chars++;\n" +
      			"		return chars;\n" +
      			"	end\n" +
      			"	else begin\n" +
      			"		string s;\n" +
      			"		s = ovm_vector_to_string(value, size, radix, radix_str);\n" +
      			"		return s.len();\n" +
      			"	end\n" +
      			"	endfunction\n"
      			;
		runTest("testOvmPrinter", content, new String[] {"ovm_num_characters"});
	}
	
	private void runTest(
			String			testname,
			String			doc,
			String			exp_items[]) {
		SVDBFile file = SVDBTestUtils.parse(doc, testname);
		
		SVDBTestUtils.assertNoErrWarn(file);
		SVDBTestUtils.assertFileHasElements(file, exp_items);
	}
	
}
