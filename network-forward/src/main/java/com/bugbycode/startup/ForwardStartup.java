package com.bugbycode.startup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.bugbycode.forward.server.ForwardServer;

@Component
@Configuration
public class ForwardStartup implements ApplicationRunner {

	@Value("${spring.netty.forwardPort}")
	private int forwardPort;
	
	@Value("${spring.netty.host}")
	private String host;
	
	@Value("${spring.netty.port}")
	private int port;
	
	@Value("${spring.netty.so_backlog}")
	private int soBacklog;
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		new Thread(new ForwardServer(forwardPort, host, port, soBacklog)).start();
	}

}
