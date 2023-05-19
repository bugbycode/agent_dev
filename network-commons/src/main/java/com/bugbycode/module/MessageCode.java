package com.bugbycode.module;

public abstract class MessageCode {
	
	public final static int HEARTBEAT = 0; 	//心跳通信
	
	public final static int CONNECTION = 1;	//连接目标设备
	
	public final static int CONNECTION_SUCCESS = 2; //连接成功
	
	public final static int CONNECTION_ERROR = 3; //连接失败
	
	public final static int TRANSFER_DATA = 4; //流量转发
	
	public final static int CLOSE_CONNECTION = 5; //断开与目标设备的连接
}
