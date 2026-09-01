package com.bugbycode.module.setting;

public class Setting {

	private int id;
	
	private int agentPort = 50000;//代理端口
	
	private int soBacklog = 1024;//代理服务最大连接数
	
	private int serverPort = 36500;//服务端口
	
	private String serverAddress = "localhost";//服务地址
	
	private int proxyStatus = 0;//代理启用状态 0：关闭 1：开启
	
	private String keystorePassword;//证书密码

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getAgentPort() {
		return agentPort;
	}

	public void setAgentPort(int agentPort) {
		this.agentPort = agentPort;
	}

	public int getSoBacklog() {
		return soBacklog;
	}

	public void setSoBacklog(int soBacklog) {
		this.soBacklog = soBacklog;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public int getProxyStatus() {
		return proxyStatus;
	}

	public void setProxyStatus(int proxyStatus) {
		this.proxyStatus = proxyStatus;
	}

	public String getKeystorePassword() {
		return keystorePassword;
	}

	public void setKeystorePassword(String keystorePassword) {
		this.keystorePassword = keystorePassword;
	}
	
}
