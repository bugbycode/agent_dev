package com.bugbycode.module;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class ConnectionInfo implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 9027795841206273339L;

	private int protocol;
	
	private String host;
	
	private int port;

	public ConnectionInfo() {
		
	}
	
	public ConnectionInfo(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public int getProtocol() {
		return protocol;
	}

	public void setProtocol(int protocol) {
		this.protocol = protocol;
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
		JSONObject json = new JSONObject();
		try {
			json.put("port", port);
			json.put("host", host);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json.toString();
	}
}
