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


package net.sf.sveditor.ui.tests.editor;

import junit.framework.TestCase;
import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.tests.indent.IndentComparator;
import net.sf.sveditor.ui.tests.UiReleaseTests;
import net.sf.sveditor.ui.tests.editor.utils.AutoEditTester;

import org.eclipse.jface.text.BadLocationException;

public class TestAutoIndent extends TestCase {
	
	public void testBasicIndent() throws BadLocationException {
		AutoEditTester tester = UiReleaseTests.createAutoEditTester();
		
		tester.type("\n\n");
		tester.type("class foobar;\n");
		tester.type("\nfunction void foobar();\n");
		tester.type("a = 5;\n");
		tester.type("endfunction\n\n");
		tester.type("endclass\n");
		
		String content = tester.getContent();
		
		String expected = 
			"\n\n" +
			"class foobar;\n" +
			"\t\n" +
			"\tfunction void foobar();\n" +
			"\t\ta = 5;\n" +
			"\tendfunction\n" +
			"\t\n" +
			"endclass\n";
		
		System.out.println("Result:\n" + content);
		IndentComparator.compare("testBasicIndent", expected, content);
	}
	
	
	public void testAutoIndentAlways() throws BadLocationException {
		AutoEditTester tester = UiReleaseTests.createAutoEditTester();
		String content = 
			"module foo;\n" +
			"always @(posedge clk)\n" +
			"if (~rst_n_clk) bus_release_cnt <= 'b0;\n" +
			"else if (slow_packet_finished) bus_release_cnt <= bus_release_cnt + 1'b1;\n" +
			"else if (|bus_release_cnt) bus_release_cnt <= bus_release_cnt + 1'b1;\n" +
			"else if"
			;

		tester.type(content);
		
		String result = tester.getContent();
		
		String expected = 
			"module foo;\n" +
			"	always @(posedge clk)\n" +
			"		if (~rst_n_clk) bus_release_cnt <= 'b0;\n" +
			"		else if (slow_packet_finished) bus_release_cnt <= bus_release_cnt + 1'b1;\n" +
			"		else if (|bus_release_cnt) bus_release_cnt <= bus_release_cnt + 1'b1;\n" +
			"		else if"
			;
		
		System.out.println("Result:\n" + result);
		IndentComparator.compare("testBasicIndent", expected, result);
	}
	
	public void testAutoPostSingleComment() throws BadLocationException {
		String content =
			"class foo;\n" +
			"function void my_func();\n" +
			"if (foobar) begin\n" +
			"end else begin\n" +
			"// comment block\n" +
			"a.b = 5;\n" +
			"end\n" +
			"endfunction\n" +
			"endclass\n"
			;
		String expected =
			"class foo;\n" +
			"	function void my_func();\n" +
			"		if (foobar) begin\n" +
			"		end else begin\n" +
			"			// comment block\n" +
			"			a.b = 5;\n" +
			"		end\n" +
			"	endfunction\n" +
			"endclass\n" 
			;

		AutoEditTester tester = UiReleaseTests.createAutoEditTester();
		tester.type(content);
		
		String result = tester.getContent();
		
		System.out.println("Result:\n" + result);
		IndentComparator.compare("testBasicIndent", expected, result);
	}

	public void testPaste() throws BadLocationException {
		String first =
			"`ifndef INCLUDED_transport_packet_svh\n" +
			"`define INCLUDED_transport_packet_svh\n" +
			"\n";
		
		String text = 
			"class vmm_xactor;\n" +
			"\n" +
			"`VMM_NOTIFY notify;\n";
		
		String expected = 
			"`ifndef INCLUDED_transport_packet_svh\n" +
			"`define INCLUDED_transport_packet_svh\n" +
			"\n" +
			"class vmm_xactor;\n" +
			"\n" +
			"	`VMM_NOTIFY notify;\n"
			;
			
		
		AutoEditTester tester = UiReleaseTests.createAutoEditTester();
		tester.type(first);
		tester.paste(text);
		
		String content = tester.getContent();
		
		System.out.println("Result:\n" + content);
		IndentComparator.compare("testPaste", expected, content);
	}

