package com.bugbycode.conf;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.bugbycode.agent.handler.AgentHandler;
import com.bugbycode.client.startup.NettyClient;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

@Configuration
public class AppConfig {
	
	public static final int WORK_THREAD_NUMBER = 100;
	
	@Bean("channelGroup")
	public ChannelGroup getChannelGroup() {
		return new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	}
	
	@Bean
	public Map<String,NettyClient> nettyClientMap(){
		return new Hashtable<String,NettyClient>();
	}
	
	@Bean
	public Map<String,AgentHandler> agentHandlerMap(){
		return new Hashtable<String,AgentHandler>();
	}
	
	@Bean
	public Map<String,AgentHandler> forwardHandlerMap(){
		return new Hashtable<String,AgentHandler>();
	}
	
	@Bean
	public NioEventLoopGroup remoteGroup() {
		return new NioEventLoopGroup();
	}
	
	@Bean
	public RestTemplate restTemplate() {
		
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        restTemplate.setErrorHandler(new HttpResponseErrorHandler());
        
        return restTemplate;
	}
	
	private class HttpResponseErrorHandler implements ResponseErrorHandler{
		
		private final Logger logger = LogManager.getLogger(HttpResponseErrorHandler.class);
		
		@Override
		public boolean hasError(ClientHttpResponse response) throws IOException {
			return response.getStatusCode().series() == HttpStatus.Series.CLIENT_ERROR 
		               || response.getStatusCode().series() == HttpStatus.Series.SERVER_ERROR;
		}
		
		@Override
		public void handleError(ClientHttpResponse response) throws IOException {
			logger.error("Error response received with status code: " + response.getStatusCode());
		}
	}
}
