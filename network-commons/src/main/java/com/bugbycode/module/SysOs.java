package com.bugbycode.module;

public enum SysOs {

	UNKNOWN(""),
	
	WINDOWS("win"),
	
	LINUX("linux");

	private String value;
	
	SysOs(String value) {
		this.value = value;
	}
	
	public String value() {
		return this.value;
	}
	
	public static SysOs parse(String s) {
		SysOs result = UNKNOWN;
		SysOs[] os = SysOs.values();
		for(SysOs o : os) {
			if(o == UNKNOWN) {
				continue;
			}
			if(s.toLowerCase().contains(o.value())) {
				return o;
			}
		}
		return result;
	}
}