	public void testPasteModule() throws BadLocationException {
		String first =
			"module t();\n" +
			"	logic a;\n" +
			"endmodule\n";

		String text =
			"\n" +
			"	logic a;\n"
			;
		
		AutoEditTester tester = UiReleaseTests.createAutoEditTester();
		tester.type(first);
		tester.setCaretOffset(first.length());
		tester.paste(text);
		
		String content = tester.getContent();
		
		System.out.println("content=\"" + content + "\"");
		
	}

	public void testModuleWires() throws BadLocationException {
		String content =
			"module t();\n" +
			"logic a;\n" +
			"logic b;\n" +
			"endmodule\n";

		String expected =
			"module t();\n" +
			"	logic a;\n" +
			"	logic b;\n" +
			"endmodule\n";
		
		AutoEditTester tester = UiReleaseTests.createAutoEditTester();
		tester.type(content);
		
		String result = tester.getContent();

		System.out.println("Result:\n" + result);
		IndentComparator.compare("testModuleWires", expected, result);
	}

	public void testModuleWiresPastePost() throws BadLocationException {
		String content =
			"module t();\n" +
			"logic a;\n" +
			"logic b;\n" +
			"endmodule\n";
		
		String expected =
			"module t();\n" +
			"	logic a;\n" +
			"	logic b;\n" +
			"endmodule\n" +
			"module b();\n" +
			"	logic a;\n" +
			"	logic b;\n" +
			"endmodule\n";
		
		AutoEditTester tester = UiReleaseTests.createAutoEditTester();
		tester.type(content);
		tester.paste(
				"module b();\n" +
				"logic a;\n" +
				"logic b;\n" +
				"endmodule\n");

		
		String result = tester.getContent();

		System.out.println("Result:\n" + result);
		IndentComparator.compare("testModuleWires", expected, result);
	}

	public void testPasteInModule() throws BadLocationException {
		
		SVCorePlugin.getDefault().enableDebug(false);
		
		String first =
			"module t();\n" +
			"	logic a;\n" +
			"endmodule\n";

		String text =
			"logic b;\n"
			;
		
		String expected = 
			"module t();\n" +
			"	logic a;\n" +
			"	logic b;\n" +
			"endmodule\n"
			;
		
		AutoEditTester tester = UiReleaseTests.createAutoEditTester();
		tester.setContent(first);
//		tester.type(first);
		tester.setCaretOffset(
				("module t();\n" +
				 "	logic a;\n").length());
		
		tester.paste(text);
		
		String content = tester.getContent();
		
		System.out.println("Result:\n" + content);
		IndentComparator.compare("", expected, content);
	}

