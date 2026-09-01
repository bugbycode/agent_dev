package com.bugbycode.agent.server;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.forward.client.StartupRunnable;
import com.bugbycode.mapper.host.HostMapper;
import com.bugbycode.mapper.table.TableMapper;
import com.bugbycode.mapper.user.UserMapper;
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
	
	@Value("${spring.keystore.path:client.keystore}")
	private String keystorePath = "";
	
	@Value("${spring.keystore.password:changeit}")
	private String keystorePassword = "";
	
	@Value("${spring.netty.auth.host}")
	private String host;
	
	@Value("${spring.netty.auth.port}")
	private int port;
	
	@Value("${spring.netty.agent.port}")
	private int agentPort;
	
	@Value("${spring.netty.agent.so_backlog}")
	private int soBacklog;
	
	@Autowired
	private TableMapper tableMapper;
	
	@Autowired
	private HostMapper hostMapper;
	
	@Autowired
	private UserMapper userMapper;
	
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
		
		UserInfo user = userMapper.loadUserByUsername(USER_NAME);
		if(user == null) {
			user = new UserInfo();
			user.setUsername(USER_NAME);
			user.setPassword(MD5Util.md5(USER_NAME));
			userMapper.insert(user);
		}

		if(isDebugMode) {
			return;
		}
		
		StartupRunnable startup = new StartupRunnable(host, port,keystorePath, keystorePassword, nettyClientMap); 
		
		new WorkTread(startup).start();
		
		AgentServer server = new AgentServer(agentPort, soBacklog, nettyClientMap,
				startup,hostMapper,testnetService,workTaskPool);
		new Thread(server).start();
		
		ProxyUtil.setProxy("localhost", agentPort);
		
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
