package com.bugbycode.module;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class Authentication implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 152770067471896521L;

	private String username;
	
	private String password;
	
	public Authentication() {
		
	}

	public Authentication(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public String toString() {
		JSONObject json = new JSONObject();
		try {
			json.put("username", username);
			json.put("password", password);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json.toString();
	}
}