	public void testAutoIndentIfThenElse() throws BadLocationException {
		
		SVCorePlugin.getDefault().enableDebug(false);
		
		String content = 
			"module t();\n" +
			"if (foo)\n" +
			"a = 5;\n" +
			"else if (bar)\n" +
			"b = 6;\n" +
			"\n" +
			"if (foo) begin\n" +
			"a = 5;\n" +
			"end else if (bar) begin\n" +
			"b = 6;\n" +
			"end\n" +
			"\n" +
			"if (foo)\n" +
			"begin\n" +
			"a = 5;\n" +
			"end\n" +
			"else\n" +
			"begin\n" +
			"b = 6;\n" +
			"end\n" +
			"endmodule\n"
			;
		String expected = 
			"module t();\n" +
			"	if (foo)\n" +
			"		a = 5;\n" +
			"	else if (bar)\n" +
			"		b = 6;\n" +
			"\n" +
			"	if (foo) begin\n" +
			"		a = 5;\n" +
			"	end else if (bar) begin\n" +
			"		b = 6;\n" +
			"	end\n" +
			"\n" +
			"	if (foo)\n" +
			"		begin\n" +
			"			a = 5;\n" +
			"		end\n" +
			"	else\n" +
			"		begin\n" +
			"			b = 6;\n" +
			"		end\n" +
			"endmodule\n"
			;
		
		AutoEditTester tester = UiReleaseTests.createAutoEditTester();
		tester.type(content);
		
		String result = tester.getContent();
		
		System.out.println("Result:\n" + content);
		IndentComparator.compare("", expected, result);
	}

	
	public void testCovergroup() throws BadLocationException {
		String input = 
			"class foobar;\n\n" +
			"covergroup foobar;\n\n" +
			"var_cp : coverpoint (var) iff (var_cond);\n\n" +
			"var2_cp : coverpoint (var) iff (var_cond) {\n" +
			"bins subrange1[] = {[0:3]};\n" +
			"bins subrange2[] = {\n" +
			"[0:3],\n" +
			"[4:7]\n" +
			"};\n" +
			"}\n" +
			"endgroup\n";
		String expected =
			"class foobar;\n" +
			"\t\n" +
			"\tcovergroup foobar;\n" +
			"\t\t\n" +
			"\t\tvar_cp : coverpoint (var) iff (var_cond);\n" +
			"\t\t\n" +
			"\t\tvar2_cp : coverpoint (var) iff (var_cond) {\n" +
			"\t\t\tbins subrange1[] = {[0:3]};\n" +
			"\t\t\tbins subrange2[] = {\n" +
			"\t\t\t\t[0:3],\n" +
			"\t\t\t\t[4:7]\n" +
			"\t\t\t};\n" +
			"\t\t}\n" +
			"\tendgroup\n" +
			"\t";
		
		AutoEditTester tester = UiReleaseTests.createAutoEditTester();
		tester.type(input);
		String result = tester.getContent();
		

		System.out.println("Result:\n" + result);
		IndentComparator.compare("Covergroup indent failed", expected, result);
	}

	public void testVirtualFunction() throws BadLocationException {
		String input1 = 
			"class foobar;\n\n" +
			"function new();\n" +
			"endfunction\n" +
			"\n" +
			"virtual function string foo();";
		String input2 = "\n" +
			"a = 5;\n" +
			"endfunction\n";
		String expected =
			"class foobar;\n" +
			"	\n" +
			"	function new();\n" +
			"	endfunction\n" +
			"\n" +
			"	virtual function string foo();\n" +
			"		a = 5;\n" +
			"	endfunction\n" +
			"";
		
		
		AutoEditTester tester = UiReleaseTests.createAutoEditTester();
		tester.type(input1);
		// SVCorePlugin.getDefault().enableDebug(false);
		tester.type(input2);
		String result = tester.getContent();
		
		
		System.out.println("Result:\n" + result);
		IndentComparator.compare("testVirtualFunction", expected, result);
	}

	public void testPastePostStringAdaptiveIndent() throws BadLocationException {
		AutoEditTester tester = UiReleaseTests.createAutoEditTester();
		String content = 
			"class foobar;\n" +
			"\n" +
			"function void foo2();\n" +
			"	$psprintf(\"Hello World\\n    Testing %d\\n\",\n" +
			"		a, b, c);\n\n";
		String expected =
			"class foobar;\n" +
			"\n" +
			"function void foo2();\n" +
			"	$psprintf(\"Hello World\\n    Testing %d\\n\",\n" +
			"		a, b, c);\n" +
			"" +
			"	if (foobar) begin\n" +
			"		a = 6;\n" +
			"	end\n";
		tester.setContent(content);
		//SVCorePlugin.getDefault().enableDebug(false);
		tester.paste(
				"if (foobar) begin\n" +
				"a = 6;\n" +
				"end\n");
		
		String result = tester.getContent();
		
		System.out.println("Result:\n" + result);
		IndentComparator.compare("testPastePostStringAdaptiveIndent", expected, result);
	}

	public void testPasteAdaptiveIndent() throws BadLocationException {
		AutoEditTester tester = UiReleaseTests.createAutoEditTester();
		String content = 
			"class foobar;\n" +
			"\n" +
			"function void foo2();\n\n";
		String expected =
			"class foobar;\n" +
			"\n" +
			"function void foo2();\n" +
			"	if (foobar) begin\n" +
			"		a = 6;\n" +
			"	end\n";
		
		tester.setContent(content);
		tester.paste(
				"if (foobar) begin\n" +
				"a = 6;\n" +
				"end\n");
		
		String result = tester.getContent();
		
		System.out.println("Result:\n" + result);
		IndentComparator.compare("testPasteAdaptiveIndent", expected, result);
	}
	
