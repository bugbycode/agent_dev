package com.bugbycode.module;

import java.io.Serializable;


public class ConnectionInfo implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 9027795841206273339L;

	private String host;
	
	private int port;

	public ConnectionInfo() {
		
	}
	
	public ConnectionInfo(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String toString() {
		return host + ":" + port;
	}
	
}
