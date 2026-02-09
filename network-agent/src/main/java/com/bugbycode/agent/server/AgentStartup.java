package com.bugbycode.agent.server;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.bugbycode.agent.handler.AgentHandler;
import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.forward.client.StartupRunnable;
import com.bugbycode.mapper.host.HostMapper;
import com.bugbycode.mapper.table.TableMapper;
import com.bugbycode.service.testnet.TestnetService;
import com.bugbycode.webapp.pool.WorkTaskPool;

import io.netty.channel.EventLoopGroup;

@Component
@Configuration
public class AgentStartup implements ApplicationRunner {

	@Autowired
	private Map<String,AgentHandler> agentHandlerMap;
	
	@Autowired
	private Map<String,AgentHandler> forwardHandlerMap;
	
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
	private EventLoopGroup remoteGroup;
	
	@Autowired
	private TestnetService testnetService;
	
	@Autowired
	private WorkTaskPool workTaskPool;
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		
		tableMapper.initHostTable();
		
		StartupRunnable startup = new StartupRunnable(host, port,keystorePath,keystorePassword,forwardHandlerMap,remoteGroup); 
		
		new WorkTread(startup).start();
		
		AgentServer server = new AgentServer(agentPort, soBacklog, agentHandlerMap,forwardHandlerMap,nettyClientMap,
				startup,hostMapper,testnetService,workTaskPool);
		new Thread(server).start();
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
