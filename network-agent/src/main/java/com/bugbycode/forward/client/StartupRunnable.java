package com.bugbycode.forward.client;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.agent.handler.AgentHandler;
import com.bugbycode.config.HandlerConst;
import com.bugbycode.config.IdleConfig;
import com.bugbycode.forward.handler.ClientHandler;
import com.bugbycode.handler.MessageDecoder;
import com.bugbycode.handler.MessageEncoder;
import com.util.ssl.SSLContextFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;

public class StartupRunnable implements Runnable {

	private final Logger logger = LogManager.getLogger(StartupRunnable.class);
	
	private String host;
	
	private int port;
	
	private String keyStorePath;
	
	private String keyStorePassword;
	
	private Map<String,AgentHandler> agentHandlerMap;
	
	private Channel clientChannel;
	
	private NioEventLoopGroup remoteGroup;
	
	private boolean starting = false;
	
	public StartupRunnable(String host, int port,String keyStorePath,String keyStorePassword,
			Map<String,AgentHandler> agentHandlerMap,NioEventLoopGroup remoteGroup) {
		this.host = host;
		this.port = port;
		this.keyStorePath = keyStorePath;
		this.keyStorePassword = keyStorePassword;
		this.agentHandlerMap = agentHandlerMap;
		this.remoteGroup = remoteGroup;
	}

	@Override
	public void run() {
		starting = true;
		Bootstrap client = new Bootstrap();
		client.group(this.remoteGroup).channel(NioSocketChannel.class);
		client.option(ChannelOption.TCP_NODELAY, true);
		client.option(ChannelOption.SO_KEEPALIVE, true);
		client.handler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				SSLEngine engine = SSLContextFactory.getContext(keyStorePath, keyStorePassword).createSSLEngine();
		        engine.setUseClientMode(true);
		        ch.pipeline().addLast(new SslHandler(engine));
		        
				ch.pipeline().addLast(new IdleStateHandler(IdleConfig.READ_IDEL_TIME_OUT,
						IdleConfig.WRITE_IDEL_TIME_OUT,
						IdleConfig.ALL_IDEL_TIME_OUT, TimeUnit.SECONDS));
				 ch.pipeline().addLast(new MessageDecoder(HandlerConst.MAX_FRAME_LENGTH, HandlerConst.LENGTH_FIELD_OFFSET, 
							HandlerConst.LENGTH_FIELD_LENGTH, HandlerConst.LENGTH_AD_JUSTMENT, 
							HandlerConst.INITIAL_BYTES_TO_STRIP));
				 ch.pipeline().addLast(new MessageEncoder());
				 ch.pipeline().addLast(new ClientHandler(StartupRunnable.this,agentHandlerMap));
			}
			
		});
		client.connect(host, port).addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					clientChannel = future.channel();
					logger.info("Connection to " + host + ":" + port + " success...");
				} else{
					logger.error("Connection to " + host + ":" + port + " failed...");
				}
				starting = false;
			}
		});
	}
	
	public void restart() {
		this.run();
	}
	
	public boolean starting() {
		return this.starting;
	}
	
	public void writeAndFlush(Object msg) {
		if(isOpen()) {
			this.clientChannel.writeAndFlush(msg);
		}else {
			throw new RuntimeException("Unconnected forward server");
		}
	}
	
	public boolean isOpen() {
		return this.clientChannel != null && this.clientChannel.isOpen();
	}
}
