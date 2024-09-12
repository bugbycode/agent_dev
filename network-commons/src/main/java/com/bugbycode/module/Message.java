package com.bugbycode.module;

import java.io.Serializable;

public class Message implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6372682599096032537L;
	
	private String token;
	
	private MessageType type;
	
	private Object data;

	public Message() {
		
	}
	
	public Message(String token, MessageType type, Object data) {
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

	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
	
	public MessageType resolve(int typeValue) {
		MessageType[] arr = MessageType.values();
		MessageType result = null;
		for(MessageType mt : arr) {
			if(mt.getValue() == typeValue) {
				result = mt;
				break;
			}
		}
		if(result == null) {
			throw new RuntimeException("MessageType error.");
		}
		return result;
	}

	@Override
	public String toString() {
		return "Message [token=" + token + ", type=" + type.getLabel() + ", data=" + data + "]";
	}
}
