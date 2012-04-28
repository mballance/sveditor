package net.sf.sveditor.core.templates;

public class TemplateParameter {
	private TemplateParameterType			fType;
	private String							fName;
	private String							fDefault;
	private String							fValue;
	private String							fExtFrom;
	
	public TemplateParameter(
			TemplateParameterType		type,
			String						name,
			String						dflt,
			String						ext_from) {
		fType 		= type;
		fName 		= name;
		fDefault 	= dflt;
		fValue		= dflt;
		fExtFrom	= ext_from;
	}
	
	public TemplateParameterType getType() {
		return fType;
	}
	
	public String getName() {
		return fName;
	}
	
	public String getDefault() {
		return fDefault;
	}
	
	public String getValue() {
		return fValue;
	}
	
	public void setValue(String val) {
		fValue = val;
	}

	public String getExtFrom() {
		return fExtFrom;
	}
	
	public TemplateParameter duplicate() {
		TemplateParameter p = new TemplateParameter(fType, fName, fDefault, fExtFrom);
		p.setValue(fValue);
		
		return p;
	}
}

