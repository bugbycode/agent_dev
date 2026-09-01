package com.bugbycode.conf;

import java.io.IOException;
import java.net.URI;
import java.util.Hashtable;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.bugbycode.agent.server.AgentServer;
import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.forward.client.StartupRunnable;
import com.bugbycode.webapp.pool.WorkTaskPool;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

@Configuration
public class AppConfig {
	
	public static final String DEFAULT_USER_NAME = "admin";
	
	public static int AGENT_PORT = 50000;
	
	public static int SO_BACK_LOG = 1024;
	
	public static int SERVER_PORT = 36500;
	
	public static String SERVER_ADDRESS = "localhost";
	
	public static int PROXY_STATUS = 0;
	
	public static String KEYSTORE_PASSWORD = "";
	
	public static StartupRunnable START_UP = null;
	
	public static AgentServer SERVER = null;
	
	@Bean("channelGroup")
	public ChannelGroup getChannelGroup() {
		return new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	}
	
	@Bean
	public Map<String,NettyClient> nettyClientMap(){
		return new Hashtable<String,NettyClient>();
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
			return response.getStatusCode().value() == HttpStatus.Series.CLIENT_ERROR.value() 
		               || response.getStatusCode().value() == HttpStatus.Series.SERVER_ERROR.value();
		}
		
		@Override
		public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
			logger.error("Error response received with status code: " + response.getStatusCode());
		}
	}
	
	@Bean
	public WorkTaskPool workTaskPool() {
		return new WorkTaskPool("SqliteDataBaseWorkThread",1);
	}
}
