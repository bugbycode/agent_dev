package com.bugbycode.exception;

/**
 * 自定义异常类
 */
public class AgentException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	public AgentException(String message) {
		super(message);
	}
}
