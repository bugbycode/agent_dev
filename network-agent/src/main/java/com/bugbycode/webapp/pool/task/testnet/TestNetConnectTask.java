package com.bugbycode.webapp.pool.task.testnet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.service.testnet.TestnetService;
import com.util.StringUtil;

/**
 * 测试连通性任务
 */
public class TestNetConnectTask implements Runnable {

	private final Logger logger = LogManager.getLogger(TestNetConnectTask.class);
	
	private TestnetService testnetService;
	
	private String url;
	
	private Runnable runTask;
	
	/**
	 * @param testnetService
	 * @param url 要测试的HTTP/HTTPS URL地址
	 * @param runTask 测试后执行的任务
	 */
	public TestNetConnectTask(TestnetService testnetService, String url, Runnable runTask) {
		this.testnetService = testnetService;
		this.url = url;
		this.runTask = runTask;
	}

	@Override
	public void run() {
		try {
			if(StringUtil.isNotEmpty(url)) {
				if(!testnetService.checkHttpConnect(url) && runTask != null) {
					runTask.run();
				}
			}
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage());
		}
	}
}