	public void testPasteInsertOpeningComment() throws BadLocationException {
		String input = 
			"class foo;\n" +
			"\n" +
			"	function void foobar;\n" +
			"		int var;\n" +
			"		var = 5;\n" +
			"		bar = 6;\n" +
			"		*/\n" +
			"	endfunction\n" +
			"\n" +
			"endclass\n";
		String expected =
			"class foo;\n" +
			"\n" +
			"	function void foobar;\n" +
			"		int var;\n" +
			"/*\n" +
			"		var = 5;\n" +
			"		bar = 6;\n" +
			"		*/\n" +
			"	endfunction\n" +
			"\n" +
			"endclass\n";
			
		AutoEditTester tester = UiReleaseTests.createAutoEditTester();
		tester.setContent(input);
		
		tester.setCaretOffset(0);
		while (true) {
			String line = tester.readLine();
			System.out.println("line=\"" + line + "\"");
			
			if (line.trim().startsWith("int var")) {
				break;
			}
		}
		tester.paste("/*\n");
		
		String result = tester.getContent();
		System.out.println("Result:\n" + result);
		IndentComparator.compare("testPasteInsertOpeningComment", expected, result);
	}

	public void disabled_testCaseStatement() throws BadLocationException {
		String input = 
			"class foobar;\n" +
			"\n" +
			"function void foobar();\n" +
			"case" +
			"covergroup foobar;\n\n" +
			"var_cp : coverpoint (var) iff (var_cond);\n\n" +
			"var2_cp : coverpoint (var) iff (var_cond) {\n" +
			"bins subrange1[] = {[0:3]};\n" +
			"bins subrange2[] = {\n" +
			"[0:3],\n" +
			"[4:7]\n" +
			"};\n" +
			"}\n" +
			"endgroup\n";
		String expected =
			"class foobar;\n" +
			"\t\n" +
			"\tcovergroup foobar;\n" +
			"\t\t\n" +
			"\t\tvar_cp : coverpoint (var) iff (var_cond);\n" +
			"\t\t\n" +
			"\t\tvar2_cp : coverpoint (var) iff (var_cond) {\n" +
			"\t\t\tbins subrange1[] = {[0:3]};\n" +
			"\t\t\tbins subrange2[] = {\n" +
			"\t\t\t\t[0:3],\n" +
			"\t\t\t\t[4:7]\n" +
			"\t\t\t};\n" +
			"\t\t}\n" +
			"\tendgroup\n" +
			"\t";
		
		AutoEditTester tester = UiReleaseTests.createAutoEditTester();
		tester.type(input);
		String result = tester.getContent();

		IndentComparator.compare("", expected, result);
	}

	public void disabled_testModifyIndent() throws BadLocationException {
		int offset1, offset2;
		AutoEditTester tester = UiReleaseTests.createAutoEditTester();
		
		tester.type("\n\n");
		tester.type("class foobar;\n");
		tester.type("\n");
		offset1 = tester.getCaretOffset();
		tester.type("function void foobar();\n");
		offset2 = tester.getCaretOffset();
		tester.setCaretOffset(offset1);
		tester.type("\n\n");
		tester.setCaretOffset(offset2+3);
		System.out.println("char @ " + (offset2+2) + " = \"" + tester.getChar() + "\"");
		tester.type("a = 5;\n");
		tester.type("endfunction\n\n");
		tester.type("endclass\n");
		
		String content = tester.getContent();
		
		String expected = 
			"\n\n" +
			"class foobar;\n" +
			"\t\n" +
			"\tfunction void foobar();\n" +
			"\t\ta = 5;\n" +
			"\tendfunction\n" +
			"\t\n" +
			"endclass\n";

		System.out.println("content=\n" + content);

		assertEquals("Check for expected indent", expected, content);
		
	}
	
}
