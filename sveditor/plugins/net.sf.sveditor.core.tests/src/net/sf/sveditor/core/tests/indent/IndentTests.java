/****************************************************************************
 * Copyright (c) 2008-2014 Matthew Ballance and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Ballance - initial implementation
 ****************************************************************************/


package net.sf.sveditor.core.tests.indent;

import java.io.ByteArrayOutputStream;

import junit.framework.Test;
import junit.framework.TestSuite;
import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.indent.ISVIndenter;
import net.sf.sveditor.core.indent.SVIndentScanner;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;
import net.sf.sveditor.core.scanutils.StringTextScanner;
import net.sf.sveditor.core.tests.SVCoreTestCaseBase;
import net.sf.sveditor.core.tests.SVCoreTestsPlugin;
import net.sf.sveditor.core.tests.utils.BundleUtils;

public class IndentTests extends SVCoreTestCaseBase {
	
	public static Test suite() {
		TestSuite suite = new TestSuite("IndentTests");
		suite.addTest(new TestSuite(IndentTests.class));
		suite.addTest(new TestSuite(TestIndentAssertions.class));
		suite.addTest(new TestSuite(TestIndentBehavioralStmts.class));
		suite.addTest(new TestSuite(TestIndentConstraints.class));
		suite.addTest(new TestSuite(TestIndentCovergroup.class));
		suite.addTest(new TestSuite(NoHangIndentTests.class));
		suite.addTest(new TestSuite(TestIndentScanner.class));
		suite.addTest(new TestSuite(TestAdaptiveIndent.class));
		
		return suite;
	}
	
	public void testClass() {
		SVCorePlugin.getDefault().enableDebug(false);
		BundleUtils utils = new BundleUtils(SVCoreTestsPlugin.getDefault().getBundle());
		ByteArrayOutputStream bos;
		
		bos = utils.readBundleFile("/indent/class1.svh");
		
		String ref = bos.toString();
		StringBuilder sb = removeLeadingWS(ref);
		
		// Run the indenter over the reference source
		SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(sb));
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		StringBuilder result = new StringBuilder(indenter.indent(-1, -1));
		
		fLog.debug("Reference:\n" + ref);
		
		fLog.debug("===");
		fLog.debug("Result:\n" + result.toString());
		
