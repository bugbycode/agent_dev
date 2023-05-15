package com.bugbycode.module;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class Message implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6372682599096032537L;
	
	private String token;
	
	private int type;
	
	private Object data;

	public Message() {
		
	}
	
	public Message(String token, int type, Object data) {
		this.token = token;
		this.type = type;
		this.data = data;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
	
	@Override
	public String toString() {
		JSONObject json = new JSONObject();
		try {
			json.put("type", type);
			json.put("data", data);
			json.put("token", token);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json.toString();
	}
}
