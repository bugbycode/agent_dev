package com.bugbycode.module;

public enum MessageType {
	
	HEARTBEAT(0,"心跳通信"),
	CONNECTION(1,"连接目标设备"),
	CONNECTION_SUCCESS(2,"连接成功"),
	CONNECTION_ERROR(3,"连接失败"),
	TRANSFER_DATA(4,"流量转发"),
	CLOSE_CONNECTION(5,"断开与目标设备的连接"),;

	private int value;
	
	private String label;
	
	MessageType(int value, String label) {
		this.value = value;
		this.label = label;
	}

	public int getValue() {
		return value;
	}

	public String getLabel() {
		return label;
	}
	
	
}
