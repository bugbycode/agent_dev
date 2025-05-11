package com.bugbycode.forward.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.config.IdleConfig;
import com.bugbycode.forward.handler.ForwardHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

public class ForwardServer implements Runnable {

	private final Logger logger = LogManager.getLogger(ForwardServer.class);
	
	private int forwardPort = 0;
	
	private int soBacklog;
	
	private EventLoopGroup boss;
	
	private EventLoopGroup worker;
	
	private String host;
	
	private int port;
	
	public ForwardServer(int forwardPort, String host, int port, int soBacklog) {
		this.forwardPort = forwardPort;
		this.host = host;
		this.port = port;
		this.soBacklog = soBacklog;
	}
	
	@Override
	public void run() {
		ServerBootstrap bootstrap = new ServerBootstrap();
		boss = new NioEventLoopGroup();
		worker = new NioEventLoopGroup();
		bootstrap.group(boss, worker).channel(NioServerSocketChannel.class)
		.option(ChannelOption.SO_REUSEADDR, true)
		.option(ChannelOption.SO_BACKLOG, soBacklog)
		.option(ChannelOption.TCP_NODELAY, true)
		.option(ChannelOption.SO_KEEPALIVE, true)
		.childOption(ChannelOption.TCP_NODELAY, true)
		.childOption(ChannelOption.SO_KEEPALIVE, true)
		.childOption(ChannelOption.SO_REUSEADDR, true)
		.childHandler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(
						new IdleStateHandler(IdleConfig.READ_IDEL_TIME_OUT, IdleConfig.WRITE_IDEL_TIME_OUT, IdleConfig.ALL_IDEL_TIME_OUT),
						new ForwardHandler(host, port));
			}
			
		});
		
		bootstrap.bind(forwardPort).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					logger.info("Forward server startup success, port {}, soBacklog {} ......", forwardPort, soBacklog);
				} else {
					future.cause().printStackTrace();
					logger.info("Forward server startup failed, port {}......", forwardPort);
					close();
				}
			}
			
		});
	}
	
	public void close() {
		
		if(boss != null) {
			boss.shutdownGracefully();
		}
		
		if(worker != null) {
			worker.shutdownGracefully();
		}
		
		logger.info("Forward server shutdown, port {}......", forwardPort);
	}

}