		IndentComparator.compare("testClass", ref, result.toString());
	}
	
	public void testBasicCoverpoint() {
		SVCorePlugin.getDefault().enableDebug(false);
		String content =
			"class c1;\n" +
			"\n" +
			"covergroup cg1;\n" +
			"cp : coverpoint a {\n" +
			"bins ab[] = {1, 2, 3, 4};\n" +
			"}\n" +
			"endgroup\n" +
			"endclass\n"
			;
		String expected =
			"class c1;\n" +
			"\n" +
			"	covergroup cg1;\n" +
			"		cp : coverpoint a {\n" +
			"			bins ab[] = {1, 2, 3, 4};\n" +
			"		}\n" +
			"	endgroup\n" +
			"endclass\n"
			;
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		fLog.debug("Result:");
		fLog.debug(result);
		IndentComparator.compare(getName(), expected, result);
	}	

	public void testBasicClass() {
		LogHandle log = LogFactory.getLogHandle("testBasicClass");
		String content =
			"\n" +
			"class class1 #(type T=int);\n" +
			"\n" +
			"virtual interface some_if the_if;\n" +
			"\n" +
			"function new();\n" +
			"if (foo)\n" +
			"foo = 5;\n" +
			"else if (foo2) begin\n" +
			"foo = 6;\n" +
			"end\n" +
			"endfunction\n" +
			"endclass\n"
			;
		String expected =
			"\n" +
			"class class1 #(type T=int);\n" +
			"\n" +
			"	virtual interface some_if the_if;\n" +
			"\n" +
			"	function new();\n" +
			"		if (foo)\n" +
			"			foo = 5;\n" +
			"		else if (foo2) begin\n" +
			"			foo = 6;\n" +
			"		end\n" +
			"	endfunction\n" +
			"endclass\n"
			;
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		log.debug("Result:");
		log.debug(result);
		IndentComparator.compare("testBasicClass", expected, result);
		LogFactory.removeLogHandle(log);
	}
	
	public void testAssertStmt() {
		LogHandle log = LogFactory.getLogHandle("testAssertStmt");
		String content =
						"\n" +
						"module t; // Comment.\n" +
						"task t();\n" +
						"assert (this.foo.randomize ()); // Single line assertion\n" +
						"assert (this.foo.randomize ())  // assert-else\n" +
						"else \n" +
						"$display (\"Fail\");\n" +
						"assert (a==1) // assert pass message fail message\n" +
						"begin\n" +
						"$display (\"Pass\");\n" +
						"end\n" +
						"else\n" +
						"$display (\"Fail\");\n" +
						"if (1)	begin\n" +
						"thing = 1;\n" +
						"end\n" +
						"else\n" +
						"begin\n" +
						"thing = 2;\n" +
						"end\n" +
						"endtask\n" +
						"//comment1\n" +
						"assign a = b;\n" +
						"endmodule\n"
			;
		String expected =
						"\n" +
						"module t; // Comment.\n" +
						"	task t();\n" +
						"		assert (this.foo.randomize ()); // Single line assertion\n" +
						"		assert (this.foo.randomize ())  // assert-else\n" +
						"		else \n" +
						"			$display (\"Fail\");\n" +
						"		assert (a==1) // assert pass message fail message\n" +
						"		begin\n" +
						"			$display (\"Pass\");\n" +
						"		end\n" +
						"		else\n" +
						"			$display (\"Fail\");\n" +
						"		if (1)	begin\n" +
						"			thing = 1;\n" +
						"		end\n" +
						"		else\n" +
						"		begin\n" +
						"			thing = 2;\n" +
						"		end\n" +
						"	endtask\n" +
						"	//comment1\n" +
						"	assign a = b;\n" +
						"endmodule\n"
						;
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		log.debug("Result:");
		log.debug(result);
		IndentComparator.compare("testAssertStmt", expected, result);
		LogFactory.removeLogHandle(log);
	}
	
	public void testBasicCoverpoint_2() {
		SVCorePlugin.getDefault().enableDebug(false);
		String content =
			"class c1;\n" +
			"\n" +
			"covergroup cg1;\n" +
			"cp : coverpoint a;\n" +
			"cp : coverpoint b;\n" +
			"endgroup\n" +
			"endclass\n"
			;
		String expected =
			"class c1;\n" +
			"\n" +
			"	covergroup cg1;\n" +
			"		cp : coverpoint a;\n" +
			"		cp : coverpoint b;\n" +
			"	endgroup\n" +
			"endclass\n"
			;
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		fLog.debug("Result:");
		fLog.debug(result);
		IndentComparator.compare(getName(), expected, result);
	}

	public void testBasicCoverpoint_3() {
		SVCorePlugin.getDefault().enableDebug(false);
		String content =
			"class c1;\n" +
			"\n" +
			"covergroup cg1;\n" +
			"cp : coverpoint\n" +
			"a;\n" +
			"cp : coverpoint\n" +
			"b;\n" +
			"endgroup\n" +
			"endclass\n"
			;
		String expected =
			"class c1;\n" +
			"\n" +
			"	covergroup cg1;\n" +
			"		cp : coverpoint\n" +
			"			a;\n" +
			"		cp : coverpoint\n" +
			"			b;\n" +
			"	endgroup\n" +
			"endclass\n"
			;
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		fLog.debug("Result:");
		fLog.debug(result);
		IndentComparator.compare(getName(), expected, result);
	}
	
	public void testEmptyCaseStmt() throws Exception {
		LogHandle log = LogFactory.getLogHandle("testEmptyCaseStmt");
		String content =
			"module t;\n" +
			"logic a;\n" +
			"always @ (a) begin\n" +
			"case(a)\n" + 
			"endcase\n" +
			"end\n" +
			"endmodule\n"
			;
		String expected =
			"module t;\n" +
			"	logic a;\n" +
			"	always @ (a) begin\n" +
			"		case(a)\n" + 
			"		endcase\n" +
			"	end\n" +
			"endmodule\n"
			;

		SVCorePlugin.getDefault().enableDebug(false);
		log.debug("--> testEmptyCaseStmt()");
		try {
			SVIndentScanner scanner = new SVIndentScanner(
					new StringTextScanner(content));

			ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
			indenter.init(scanner);
			indenter.setTestMode(true);

			String result = indenter.indent();

			log.debug("Result:");
			log.debug(result);
			IndentComparator.compare("testEmptyCaseStmt", expected, result);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			log.debug("<-- testEmptyCaseStmt()");
		}
		LogFactory.removeLogHandle(log);
	}

	public void testRandSequence() throws Exception {
		LogHandle log = LogFactory.getLogHandle("testRandSequence");
		String expected =
			"module rs();\n" +
			"	initial\n" +
			"	begin\n" +
			"		repeat(5)\n" +
			"		begin\n" +
			"			randsequence( main )\n" +
			"				main : one two three ;\n" +
			"				one : {$write(\"one\");};\n" +
			"				two : {\n" +
			"					$write(\"two\");\n" +
			"				};\n" +
			"				three : \n" +
			"				{\n" +
			"					$write(\"three\");\n" +
			"				};\n" +
			"			endsequence\n" +
			"		end\n" +
			"	end\n" +
			"	initial\n" +
			"	begin\n" +
			"		repeat(6000)\n" +
			"			randsequence( main )\n" +
			"				main : one := 1| two := 2| three := 3;\n" +
			"				three: {three_3++;};\n" +
			"			endsequence\n" +
			"		$display(\" one %0d two %0d three %0d\",one_1,two_2,three_3);\n" +
			"	end\n" +
			"\n" +
			"	initial\n" +
			"	begin\n" +
			"		on = 0;\n" +
			"		one_1 = 0;\n" +
			"		repeat(2500)\n" +
			"			randsequence( main )\n" +
			"				main : one three;\n" +
			"				one : {\n" +
			"					if(on) \n" +
			"						one_1++; \n" +
			"					else \n" +
			"						two_2 ++; \n" +
			"				};\n" +
			"				three: {\n" +
			"					three_3++;\n" +
			"				};\n" +
			"			endsequence\n" +
			"	end\n" +
			"\n" +
			"	initial\n" +
			"	begin\n" +
			"		one_1 = 0;\n" +
			"		for(int i = 0 ;i < 6 ;i++)\n" +
			"		begin\n" +
			"			randsequence( main )\n" +
			"				main : \n" +
			"					case(i%3)\n" +
			"						0 : one;\n" +
			"						1 : begin\n" +
			"							two;\n" +
			"						end\n" +
			"						default: def;\n" +
			"					endcase;\n" +
			"				one : {\n" +
			"					$display(\"one\");\n" +
			"				};\n" +
			"				def : {$display(\"default\");};\n" +
			"			endsequence\n" +
			"		end\n" +
			"	end\n" +
			"\n" +
			"	initial\n" +
			"	begin\n" +
			"		one_1 = 0;\n" +
			"		randsequence( main )\n" +
			"			main : \n" +
			"				one | \n" +
			"				repeat(2) \n" +
			"				two | \n" +
			"				repeat (3) three ;\n" +
			"			one : one_1++;\n" +
			"			two : \n" +
			"				two_2++;\n" +
			"		endsequence\n" +
			"\n" +
			"	end\n" +
			"\n" +
			"	initial\n" +
			"		for(int i = 0;i < 24;i++) begin\n" +
			"			randsequence( main )\n" +
			"				main : rand join S1 S2 ;\n" +
			"				S1 : \n" +
			"					A B ;\n" +
			"				S2 : C D ;\n" +
			"				A : $write(\"A\");\n" +
			"				B : \n" +
			"					$write(\"B\");\n" +
			"			endsequence\n" +
			"		end\n" +
			"\n" +
			"	initial\n" +
			"		randsequence()\n" +
			"			TOP : P1 P2 ;\n" +
			"			P2 : A { if( flag == 1 ) return; } B C ;\n" +
			"			A : { $display( A ); } ;\n" +
			"			B : { \n" +
			"				if( flag == 2 ) \n" +
			"					return; \n" +
			"				$display( B ); \n" +
			"			} ;\n" +
			"		endsequence\n" +
			"	initial\n" +
			"		randsequence( main )\n" +
			"			main : first second gen ;\n" +
			"			first : add | dec ;\n" +
			"			second : pop | push ;\n" +
			"			add : gen(\"add\") ;\n" +
			"			dec : \n" +
			"				gen(\"dec\") ;\n" +
			"			gen( string s = \"done\" ) : { $display( s ); } ;\n" +
			"		endsequence						\n" +
			"endmodule\n"
			;


		SVCorePlugin.getDefault().enableDebug(false);
		log.debug("--> testRandSequenceStmt()");
		try {
			SVIndentScanner scanner = new SVIndentScanner(
					new StringTextScanner(removeLeadingWS(expected.toString())));

			ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
			indenter.init(scanner);
			indenter.setTestMode(true);

			String result = indenter.indent();

			log.debug("Result:");
			log.debug(result);
			IndentComparator.compare("testRandSequenceStmt", expected, result);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			log.debug("<-- testRandSequenceStmt()");
		}
		LogFactory.removeLogHandle(log);
	}

	public void testPriorityUniqueStmt() throws Exception {
		LogHandle log = LogFactory.getLogHandle("testPriorityUniqueStmt");
		String content =
				"module t;\n" +
						"// comment\n" +
						"logic a;\n" +
						"// comment\n" +
						"always @ (a) begin\n" +
						"// comment\n" +
						"unique if(a) begin\n" +
						"// comment\n" +
						"c = d;\n" + 
						"end\n" + 
						"// comment\n" +
						"else\n" + 
						"// comment\n" +
						"c = d;\n" + 
						"// comment\n" +
						"priority if(a)\n" +
						"begin\n" + 
						"// comment\n" +
						"c = d;\n" + 
						"end\n" + 
						"// comment\n" +
						"else begin\n" + 
						"// comment\n" +
						"c = d;\n" + 
						"end\n" + 
						"// comment\n" +
						"unique case(a)\n" + 
						"// comment\n" +
						"1: begin\n" + 
						"// comment\n" +
						"a = b;\n" + 
						"end\n" +
						"// comment\n" +
						"2: a = b;\n" +
						"// comment\n" +
						"3:\n" +
						"// comment\n" +
						"a = b;\n" +
						"endcase\n" +
						"// comment\n" +
						"priority case(a)\n" + 
						"// comment\n" +
						"1: begin\n" + 
						"// comment\n" +
						"a = b;\n" + 
						"end\n" +
						"// comment\n" +
						"2: a = b;\n" +
						"// comment\n" +
						"3:\n" +
						"// comment\n" +
						"a = b;\n" +
						"endcase\n" +
						"// comment\n" +
						"end\n" +
						"endmodule\n"
						;
		String expected =
						"module t;\n" +
						"	// comment\n" +
						"	logic a;\n" +
						"	// comment\n" +
						"	always @ (a) begin\n" +
						"		// comment\n" +
						"		unique if(a) begin\n" +
						"			// comment\n" +
						"			c = d;\n" + 
						"		end\n" + 
						"		// comment\n" +
						"		else\n" + 
						"			// comment\n" +
						"			c = d;\n" + 
						"		// comment\n" +
						"		priority if(a)\n" +
						"		begin\n" + 
						"			// comment\n" +
						"			c = d;\n" + 
						"		end\n" + 
						"		// comment\n" +
						"		else begin\n" + 
						"			// comment\n" +
						"			c = d;\n" + 
						"		end\n" + 
						"		// comment\n" +
						"		unique case(a)\n" + 
						"			// comment\n" +
						"			1: begin\n" + 
						"				// comment\n" +
						"				a = b;\n" + 
						"			end\n" +
						"			// comment\n" +
						"			2: a = b;\n" +
						"				// comment\n" +
						"			3:\n" +
						"			// comment\n" +
						"				a = b;\n" +
						"		endcase\n" +
						"		// comment\n" +
						"		priority case(a)\n" + 
						"			// comment\n" +
						"			1: begin\n" + 
						"				// comment\n" +
						"				a = b;\n" + 
						"			end\n" +
						"			// comment\n" +
						"			2: a = b;\n" +
						"				// comment\n" +
						"			3:\n" +
						"			// comment\n" +
						"				a = b;\n" +
						"		endcase\n" +
						"		// comment\n" +
						"	end\n" +
						"endmodule\n"
						;
		
		SVCorePlugin.getDefault().enableDebug(false);
		log.debug("--> testPriorityUniqueStmt()");
		try {
			SVIndentScanner scanner = new SVIndentScanner(
					new StringTextScanner(content));
			
			ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
			indenter.init(scanner);
			indenter.setTestMode(true);
			
			String result = indenter.indent();
			
			log.debug("Result:");
			log.debug(result);
			IndentComparator.compare("testPriorityUniqueStmt", expected, result);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			log.debug("<-- testPriorityUniqueStmt()");
		}
		LogFactory.removeLogHandle(log);
	}
	
	public void testBussedInAlways() throws Exception {
		LogHandle log = LogFactory.getLogHandle("testBussedInAlways");
		String content =
				"module t;\n" +
				"always @(posedge clk)\n" +
				"begin\n" +
				"{a} <= 1;\n" +
				"end\n" +
				"endmodule\n"
				;
		String expected =
				"module t;\n" +
				"	always @(posedge clk)\n" +
				"	begin\n" +
				"		{a} <= 1;\n" +
				"	end\n" +
				"endmodule\n"
				;
		
		SVCorePlugin.getDefault().enableDebug(false);
		log.debug("--> testBussedInAlways()");
		try {
			SVIndentScanner scanner = new SVIndentScanner(
					new StringTextScanner(content));
			
			ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
			indenter.init(scanner);
			indenter.setTestMode(true);
			
			String result = indenter.indent();
			
			log.debug("Result:");
			log.debug(result);
			IndentComparator.compare("testBussedInAlways", expected, result);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			log.debug("<-- testBussedInAlways()");
		}
		LogFactory.removeLogHandle(log);
	}
	
	public void testInitialBlock() {
		LogHandle log = LogFactory.getLogHandle("testInitialBlock");
		String content =
			"module t;\n" +
			"logic a;\n" +
			"initial begin\n" +
			"a = 5;\n" +
			"end\n" +
			"endmodule\n"
			;
		String expected =
			"module t;\n" +
			"	logic a;\n" +
			"	initial begin\n" +
			"		a = 5;\n" +
			"	end\n" +
			"endmodule\n"
			;
		SVCorePlugin.getDefault().enableDebug(false);
		log.debug("--> testInitialBlock");
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		log.debug("Result:");
		log.debug(result);
		IndentComparator.compare("testInitialBlock", expected, result);
		log.debug("<-- testInitialBlock");
		
		LogFactory.removeLogHandle(log);
	}

	public void testInitialStmt() {
		LogHandle log = LogFactory.getLogHandle("testInitialStmt");
		String content =
			"module t;\n" +
			"logic a;\n" +
			"initial\n" +
			"a = 5;\n" +
			"logic b;\n" +
			"endmodule\n"
			;
		String expected =
			"module t;\n" +
			"	logic a;\n" +
			"	initial\n" +
			"		a = 5;\n" +
			"	logic b;\n" +
			"endmodule\n"
			;
		SVCorePlugin.getDefault().enableDebug(false);
		log.debug("--> testInitialStmt");
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		log.debug("Result:");
		log.debug(result);
		IndentComparator.compare("testInitialStmt", expected, result);
		log.debug("<-- testInitialStmt");
		
		LogFactory.removeLogHandle(log);
	}

	public void testExpectStmnt() {
		LogHandle log = LogFactory.getLogHandle("testExpectStmnt");
		String content =
				"module bob ();\n" +
				"logic clk, a_signal;\n" +
				"\n" +
				"task t ();\n" +
				"begin\n" +
				"expect( \n" +
				"@(posedge clk) \n" +
				"a_signal \n" +
				")   // Parser error on ## \n" +
				"else begin  // Indent error ... else should not be indented\n" +
				"$display(\"mismatch event\");\n" +
				"end   // indent error, indent once more\n" +
				"end\n" +
				"endtask\n" +
				"task t ();\n" +
				"begin\n" +
				"expect( @(posedge clk) a_signal )   // Parser error on ##\n" +
				"pass();\n" +
				"else begin  // Indent error ... else should not be indented\n" +
				"$display(\"mismatch event\");\n" +
				"end   // indent error, indent once more\n" +
				"end\n" +
				"endtask\n" +
				"task t ();\n" +
				"begin\n" +
				"expect( @(posedge clk) a_signal )   // Parser error on ##\n" +
				"begin\n" +
				"pass();\n" +
				"pass();\n" +
				"pass();\n" +
				"end\n" +
				"else begin  // Indent error ... else should not be indented\n" +
				"$display(\"mismatch event\");\n" +
				"end   // indent error, indent once more\n" +
				"end\n" +
				"endtask\n" +
				"endmodule\n"
			;
		String expected =
			"module bob ();\n" +
			"	logic clk, a_signal;\n" +
			"	\n" +
			"	task t ();\n" +
			"		begin\n" +
			"			expect( \n" +
			"					@(posedge clk) \n" +
			"					a_signal \n" +
			"				)   // Parser error on ## \n" +
			"			else begin  // Indent error ... else should not be indented\n" +
			"				$display(\"mismatch event\");\n" +
			"			end   // indent error, indent once more\n" +
			"		end\n" +
			"	endtask\n" +
			"	task t ();\n" +
			"		begin\n" +
			"			expect( @(posedge clk) a_signal )   // Parser error on ##\n" +
			"				pass();\n" +
			"			else begin  // Indent error ... else should not be indented\n" +
			"				$display(\"mismatch event\");\n" +
			"			end   // indent error, indent once more\n" +
			"		end\n" +
			"	endtask\n" +
			"	task t ();\n" +
			"		begin\n" +
			"			expect( @(posedge clk) a_signal )   // Parser error on ##\n" +
			"			begin\n" +
			"				pass();\n" +
			"				pass();\n" +
			"				pass();\n" +
			"			end\n" +
			"			else begin  // Indent error ... else should not be indented\n" +
			"				$display(\"mismatch event\");\n" +
			"			end   // indent error, indent once more\n" +
			"		end\n" +
			"	endtask\n" +
			"endmodule\n"
			;
		SVCorePlugin.getDefault().enableDebug(false);
		log.debug("--> testExpectStmnt");
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		log.debug("Result:");
		log.debug(result);
		IndentComparator.compare("testExpectStmnt", expected, result);
		log.debug("<-- testExpectStmnt");
		
		LogFactory.removeLogHandle(log);
	}

	public void testStructVar() {
		String testname = "testStructVar";
		LogHandle log = LogFactory.getLogHandle(testname);
		String content =
			"module t;\n" +
			"struct {\n" +
			"int a;\n" +
			"int b;\n" +
			"} s;\n" +
			"logic b;\n" +
			"endmodule\n"
			;
		String expected =
			"module t;\n" +
			"	struct {\n" +
			"		int a;\n" +
			"		int b;\n" +
			"	} s;\n" +
			"	logic b;\n" +
			"endmodule\n"
			;
		SVCorePlugin.getDefault().enableDebug(false);
		log.debug("--> " + testname);
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		log.debug("Result:");
		log.debug(result);
		IndentComparator.compare(testname, expected, result);
		log.debug("<-- " + testname);
		
		LogFactory.removeLogHandle(log);
	}

	public void testTypedefStruct() {
		String testname = "testTypedefStruct";
		LogHandle log = LogFactory.getLogHandle(testname);
		String content =
			"module t;\n" +
			"//comment1\n" +
			"typedef struct {\n" +
			"//comment2\n" +
			"int a;\n" +
			"int b;\n" +
			"//comment3\n" +
			"} foo_t;\n" +
			"//comment4\n" +
			"foo_t b;\n" +
			"//comment5\n" +
			"typedef someclass_c #(1) new_name;\n" +
			"//comment6\n" +
			"foo_t b;\n" +
			"endmodule\n"
			;
		String expected =
			"module t;\n" +
			"	//comment1\n" +
			"	typedef struct {\n" +
			"		//comment2\n" +
			"		int a;\n" +
			"		int b;\n" +
			"		//comment3\n" +
			"	} foo_t;\n" +
			"	//comment4\n" +
			"	foo_t b;\n" +
			"	//comment5\n" +
			"	typedef someclass_c #(1) new_name;\n" +
			"	//comment6\n" +
			"	foo_t b;\n" +
			"endmodule\n"
			;
		log.debug("--> " + testname);
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		log.debug("Result:");
		log.debug(result);
		IndentComparator.compare(testname, expected, result);
		log.debug("<-- " + testname);
		
		LogFactory.removeLogHandle(log);
	}

	public void testTypedefNonStructUnion() {
		String testname = "testTypedefNonStructUnion";
		LogHandle log = LogFactory.getLogHandle(testname);
		SVCorePlugin.getDefault().enableDebug(false);
		String content =
			"module t;\n" +
			"typedef int unsigned uint32_t;\n" +
			"typedef short unsigned uint16_t;\n" +
			"uint32_t foo;\n" +
			"endmodule\n"
			;
		String expected =
			"module t;\n" +
			"	typedef int unsigned uint32_t;\n" +
			"	typedef short unsigned uint16_t;\n" +
			"	uint32_t foo;\n" +
			"endmodule\n"
			;
		log.debug("--> " + testname);
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		log.debug("Result:");
		log.debug(result);
		IndentComparator.compare(testname, expected, result);
		log.debug("<-- " + testname);
		
		LogFactory.removeLogHandle(log);
	}

	public void testUnionVar() {
		String testname = "testUnionVar";
		LogHandle log = LogFactory.getLogHandle(testname);
		String content =
			"module t;\n" +
			"union {\n" +
			"int a;\n" +
			"int b;\n" +
			"} s;\n" +
			"// this is a comment\n" +
			"logic b;\n" +
			"endmodule\n"
			;
		String expected =
			"module t;\n" +
			"	union {\n" +
			"		int a;\n" +
			"		int b;\n" +
			"	} s;\n" +
			"	// this is a comment\n" +
			"	logic b;\n" +
			"endmodule\n"
			;
		log.debug("--> " + testname);
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		log.debug("Result:");
		log.debug(result);
		IndentComparator.compare(testname, expected, result);
		log.debug("<-- " + testname);
		
		LogFactory.removeLogHandle(log);
	}

	public void testTypedefUnion() {
		String testname = "testTypedefUnion";
		LogHandle log = LogFactory.getLogHandle(testname);
		String content =
			"module t;\n" +
			"typedef union {\n" +
			"//comment1\n" +
			"int a;\n" +
			"//comment2\n" +
			"int b;\n" +
			"} foo_t;\n" +
			"//comment3\n" +
			"foo_t b;\n" +
			"endmodule\n"
			;
		String expected =
			"module t;\n" +
			"	typedef union {\n" +
			"		//comment1\n" +
			"		int a;\n" +
			"		//comment2\n" +
			"		int b;\n" +
			"	} foo_t;\n" +
			"	//comment3\n" +
			"	foo_t b;\n" +
			"endmodule\n"
			;
		log.debug("--> " + testname);
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		log.debug("Result:");
		log.debug(result);
		IndentComparator.compare(testname, expected, result);
		log.debug("<-- " + testname);
		
		LogFactory.removeLogHandle(log);
	}

	public void testEnumVar() {
		String testname = "testEnumVar";
		LogHandle log = LogFactory.getLogHandle(testname);
		String content =
			"module t;\n" +
			"enum {\n" +
			"A,\n" +
			"B\n" +
			"} e;\n" +
			"logic b;\n" +
			"endmodule\n"
			;
		String expected =
			"module t;\n" +
			"	enum {\n" +
			"		A,\n" +
			"		B\n" +
			"	} e;\n" +
			"	logic b;\n" +
			"endmodule\n"
			;
		log.debug("--> " + testname);
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		log.debug("Result:");
		log.debug(result);
		IndentComparator.compare(testname, expected, result);
		log.debug("<-- " + testname);
		
		LogFactory.removeLogHandle(log);
	}

	public void testTypedefEnum() {
		String testname = "testTypedefEnum";
		LogHandle log = LogFactory.getLogHandle(testname);
		String content =
			"module t;\n" +
			"typedef enum {\n" +
			"A,\n" +
			"B\n" +
			"} foo_t;\n" +
			"foo_t b;\n" +
			"endmodule\n"
			;
		String expected =
			"module t;\n" +
			"	typedef enum {\n" +
			"		A,\n" +
			"		B\n" +
			"	} foo_t;\n" +
			"	foo_t b;\n" +
			"endmodule\n"
			;
		log.debug("--> " + testname);
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		log.debug("Result:");
		log.debug(result);
		IndentComparator.compare(testname, expected, result);
		log.debug("<-- " + testname);
		
		LogFactory.removeLogHandle(log);
	}

	public void testBasicModuleComment() {
		LogHandle log = LogFactory.getLogHandle("testBasicModuleComment");
		String content =
			"module t; // Comment.\n" +
			"logic a;\n" +
			"logic b;\n" +
			"\n" +
			"//comment1\n" +
			"assign a = b;\n" +
			"\n" +
			"//comment2\n" +
			"assign a = \n" +
			"//comment2.1\n" +
			"b;\n" +
			"//comment2.2\n" +
			"assign a = (\n" +
			"//comment2.2.1\n" +
			"b +\n" +
			"//comment2.2.2\n" +
			"(\n" +
			"//comment2.2.3\n" +
			"1 + 2\n" +
			"//comment2.2.4\n" +
			")\n" +
			"//comment2.2.5\n" +
			");\n" +
			"//comment3\n" +
			"submod sm1 (.a(a),\n" +
			"//comment4\n" +
			".b(b),\n" +
			"//comment5\n" +
			".c(\n" +
			"//comment5.1\n" +
			"c\n" +
			"//comment5.2\n" +
			"),\n" +
			"//comment6\n" +
			".d(\n" +
			"//comment6.1\n" +
			"(\n" +
			"//comment6.2\n" +
			"(\n" +
			"//comment6.3\n" +
			"d+1\n" +
			"//comment6.4\n" +
			")\n" +
			"//comment6.5\n" +
			")\n" +
			"//comment6.6\n" +
			")\n" +
			");\n" +
			"//comment7\n" +
			"endmodule\n" 
			;
		String expected =
			"module t; // Comment.\n" +
			"	logic a;\n" +
			"	logic b;\n" +
			"\n" +
			"	//comment1\n" +
			"	assign a = b;\n" +
			"\n" +
			"	//comment2\n" +
			"	assign a = \n" + 
			"		//comment2.1\n" +
			"		b;\n" +
			"	//comment2.2\n" +
			"	assign a = (\n" +
			"			//comment2.2.1\n" +
			"			b +\n" +
			"			//comment2.2.2\n" +
			"			(\n" +
			"				//comment2.2.3\n" +
			"				1 + 2\n" +
			"				//comment2.2.4\n" +
			"			)\n" +
			"			//comment2.2.5\n" +
			"		);\n" +
			"	//comment3\n" +
			"	submod sm1 (.a(a),\n" +
			"			//comment4\n" +
			"			.b(b),\n" +
			"			//comment5\n" +
			"			.c(\n" +
			"				//comment5.1\n" +
			"				c\n" +
			"				//comment5.2\n" +
			"			),\n" +
			"			//comment6\n" +
			"			.d(\n" +
			"				//comment6.1\n" +
			"				(\n" +
			"					//comment6.2\n" +
			"					(\n" +
			"						//comment6.3\n" +
			"						d+1\n" +
			"						//comment6.4\n" +
			"					)\n" +
			"					//comment6.5\n" +
			"				)\n" +
			"				//comment6.6\n" +
			"			)\n" +
			"		);\n" +
			"	//comment7\n" +
			"endmodule\n" 
			;
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		log.debug("Result:");
		log.debug(result);
		IndentComparator.compare("testBasicModuleComment", expected, result);
		LogFactory.removeLogHandle(log);
	}
	
	public void testNestedModule() {
		LogHandle log = LogFactory.getLogHandle("testNestedModule");
		String content =
			"module t;\n" +
			"logic a;\n" +
			"logic b;\n" +
			"\n" +
			"module t1;\n" +
			"wire a;\n" +
			"wire b;\n" +
			"endmodule\n" +
			"endmodule\n" 
			;
		String expected =
			"module t;\n" +
			"	logic a;\n" +
			"	logic b;\n" +
			"\n" +
			"	module t1;\n" +
			"		wire a;\n" +
			"		wire b;\n" +
			"	endmodule\n" +
			"endmodule\n" 
			;
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		log.debug("Result:");
		log.debug(result);
		IndentComparator.compare("testNestedModule", expected, result);
		LogFactory.removeLogHandle(log);
	}
	
	public void testIndentPostSingleComment() {
		LogHandle log = LogFactory.getLogHandle("testIndentPostSingleComment");
		String content =
			"class foo;\n" +					// 1
			"//comment1\n" +
			"function void my_func();\n" +		// 2
			"if (foobar) begin\n" +				// 3
			"end else begin\n" +				// 4
			"// comment block\n" +				// 5
			"a.b = 5;\n" +						// 6
			"end\n" +							// 7
			"endfunction\n" +					// 8
			"//comment2\n" +
			"endclass\n" 						// 9
			;
		String expected =
			"class foo;\n" +
			"	//comment1\n" +
			"	function void my_func();\n" +
			"		if (foobar) begin\n" +
			"		end else begin\n" +
			"			// comment block\n" +
			"			a.b = 5;\n" +
			"		end\n" +
			"	endfunction\n" +
			"	//comment2\n" +
			"endclass\n" 
			;
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		log.debug("Result:");
		log.debug(result);
		IndentComparator.compare("testIndentPostSingleComment", expected, result);
		LogFactory.removeLogHandle(log);
	}

	public void testBasicModuleWire() {
		LogHandle log = LogFactory.getLogHandle("testBasicModuleWire");
		String content =
			"module top;\n" +
			"// comment1\n" +
			"logic a;\n" +
			"logic b;\n" +
			"// comment2\n" +
			"endmodule\n"
			;
		String expected =
			"module top;\n" +
			"	// comment1\n" +
			"	logic a;\n" +
			"	logic b;\n" +
			"	// comment2\n" +
			"endmodule\n"
			;
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		log.debug("Result:");
		log.debug(result);
		IndentComparator.compare("testBasicModuleWire", expected, result);
		LogFactory.removeLogHandle(log);
	}

	public void testGenerate() {
		LogHandle log = LogFactory.getLogHandle("testGenerate");
		String content =
			"module t;\n" +
			"parameter a = 0;\n" +
			"generate\n" +
			"if(a == 0) begin\n" +
			"// ...\n" +
			"end\n" +
			"if(a == 1) begin\n" +
			"// ...\n" +
			"end\n" +
			"endgenerate\n" +
			"generate\n" +
			"begin:bob\n" +
			"if(a == 0) begin\n" +
			"// ...\n" +
			"end\n" +
			"if(a == 1) begin\n" +
			"// ...\n" +
			"end\n" +
			"end\n" +
			"endgenerate\n" +
			"endmodule\n"
			;
		String expected =
				"module t;\n" +
				"	parameter a = 0;\n" +
				"	generate\n" +
				"		if(a == 0) begin\n" +
				"			// ...\n" +
				"		end\n" +
				"		if(a == 1) begin\n" +
				"			// ...\n" +
				"		end\n" +
				"	endgenerate\n" +
				"	generate\n" +
				"	begin:bob\n" +
				"		if(a == 0) begin\n" +
				"			// ...\n" +
				"		end\n" +
				"		if(a == 1) begin\n" +
				"			// ...\n" +
				"		end\n" +
				"	end\n" +
				"	endgenerate\n" +
				"endmodule\n"
				;
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		log.debug("Result:");
		log.debug(result);
		IndentComparator.compare("testGenerate", expected, result);
		LogFactory.removeLogHandle(log);
	}
	

	public void testNoBlockIf() {
		String content =
			"class class1;\n" +
			"\n" +
			"function new();\n" +
			"if (foo)\n" +
			"$display(\"Hello\");\n" +
			"else\n" +
			"$display(\"Goodbye\");\n" +
			"foo = 6;\n" +
			"endfunction\n" +
			"endclass\n"
			;
		String expected =
			"class class1;\n" +
			"\n" +
			"	function new();\n" +
			"		if (foo)\n" +
			"			$display(\"Hello\");\n" +
			"		else\n" +
			"			$display(\"Goodbye\");\n" +
			"		foo = 6;\n" +
			"	endfunction\n" +
			"endclass\n"
			;				
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		fLog.debug("Result:");
		fLog.debug(result);
		IndentComparator.compare(getName(), expected, result);
	}
	
	public void testModule() {
		BundleUtils utils = new BundleUtils(SVCoreTestsPlugin.getDefault().getBundle());
		ByteArrayOutputStream bos;
		
		bos = utils.readBundleFile("/indent/module.svh");
		
		String expected = bos.toString();
		StringBuilder sb = removeLeadingWS(expected);
		
		// Run the indenter over the reference source
		SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(sb));
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		IndentComparator.compare("testModule", expected, result);
		
		/*
		StringBuilder result = new StringBuilder(indenter.indent(-1, -1));
		StringBuilder reference = new StringBuilder(ref);
		
		String ref_line, ind_line;
		int err_cnt = 0;
		int pass_cnt = 0;
		
		do {
			ref_line = readLine(reference);
			ind_line = readLine(result);
			
			if (ref_line != null && ind_line != null) {
				if (ref_line.equals(ind_line)) {
					log.debug("[OK ]:" + ref_line);
					pass_cnt++;
				} else {
					log.debug("[ERR]:" + ref_line);
					log.debug("[   ]:" + ind_line);
					err_cnt++;
				}
			}
		} while (ref_line != null && ind_line != null);
		
		assertNull("Checking that output not truncated", ref_line);
		assertNull("Checking for no excess output", ind_line);
		
		assertEquals("Expect no errors", 0, err_cnt);
		assertTrue("Check accomplished work", (pass_cnt != 0));
		 */
	}

	public void testMultiBlankLine() {
		LogHandle log = LogFactory.getLogHandle("testMultiBlankLine");
		String ref = 
		"class my_component1 extends ovm_component;\n" +
		"	\n" +
		"	\n" +
		"	function new(string name, ovm_component parent);\n" +
		"		super.new(name, parent);\n" +
		"	endfunction\n" +
		"	\n"  +
		"	\n" +
		"endclass\n";
		
		// Run the indenter over the reference source
		SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(ref));
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent(-1, -1);
		
		log.debug("Ref:\n" + ref);
		log.debug("====");
		log.debug("Result:");
		log.debug(result);
		log.debug("====");
		
		IndentComparator.compare(log, "testMultiBlankLine", ref, result);
		LogFactory.removeLogHandle(log);
	}
	
	public void testFunctionComment() {
		String ref = 
			"class my_class;\n" +
			"\n" +
			"	/**\n" +
			"	 * my_func\n" +
			"	 *\n" +
			"	 */\n" +
			"	function void foobar;\n" +
			"\n" +
			"	endfunction\n" +
			"\n" +
			"endclass\n"
			;

			LogHandle log = LogFactory.getLogHandle("testFunctionComment");
			SVCorePlugin.getDefault().enableDebug(false);
			
			// Run the indenter over the reference source
			SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(ref));
			ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
			indenter.init(scanner);
			indenter.setTestMode(true);
			
			String result = indenter.indent(-1, -1);
			
			log.debug("Ref:\n" + ref);
			log.debug("====");
			log.debug("Result:");
			log.debug(result);
			log.debug("====");
			
			IndentComparator.compare(log, "testFunctionComment", ref, result);
			LogFactory.removeLogHandle(log);
	}

	public void testModuleFirstItemComment() {
		String ref = 
		"module foo;\n" +
		"	// this is a comment\n" +
		"	a = 5;\n" +
		"endmodule\n"
		;
		
		SVCorePlugin.getDefault().enableDebug(false);
		LogHandle log = LogFactory.getLogHandle("testModuleFirstItemComment");
		
		// Run the indenter over the reference source
		SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(ref));
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent(-1, -1);
		
		log.debug("Ref:\n" + ref);
		log.debug("====");
		log.debug("Result:\n" + result);
		log.debug("====");
		
		IndentComparator.compare(log, "testModuleFirstItemComment", ref, result);
		LogFactory.removeLogHandle(log);
	}

	public void testInitialFirstItemComment() {
		String ref = 
		"module foo;\n" +
		"	\n" +
		"	initial begin\n" +
		"		// this is a comment\n" +
		"	end\n" +
		"	a = 5;\n" +
		"endmodule\n"
		;
		
		SVCorePlugin.getDefault().enableDebug(false);
		LogHandle log = LogFactory.getLogHandle("testInitialFirstItemComment");
		
		// Run the indenter over the reference source
		SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(ref));
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);

		String result = indenter.indent(-1, -1);
		
		log.debug("Ref:\n" + ref);
		log.debug("====");
		log.debug("Result:\n" + result);
		log.debug("====");
		
		IndentComparator.compare(log, "testInitialFirstItemComment", ref, result);
		LogFactory.removeLogHandle(log);
	}

	public void testFunctionFirstItemComment() {
		String ref = 
		"module foo;\n" +
		"	\n" +
		"	function automatic void foobar_f();\n" +
		"		// this is a comment\n" +
		"	endfunction\n" +
		"	a = 5;\n" +
		"endmodule\n"
		;
		
		SVCorePlugin.getDefault().enableDebug(false);
		LogHandle log = LogFactory.getLogHandle("testFunctionFirstItemComment");
		
		// Run the indenter over the reference source
		SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(ref));
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent(-1, -1);
		
		log.debug("Ref:\n" + ref);
		log.debug("====");
		log.debug("Result:\n" + result);
		log.debug("====");
		
		IndentComparator.compare(log, "testFunctionFirstItemComment", ref, result);
		LogFactory.removeLogHandle(log);
	}

	public void testIfInFunction() {
		String ref =
		"class xbus_bus_monitor;\n" +							// 1
		"\n" +													// 2
		"		function void ignored();\n" +					// 3
		"		endfunction\n" +								// 4
		"\n" +													// 5
		"		// perform_transfer_coverage\n" +				// 6
		"		function void perform_transfer_coverage();\n" +
		"			if (trans_collected.read_write != NOP) begin\n" +
		"				-> cov_transaction;\n" +
		"				for (int unsigned i = 0; i < trans_collected.size; i++) begin\n" +
        "					addr = trans_collected.addr + i;\n" +
        "					data = trans_collected.data[i];\n" +
        "					wait_state = trans_collected.wait_state[i];\n" +
        "					-> cov_transaction_beat;\n" +
        "				end\n" +
        "			end\n" +
        "		endfunction : perform_transfer_coverage\n" +
        "\n" +
        "endclass : xbus_bus_monitor\n"
        ;
		
		SVCorePlugin.getDefault().enableDebug(false);
		LogHandle log = LogFactory.getLogHandle("testIfInFunction");
		
		// Run the indenter over the reference source
		SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(ref));
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		indenter.setAdaptiveIndent(true);
		indenter.setAdaptiveIndentEnd(5);
		String result = indenter.indent(-1, -1);
		
		log.debug("Ref:\n" + ref);
		log.debug("====");
		log.debug("Result:\n" + result);
		log.debug("====");
		
		IndentComparator.compare(log, "testIfInFunction", ref, result);
		LogFactory.removeLogHandle(log);
	}

	public void testIfEmpty() {
		String testname = "testIfEmpty";
		String ref =
				"module foo ();\n" +
				"	initial begin\n" +
				"		// comment1\n" +
				"		if (1) else begin a = b; end\n" +
				"		// comment2\n" +
				"		if (1) else\n" +
				"		begin\n" +
				"			// comment3\n" +
				"			a=b;\n" +
				"		end\n" +
				"		// comment4\n" +
				"		if (1)\n" +
				"			// comment5\n" +
				"		else\n" +
				"		begin\n" +
				"			// comment6\n" +
				"			a=b;\n" +
				"		end\n" +
				"		// comment7\n" +
				"		if (1) else a = b; \n" +
				"		// comment8\n" +
				"		if (1) else\n" +
				"			// comment9\n" +
				"			a=b;\n" +
				"		// comment10\n" +
				"		if (1)\n" +
				"			// comment11\n" +
				"		else\n" +
				"			// comment12\n" +
				"			a=b;\n" +
				"		// comment13\n" +
				"		b=c;\n" +
				"	end\n"  +
				"endmodule\n" 
				;
		
		LogHandle log = LogFactory.getLogHandle(testname);
		SVCorePlugin.getDefault().enableDebug(true);
		
		// Run the indenter over the reference source
		SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(ref));
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent(-1, -1);
		
		IndentComparator.compare(log, testname, ref, result);
		SVCorePlugin.getDefault().enableDebug(false);
		LogFactory.removeLogHandle(log);
	}
	
		public void testForkJoin() {
		String testname = "testForkJoin";
		String ref =
		"class foo;\n" +
		"	task bar();\n" +
		"		fork\n" +
		"			a();\n" +
		"			b();\n" +
		"		join\n" +
		"		fork : named_fork\n" +
		"			a();\n" +
		"			b();\n" +
		"		join_none\n" +
		"	endtask\n" +
		"endclass\n"
		;
		
		SVCorePlugin.getDefault().enableDebug(false);
		LogHandle log = LogFactory.getLogHandle(testname);
		
		// Run the indenter over the reference source
		SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(ref));
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		indenter.setAdaptiveIndent(true);
		indenter.setAdaptiveIndentEnd(5);
		String result = indenter.indent(-1, -1);
		
		log.debug("Ref:\n" + ref);
		log.debug("====");
		log.debug("Result:\n" + result);
		log.debug("====");
		
		IndentComparator.compare(log, testname, ref, result);
		LogFactory.removeLogHandle(log);
	}

	public void testForever() {
		String testname = "testForever";
		String ref =
				"module foo ();\n" +
				"	initial begin\n" +
				"		forever #1\n" +
				"		begin\n" +
				"			a=b;\n" +
				"		end\n" +
				"		forever @(posedge clk)\n" +
				"		begin\n" +
				"			a=b;\n" +
				"		end\n" +
				"	end\n"  +
				"endmodule\n" 
				;
		
		LogHandle log = LogFactory.getLogHandle(testname);
		SVCorePlugin.getDefault().enableDebug(false);
		
		// Run the indenter over the reference source
		SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(ref));
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		indenter.setAdaptiveIndent(true);
		indenter.setAdaptiveIndentEnd(5);
		String result = indenter.indent(-1, -1);
		
		SVCorePlugin.getDefault().enableDebug(false);
		IndentComparator.compare(log, testname, ref, result);
		LogFactory.removeLogHandle(log);
	}
	
	public void testEmptyForkJoin() {
		String testname = "testEmptyForkJoin";
		String ref =
		"class foo;\n" +
		"	task bar();\n" +
		"		fork\n" +
		"		join\n" +
		"	endtask\n" +
		"endclass\n"
		;
		
		SVCorePlugin.getDefault().enableDebug(false);
		LogHandle log = LogFactory.getLogHandle(testname);
		
		// Run the indenter over the reference source
		SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(ref));
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		indenter.setAdaptiveIndent(true);
		indenter.setAdaptiveIndentEnd(5);
		String result = indenter.indent(-1, -1);
		
		log.debug("Ref:\n" + ref);
		log.debug("====");
		log.debug("Result:\n" + result);
		log.debug("====");
		
		IndentComparator.compare(log, testname, ref, result);
		LogFactory.removeLogHandle(log);
	}

	public void testForkJoinBlock() {
		String testname = "testForkJoinBlock";
		String ref =
		"class foo;\n" +
		"	task bar();\n" +
		"		fork\n" +
		"			begin\n" +
		"				a();\n" +
		"			end\n" +
		"			begin\n" +
		"				b();\n" +
		"			end\n" +
		"		join\n" +
		"	endtask\n" +
		"endclass\n"
		;
		
		SVCorePlugin.getDefault().enableDebug(false);
		LogHandle log = LogFactory.getLogHandle(testname);
		
		// Run the indenter over the reference source
		SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(ref));
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		indenter.setAdaptiveIndent(true);
		indenter.setAdaptiveIndentEnd(5);
		String result = indenter.indent(-1, -1);
		
		log.debug("Ref:\n" + ref);
		log.debug("====");
		log.debug("Result:\n" + result);
		log.debug("====");
		
		IndentComparator.compare(log, testname, ref, result);
		LogFactory.removeLogHandle(log);
	}

	public void testRandomizeWith() {
		String ref =
		"class foo;\n" +
		"	virtual function void build_phase(uvm_phase phase);\n" +
		"		this.randomize(random_int) with\n" +
		"			{ random_int > 0;\n" +
		"				random_int<100;\n" +
		"			};\n" +
		"		this.randomize(random_int) with\n" +
		"			{ random_int > 0;\n" +
		"				random_int<100;\n" +
		"			};\n" +
		"	endfunction\n" +
		"endclass\n"
		;
		
		SVCorePlugin.getDefault().enableDebug(false);
		
		// Run the indenter over the reference source
		SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(ref));
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		indenter.setAdaptiveIndent(true);
		indenter.setAdaptiveIndentEnd(5);
		String result = indenter.indent(-1, -1);
		
		fLog.debug("Ref:\n" + ref);
		fLog.debug("====");
		fLog.debug("Result:\n" + result);
		fLog.debug("====");
		
		IndentComparator.compare(fLog, getName(), ref, result);
	}

	public void testPoundDelay() {
		String ref =
				"module m ();\n" +
				"	initial begin\n" +
				"		#1110;\n" +
				"		a = b;\n" +
				"		#1110 a = b;\n" +
				"		#1111ns	// No ;\n" +
				"			a = b;\n" +
				"		#1113fs	// No ;\n" +
				"			// comment \n" +
				"			for (i=0; i<5; i++)\n" +
				"			begin\n" +
				"				a = b;\n" +
				"			end\n" +
				"		#112ps;	// with ;\n" +
				"		if (a == b)\n" +
				"		begin\n" +
				"			a = b;\n" +
				"		end\n" +
				"	end\n" +
				"endmodule\n"
						;
		
		SVCorePlugin.getDefault().enableDebug(false);
		
		// Run the indenter over the reference source
		SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(ref));
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		indenter.setAdaptiveIndent(true);
		indenter.setAdaptiveIndentEnd(5);
		String result = indenter.indent(-1, -1);
		
		fLog.debug("Ref:\n" + ref);
		fLog.debug("====");
		fLog.debug("Result:\n" + result);
		fLog.debug("====");
		
		IndentComparator.compare(fLog, getName(), ref, result);
	}
	

	
	public void testPreProcIndent() {
		String testname = "testPreProcIndent";
		String ref =
				"package foo;\n" +
						"	import pkg_1::*;\n" +
						"	`include \"file1.svh\"\n" +
						"	`include \"file2.svh\"\n" +
						"	`include \"file3.svh\"\n" +
						"endpackage\n" +
						"\n" +
						"`ifndef INCLUDED_my_component1_svhn\n" +
						"	module m ();\n" +
						"		`ifdef ASDF // comment\n" +
						"			// comment\n" +
						"			assign a = b;\n" +
						"		`elsif JKLM // comment\n" +
						"			assign a = b;\n" +
						"		`elsif JKLM // comment\n" +
						"			assign a = b;\n" +
						"		`else\n" +
						"			// comment\n" +
						"			assign c=d;\n" +
						"	\n" +
						"		`endif\n" +
						"		`ifdef ASDF assign a = b; `else assign b = c; `endif\n" +
						"		`ifdef ASDF assign a = b; /* comment */ `else assign b = c; /* comment */ `endif /* comment */ // comment\n" +
						"		initial\n" +
						"		begin\n" +
						"			`ifdef ASDF\n" +
						"				// comment\n" +
						"				a = b;\n" +
						"				a = b;\n" +
						"			`endif\n" +
						"			\n" +
						"		end\n" +
						"		always @*\n" +
						"		begin\n" +
						"			`ifdef ASDF\n" +
						"				// comment\n" +
						"				a = b;\n" +
						"				a = b;\n" +
						"			`endif\n" +
						"			\n" +
						"		end\n" +
						"	endmodule\n" +
						"`endif				// comment\n" +
						"// comment\n" 
						;
		
		SVCorePlugin.getDefault().enableDebug(false);
		LogHandle log = LogFactory.getLogHandle(testname);
		
		// Run the indenter over the reference source
		SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(ref));
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		indenter.setAdaptiveIndent(true);
		indenter.setAdaptiveIndentEnd(5);
		String result = indenter.indent(-1, -1);
		
		log.debug("Ref:\n" + ref);
		log.debug("====");
		log.debug("Result:\n" + result);
		log.debug("====");
		
		IndentComparator.compare(log, testname, ref, result);
		LogFactory.removeLogHandle(log);
	}
	
	public void testImportDPI() {
		String testname = "testImportDPI";
		String ref =
			"import \"DPI-C\" function string return_string_in_c(string text, string output_txt);\n" +
			"export \"DPI-C\" f_plus = function \f+ ; // \"f+\" exported as \"f_plus\" \n" +
			"export \"DPI-C\" function f; // \"f\" exported under its own name\n" +
			"import \"DPI-C\" init_1 = function void \\init[1] (); // \"init_1\" is a linkage name\n" +
			"import \"DPI-C\" \\begin = function void \\init[2] (); // \"begin\" is a linkage name\n" +
			"\n" +
			"typedef class keycontrol_seq_handles;\n" +
			"//-----------------------\n" +
			"class keycontrol_predictor#(int PARAM_PLACEHOLDER = 1) extends pve_predictor#(keycontrol_seq_handles);//extends uvm_component;\n" +
			"	`uvm_component_param_utils(keycontrol_predictor#(PARAM_PLACEHOLDER))\n" +
			"endclass\n"
			;
			
		LogHandle log = LogFactory.getLogHandle(testname);
		SVCorePlugin.getDefault().enableDebug(false);
		
		// Run the indenter over the reference source
		SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(ref));
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		indenter.setAdaptiveIndent(true);
		indenter.setAdaptiveIndentEnd(5);
		String result = indenter.indent(-1, -1);
		
		log.debug("Ref:\n" + ref);
		log.debug("====");
		log.debug("Result:\n" + result);
		log.debug("====");
		
		IndentComparator.compare(log, testname, ref, result);
		LogFactory.removeLogHandle(log);
	}

	public static StringBuilder removeLeadingWS(String ref) {
		StringBuilder sb = new StringBuilder();
		int i=0;
		while (i < ref.length()) {
			// Read leading whitespace
			while (i < ref.length() && 
					Character.isWhitespace(ref.charAt(i)) &&
					ref.charAt(i) != '\n') {
				i++;
			}
			
			if (i >= ref.length()) {
				break;
			}
			
			if (ref.charAt(i) == '\n') {
				sb.append('\n');
				i++;
				continue;
			} else {
				// Read to the end of the line
				while (i < ref.length() && ref.charAt(i) != '\n') {
					sb.append(ref.charAt(i));
					i++;
				}

				if (i < ref.charAt(i) && ref.charAt(i) == '\n') {
					sb.append('\n');
					i++;
				}
			}
		}
		
		return sb;
	}

	/*
	private String readLine(StringBuilder sb) {
		int end = 0;
		String ret = null;
		
		while (end < sb.length() && sb.charAt(end) != '\n') {
			end++;
		}
		
		if (end < sb.length()) {
			if (end == 0) {
				ret = "";
				sb.delete(0, 1);
			} else {
				ret = sb.substring(0, end);
				sb.delete(0, end+1);
			}
		}
		
		return ret;
	}
	 */
	public void testSpecifyBlock() {
		SVCorePlugin.getDefault().enableDebug(false);
		String content =
			"module m;\n" +
			"\n" +
			"specify\n" +
			"if (someSig[0] == 1'b0)\n" +
			"(CLK => Q[15])=(1.000, 1.000);\n" +
			"if (someSig[0] == 1'b0)\n" +
			"(CLK => Q[15])=(1.000, 1.000);\n" +
			"endspecify\n" +
			"endmodule\n"
			;
		String expected =
				"module m;\n" +
				"\n" +
				"	specify\n" +
				"		if (someSig[0] == 1'b0)\n" +
				"			(CLK => Q[15])=(1.000, 1.000);\n" +
				"		if (someSig[0] == 1'b0)\n" +
				"			(CLK => Q[15])=(1.000, 1.000);\n" +
				"	endspecify\n" +
				"endmodule\n"
			;
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		fLog.debug("Result:");
		fLog.debug(result);
		IndentComparator.compare(getName(), expected, result);
	}	

	public void testSpecifySpecparam() {
		SVCorePlugin.getDefault().enableDebug(false);
		String content =
				"module m;\n" +
						"\n" +
						"specify\n" +
						"if (someSig[0] == 1'b0)\n" +
						"(CLK => Q[15])=(1.000, 1.000);\n" +
						"if (someSig[0] == 1'b0)\n" +
						"(CLK => Q[15])=(1.000, 1.000);\n" +
						"endspecify\n" +
						"endmodule\n"
						;
		String expected =
				"module m;\n" +
						"\n" +
						"	specify\n" +
						"		if (someSig[0] == 1'b0)\n" +
						"			(CLK => Q[15])=(1.000, 1.000);\n" +
						"		if (someSig[0] == 1'b0)\n" +
						"			(CLK => Q[15])=(1.000, 1.000);\n" +
						"	endspecify\n" +
						"endmodule\n"
						;
		
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		fLog.debug("Result:");
		fLog.debug(result);
		IndentComparator.compare(getName(), expected, result);
	}	
	


	public void testDefines() {
		SVCorePlugin.getDefault().enableDebug(false);
		String expected =
				"module bob ();\n" +
						"	initial\n" +
						"	begin\n" +
						"		// Comment1\n" +
						"		`A_MACRO_FUNCTION1 =\n" +
						"		asdf;\n" + // ?? Should be indented?
						"		// Comment2\n" +
						"		`A_MACRO_FUNCTION2 ();  // ; detected, unindents\n" +
						"		// Comment3\n" +
						"		`A_MACRO_FUNCTION3      // End of line, unindent\n" +
						"		// Comment4\n" +
						"		`A_MACRO_FUNCTION4 (argument)  // Parse through brace, unindent on ) if no ;\n" +
						"		// Comment5\n" +
						"		`A_MACRO_FUNCTION5 (\n" +
						"			argument\n" +
						"		)    // Parse through brace, unindent on ) if no ;\n" +
						"		// Comment6\n" +
						"		a = `A_MACRO_FUNCTION6 ()      // Parse through brace, unindent on ) if no ;\n" +
						"		// Comment7\n" +
						"		a = `A_MACRO_FUNCTION7         // Unindent on EOL\n" +
						"		// Comment8\n" +
						"		a = `A_MACRO_FUNCTION8 ||      // No EOL, carries on as normal\n" +
						"		something_else;\n" + // ?? Maybe this should be indented?
						"		// Comment9\n" +
						"	end\n" +
						"endmodule\n";
		StringBuilder content = IndentTests.removeLeadingWS(expected);
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content.toString()));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		fLog.debug("Result:");
		fLog.debug(result);
		IndentComparator.compare(getName(), expected, result);
	}	
	
	
	public void testClocking() {
		SVCorePlugin.getDefault().enableDebug(false);
		String content =
				"interface intfc ( input bit clk );\n" +
						"wire req, gnt;\n" +
						"wire [7:0] addr, data;\n" +
						"clocking sb @(posedge clk);\n" +
						"input gnt;\n" +
						"output req, addr;\n" +
						"inout data;\n" +
						"property p1; \n" +
						"req ##[1:3] gnt; \n" +
						"endproperty\n" +
						"endclocking\n" +
						"default clocking sb @(posedge clk);\n" +
						"input gnt;\n" +
						"output req, addr;\n" +
						"inout data;\n" +
						"property p1; \n" +
						"req ##[1:3] gnt; \n" +
						"endproperty\n" +
						"endclocking\n" +
						"modport STB ( clocking sb ); // synchronous testbench modport\n" +
						"endinterface\n" +
						"\n";
		String expected =
				"interface intfc ( input bit clk );\n" +
						"	wire req, gnt;\n" +
						"	wire [7:0] addr, data;\n" +
						"	clocking sb @(posedge clk);\n" +
						"		input gnt;\n" +
						"		output req, addr;\n" +
						"		inout data;\n" +
						"		property p1; \n" +
						"			req ##[1:3] gnt; \n" +
						"		endproperty\n" +
						"	endclocking\n" +
						"	default clocking sb @(posedge clk);\n" +
						"		input gnt;\n" +
						"		output req, addr;\n" +
						"		inout data;\n" +
						"		property p1; \n" +
						"			req ##[1:3] gnt; \n" +
						"		endproperty\n" +
						"	endclocking\n" +
						"	modport STB ( clocking sb ); // synchronous testbench modport\n" +
						"endinterface\n" +
						"\n";
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		fLog.debug("Result:");
		fLog.debug(result);
		IndentComparator.compare(getName(), expected, result);
	}	
	
	
	public void testInterfaceClass() {
		SVCorePlugin.getDefault().enableDebug(false);
		String content =
						"interface class test_class_interface\n" +
						"#(\n" +
						"type T = int,\n" +
						"type A = bit\n" +
						");\n" +
						"// provide prototype for signal change so that pve_sequencer can call it\n" +
						"pure virtual task method_to_be_added(uvm_component sender, uvm_object data_container);\n" +
						"endclass\n" +
						"\n" +
						"class pve_predictor #(type SEQHTYPE = pve_virtual_sequencer) extends uvm_component implements test_class_interface;\n" +
						"//blablabla\n" +
						"endclass\n" +
						"\n";
		String expected =
				"interface class test_class_interface\n" +
				"		#(\n" +
				"		type T = int,\n" +
				"		type A = bit\n" +
				"		);\n" +
				"	// provide prototype for signal change so that pve_sequencer can call it\n" +
				"	pure virtual task method_to_be_added(uvm_component sender, uvm_object data_container);\n" +
				"endclass\n" +
				"\n" +
				"class pve_predictor #(type SEQHTYPE = pve_virtual_sequencer) extends uvm_component implements test_class_interface;\n" +
				"	//blablabla\n" +
				"endclass\n" +
				"\n";
		SVIndentScanner scanner = new SVIndentScanner(
				new StringTextScanner(content));
		
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
		String result = indenter.indent();
		
		fLog.debug("Result:");
		fLog.debug(result);
		SVCorePlugin.getDefault().enableDebug(false);
		IndentComparator.compare(getName(), expected, result);
	}	
	
	
	
	public static void runTest(String name, LogHandle log, String doc) {
		StringBuilder sb = removeLeadingWS(doc);
		log.debug("Stripped content:\n" + sb.toString());
		SVIndentScanner scanner = new SVIndentScanner(new StringTextScanner(sb));
		ISVIndenter indenter = SVCorePlugin.getDefault().createIndenter();
		indenter.init(scanner);
		indenter.setTestMode(true);
		
//		indenter.setAdaptiveIndent(true);
//		indenter.setAdaptiveIndentEnd(5);
		String result = indenter.indent(-1, -1);
		
		log.debug("Ref:\n" + doc);
		log.debug("====");
		log.debug("Result:\n" + result);
		log.debug("====");
		
		IndentComparator.compare(log, name, doc, result);		
	}
}
