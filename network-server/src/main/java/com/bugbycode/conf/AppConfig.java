package com.bugbycode.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.netty.channel.nio.NioEventLoopGroup;

@Configuration
public class AppConfig {
	
	@Value("${spring.netty.nThreads}")
	private int nThreads;
	
	@Bean
	public NioEventLoopGroup remoteGroup() {
		return new NioEventLoopGroup(nThreads);
	}
}
