package com.bugbycode.webapp.pool.task.host;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.mapper.host.HostMapper;
import com.bugbycode.module.host.HostModule;

/**
 * 添加服务地址信息
 */
public class InsertHostTask implements Runnable {

	private final Logger logger = LogManager.getLogger(InsertHostTask.class);
	
	private HostMapper hostMapper;
	
	private HostModule hostModule;
	
	public InsertHostTask(HostMapper hostMapper, HostModule hostModule) {
		this.hostMapper = hostMapper;
		this.hostModule = hostModule;
	}

	@Override
	public void run() {
		try {
			HostModule hm = hostMapper.queryByHost(hostModule.getHost());
			if(hm == null) {
				hostMapper.insert(hostModule);
			}
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage());
		}
	}

}
