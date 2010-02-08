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


package net.sf.sveditor.core.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum SVKeywords {
	
	KW_alias("alias"),
	KW_always("always"),
	KW_always_comb("always_comb"),
    KW_always_ff("always_ff"),
	KW_alway_latch("always_latch"),
    KW_and("and"),
	KW_assert("assert"),
    KW_assign("assign"),
	KW_assume("assume"),
	KW_automatic("automatic"),
	KW_before("before"),
	KW_begin("begin"),
	KW_bind("bind"),
	KW_bins("bins"),
	KW_binsof("binsof"),
	KW_bit("bit"),
	KW_break("break"),
	KW_buf("buf"),
	KW_bufif0("bufif0"),
	KW_bufif1("bufif1"),
	KW_byte("byte"),
	KW_case("case"),
	KW_casex("casex"),
	KW_casez("casez"),
	KW_cell("cell"),
	KW_chandle("chandle"),
	KW_class("class"),
	KW_clocking("clocking"),
	KW_cmos("cmos"),
	KW_config("config"),
	KW_const("const"),
	KW_constraint("constraint"),
	KW_context("context"),
	KW_continue("continue"),
	KW_cover("cover"),
	KW_covergroup("covergroup"),
	KW_coverpoint("coverpoint"),
	KW_cross("cross"),
	KW_deassign("deassign"),
	KW_default("default"),
	KW_defparam("defparam"),
	KW_design("design"),
	KW_disable("disable"),
	KW_dist("dist"),
	KW_do("do"),
	KW_edge("edge"),
	KW_else("else"),
	KW_end("end"),
	KW_endcase("endcase"),
	KW_endclass("endclass"),
	KW_endclocking("endclocking"),
	KW_endconfig("endconfig"),
	KW_endfunction("endfunction"),
	KW_endgenerate("endgenerate"),
	KW_endgroup("endgroup"),
	KW_endinterface("endinterface"),
	KW_endmodule("endmodule"),
	KW_endpackage("endpackage"),
	KW_endprimitive("endprimitive"),
	KW_endprogram("endprogram"),
	KW_endproperty("endproperty"),
	KW_endspecify("endspecify"),
	KW_endsequence("endsequence"),
	KW_endtable("endtable"),
	KW_endtask("endtask"),
	KW_enum("enum"),
	KW_event("event"),
	KW_expect("expect"),
	KW_export("export"),
	KW_extends("extends"),
	KW_extern("extern"),
	KW_final("final"),
	KW_first_match("first_match"),
	KW_for("for"),
	KW_force("force"),
	KW_foreach("foreach"),
	KW_forever("forever"),
	KW_fork("fork"),
	KW_forkjoin("forkjoin"),
	KW_function("function"),
	KW_generate("generate"),
	KW_genvar("genvar"),
	KW_highz0("highz0"),
	KW_highz1("highz1"),
	KW_if("if"),
	KW_iff("iff"),
	KW_ifnone("ifnone"),
	KW_ignore_bins("ignore_bins"),
	KW_illegal_bins("illegal_bins"),
	KW_import("import"),
	KW_incdir("incdir"),
	KW_include("include"),
	KW_initial("initial"),
	KW_inout("inout"),
	KW_input("input"),
	KW_inside("inside"),
	KW_instance("instance"),
	KW_int("int"),
	KW_integer("integer"),
	KW_interface("interface"),
	KW_intersect("intersect"),
	KW_join("join"),
	KW_join_any("join_any"),
	KW_join_none("join_none"),
	KW_large("large"),
	KW_libset("liblist"),
	KW_library("library"),
	KW_local("local"),
	KW_localparam("localparam"),
	KW_logic("logic"),
	KW_longint("longint"),
	KW_macromodule("macromodule"),
	KW_matches("matches"),
	KW_medium("medium"),
	KW_modport("modport"),
	KW_module("module"),
	KW_nand("nand"),
	KW_negedge("negedge"),
	KW_new("new"),
	KW_nmos("nmos"),
	KW_nor("nor"),
	KW_noshowcancelled("noshowcancelled"),
	KW_not("not"),
	KW_notif0("notif0"),
	KW_notif1("notif1"),
	KW_null("null"),
	KW_or("or"),
	KW_output("output"),
	KW_package("package"),
	KW_packed("packed"),
	KW_parameter("parameter"),
	KW_pmos("pmos"),
	KW_posedge("posedge"),
	KW_primitive("primitive"),
	KW_priority("priority"),
	KW_program("program"),
	KW_property("property"),
	KW_protected("protected"),
	KW_pull0("pull0"),
	KW_pull1("pull1"),
	KW_pulldown("pulldown"),
	KW_pullup("pullup"),
	KW_pulsestyle_onevent("pulsestyle_onevent"),
	KW_pulsestyle_ondetect("pulsestyle_ondetect"),
	KW_pure("pure"),
	KW_rand("rand"),
	KW_randc("randc"),
	KW_randcase("randcase"),
	KW_randsequence("randsequence"),
	KW_rcmos("rcmos"),
	KW_real("real"),
	KW_realtime("realtime"),
	KW_ref("ref"),
	KW_reg("reg"),
	KW_release("release"),
	KW_repeat("repeat"),
	KW_return("return"),
	KW_rnmos("rnmos"),
	KW_rpmos("rpmos"),
	KW_rtran("rtran"),
	KW_rtranif0("rtranif0"),
	KW_rtranif1("rtranif1"),
	KW_scalared("scalared"),
	KW_sequence("sequence"),
	KW_shortint("shortint"),
	KW_shortreal("shortreal"),
	KW_showcancelled("showcancelled"),
	KW_signed("signed"),
	KW_small("small"),
	KW_solve("solve"),
	KW_specify("specify"),
	KW_specparam("specparam"),
	KW_static("static"),
	KW_string("string"),
	KW_strong0("strong0"),
	KW_strong1("strong1"),
	KW_struct("struct"),
	KW_super("super"),
	KW_supply0("supply0"),
	KW_supply1("supply1"),
	KW_table("table"),
	KW_tagged("tagged"),
	KW_task("task"),
	KW_this("this"),
	KW_throughout("throughout"),
	KW_time("time"),
	KW_timeprecission("timeprecision"),
	KW_timeunit("timeunit"),
	KW_tran("tran"),
	KW_tranif0("tranif0"),
	KW_tranif1("tranif1"),
	KW_tri("tri"),
	KW_tri0("tri0"),
	KW_tri1("tri1"),
	KW_triand("triand"),
	KW_trior("trior"),
	KW_trireg("trireg"),
	KW_type("type"),
	KW_typedef("typedef"),
	KW_union("union"),
	KW_unique("unique"),
	KW_unsigned("unsigned"),
	KW_use("use"),
	KW_uwire("uwire"),
	KW_var("var"),
	KW_vectored("vectored"),
	KW_virtual("virtual"),
	KW_void("void"),
	KW_wait("wait"),
	KW_wait_order("wait_order"),
	KW_wand("wand"),
	KW_weak0("weak0"),
	KW_weak1("weak1"),
	KW_while("while"),
	KW_wildcard("wildcard"),
	KW_wire("wire"),
	KW_with("with"),
	KW_within("within"),
	KW_wor("wor"),
	KW_xnor("xnor"),
	KW_xor("xor");
	
	public static final Map<String, SVKeywords> 		fKeywords;
	
	static {
		fKeywords = new HashMap<String, SVKeywords>();
		
		for (SVKeywords kw : values()) {
			fKeywords.put(kw.fImage, kw);
		}
	}
	
	public static boolean isKeyword(String s) {
		return fKeywords.containsKey(s);
	}
	
	public static Set<String> getKeywords() {
		return fKeywords.keySet();
	}
	
	public static SVKeywords getKeyword(String s) {
		return fKeywords.get(s);
	}
	
	private String				fImage;
	
	SVKeywords(String img) {
		fImage = img;
	}
	
	public String valueOf() {
		return fImage;
	}
	
}
