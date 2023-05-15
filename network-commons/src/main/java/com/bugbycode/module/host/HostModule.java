package com.bugbycode.module.host;

import java.io.Serializable;
import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;


public class HostModule implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3888808871401284011L;
	
	private int id;
	
	private String host; //地址
	
	private int forward; //是否转发
	
	private int result; //访问结果

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date connTime; //最近访问时间

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getForward() {
		return forward;
	}

	public void setForward(int forward) {
		this.forward = forward;
	}

	public int getResult() {
		return result;
	}

	public void setResult(int result) {
		this.result = result;
	}

	public Date getConnTime() {
		return connTime;
	}

	public void setConnTime(Date connTime) {
		this.connTime = connTime;
	}

}
