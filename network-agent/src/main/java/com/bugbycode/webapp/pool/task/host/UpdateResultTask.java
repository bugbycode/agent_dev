package com.bugbycode.webapp.pool.task.host;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.mapper.host.HostMapper;

/**
 * 更新访问结果任务
 */
public class UpdateResultTask implements Runnable {

	private final Logger logger = LogManager.getLogger(UpdateResultTask.class);
	
	private HostMapper hostMapper;
	
	private String host; //地址
	
	private int result; //访问结果
	
	private Date connTime; //最近访问时间
	
	/**
	 * 
	 * @param hostMapper
	 * @param host 域名
	 * @param result 访问结果 0 失败 1 成功
	 * @param connTime 访问时间
	 */
	public UpdateResultTask(HostMapper hostMapper, String host, int result, Date connTime) {
		this.hostMapper = hostMapper;
		this.host = host;
		this.result = result;
		this.connTime = connTime;
	}

	@Override
	public void run() {
		try {
			hostMapper.updateResultDatetimeByHost(host, result, connTime);
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage());
		}
	}

}
