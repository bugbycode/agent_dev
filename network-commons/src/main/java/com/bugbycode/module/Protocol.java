package com.bugbycode.module;

public enum Protocol {
	FTP(1,"FTP"),
	HTTP(0,"HTTP"),
	HTTPS(2,"HTTPS"),
	SOCKET_4(4,"SOCKET_4"),
	SOCKET_5(5,"SOCKET_5"),;

	private int value;
	private String label;
	
	Protocol(int value, String label) {
		this.value = value;
		this.label = label;
	}

	public int getValue() {
		return value;
	}

	public String getLabel() {
		return label;
	}

	public static Protocol resolve(int value) {
		Protocol[] arr = values();
		for(Protocol p : arr) {
			if(p.getValue() == value) {
				return p;
			}
		}
		return HTTP;
	};
}
