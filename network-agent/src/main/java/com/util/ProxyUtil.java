package com.util;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.module.SysOs;

public class ProxyUtil {

	private static final Logger logger = LogManager.getLogger(ProxyUtil.class);
	
	public static void setProxy(String host, int port) {
		if(OsUtil.getOsInfo() == SysOs.WINDOWS) {
			setWindowsProxy(host, port);
		}
	}
	
	public static void setWindowsProxy(String host, int port) {
		String proxy = "http://" + host + ":" + port;
		if(OsUtil.getOsInfo() == SysOs.WINDOWS) {
			try {
				// 使用 ProcessBuilder 执行 regedit 命令修改注册表
	            String command = "reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyEnable /t REG_DWORD /d 1 /f";
	            runCommand(command);

	            command = "reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyServer /t REG_SZ /d \"" + proxy + "\" /f";
	            runCommand(command);

	            logger.info("Setting proxy " + proxy + " success.");
	            
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
	
	private static void runCommand(String command) throws IOException {
		if(OsUtil.getOsInfo() == SysOs.WINDOWS) {
	        try {
	        	ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
		        processBuilder.inheritIO(); // 输出命令的控制台信息
		        Process process = processBuilder.start();
	            process.waitFor();
	        } catch (InterruptedException e) {
	        	logger.error(e.getMessage(), e);
	        }
		}
    }
}
