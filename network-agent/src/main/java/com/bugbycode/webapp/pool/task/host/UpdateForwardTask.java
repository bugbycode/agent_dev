package com.bugbycode.webapp.pool.task.host;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.mapper.host.HostMapper;

/**
 * 修改转发信息
 */
public class UpdateForwardTask implements Runnable {

	private final Logger logger = LogManager.getLogger(UpdateForwardTask.class);
	
	private String host; //地址
	
	private int forward; //是否转发
	
	private HostMapper hostMapper;
	
	/**
	 * 
	 * @param host 域名
	 * @param forward 是否转发 0 否 1 是
	 * @param hostMapper
	 */
	public UpdateForwardTask(String host, int forward, HostMapper hostMapper) {
		this.host = host;
		this.forward = forward;
		this.hostMapper = hostMapper;
	}

	@Override
	public void run() {
		try {
			hostMapper.updateForwardByHost(host, forward);
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage());
		}
	}

}
