package com.bugbycode.agent.server;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.conf.AppConfig;
import com.bugbycode.forward.client.StartupRunnable;
import com.bugbycode.mapper.host.HostMapper;
import com.bugbycode.mapper.setting.SettingMapper;
import com.bugbycode.mapper.table.TableMapper;
import com.bugbycode.mapper.user.UserMapper;
import com.bugbycode.module.setting.Setting;
import com.bugbycode.module.user.UserInfo;
import com.bugbycode.service.testnet.TestnetService;
import com.bugbycode.webapp.pool.WorkTaskPool;
import com.util.MD5Util;
import com.util.ProxyUtil;

@Component
@Configuration
public class AgentStartup implements ApplicationRunner {
	
	@Autowired
	private Map<String,NettyClient> nettyClientMap;
	
	@Autowired
	private TableMapper tableMapper;
	
	@Autowired
	private HostMapper hostMapper;
	
	@Autowired
	private UserMapper userMapper;
	
	@Autowired
	private SettingMapper settingMapper;
	
	@Autowired
	private TestnetService testnetService;
	
	@Autowired
	private WorkTaskPool workTaskPool;
	
	private final String USER_NAME = "admin";
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();
		boolean isDebugMode = arguments.stream().anyMatch(arg ->
	        arg.contains("-agentlib:jdwp")
	    );
		
		tableMapper.initHostTable();
		tableMapper.initUserTable();
		tableMapper.initSettingTable();
		
		UserInfo user = userMapper.loadUserByUsername(USER_NAME);
		if(user == null) {
			user = new UserInfo();
			user.setUsername(USER_NAME);
			user.setPassword(MD5Util.md5(USER_NAME));
			userMapper.insert(user);
		}
		
		Setting s = settingMapper.getSetting();
		if(s != null) {
			AppConfig.AGENT_PORT = s.getAgentPort();
			AppConfig.SO_BACK_LOG = s.getSoBacklog();
			AppConfig.SERVER_PORT = s.getServerPort();
			AppConfig.SERVER_ADDRESS = s.getServerAddress();
			AppConfig.PROXY_STATUS = s.getProxyStatus();
			AppConfig.KEYSTORE_PASSWORD = s.getKeystorePassword();
		}

		if(isDebugMode) {
			//return;
		}
		
		AppConfig.START_UP = new StartupRunnable(nettyClientMap); 
		
		new WorkTread(AppConfig.START_UP).start();
		
		AppConfig.SERVER = new AgentServer(nettyClientMap, AppConfig.START_UP, hostMapper, testnetService, workTaskPool);
		new Thread(AppConfig.SERVER).start();
		
		ProxyUtil.setProxy("localhost", AppConfig.AGENT_PORT, AppConfig.PROXY_STATUS);
		
	}

	private class WorkTread extends Thread{

		StartupRunnable startup;
		
		public WorkTread(StartupRunnable startup) {
			this.startup = startup;
		}
		
		@Override
		public void run() {
			while(true) {
				if(!(this.startup.isOpen() || this.startup.starting())) {
					this.startup.run();
				}
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
	} 
}
