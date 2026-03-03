package com.bugbycode.agent.server;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.agent.handler.AgentHandler;
import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.config.IdleConfig;
import com.bugbycode.forward.client.StartupRunnable;
import com.bugbycode.mapper.host.HostMapper;
import com.bugbycode.service.testnet.TestnetService;
import com.bugbycode.webapp.pool.WorkTaskPool;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

public class AgentServer implements Runnable {
	
	private final Logger logger = LogManager.getLogger(AgentServer.class);

	private int agentPort = 0;
	
	private int soBacklog;
	
	private EventLoopGroup boss;
	
	private EventLoopGroup worker;
	
	private Map<String,NettyClient> nettyClientMap;
	
	private StartupRunnable startup;
	
	private HostMapper hostMapper;
	
	private TestnetService testnetService;
	
	private WorkTaskPool workTaskPool;
	
	public AgentServer(int agentPort,
			int soBacklog,
			Map<String,NettyClient> nettyClientMap,
			StartupRunnable startup,
			HostMapper hostMapper,TestnetService testnetService,
			WorkTaskPool workTaskPool) {
		this.agentPort = agentPort;
		this.soBacklog = soBacklog;
		this.nettyClientMap = nettyClientMap;
		this.startup = startup;
		this.hostMapper = hostMapper;
		this.testnetService = testnetService;
		this.workTaskPool = workTaskPool;
	}
	
	@Override
	public void run() {
		ServerBootstrap bootstrap = new ServerBootstrap();
		boss = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
		worker = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
		bootstrap.group(boss, worker).channel(NioServerSocketChannel.class)
		.option(ChannelOption.SO_BACKLOG, soBacklog)
		.option(ChannelOption.TCP_NODELAY, true)
		.option(ChannelOption.SO_KEEPALIVE, true)
		.option(ChannelOption.SO_REUSEADDR, true)
		.childOption(ChannelOption.TCP_NODELAY, true)
		.childOption(ChannelOption.SO_KEEPALIVE, true)
		.childOption(ChannelOption.SO_REUSEADDR, true)
		.childHandler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(
						new IdleStateHandler(IdleConfig.READ_IDEL_TIME_OUT, IdleConfig.WRITE_IDEL_TIME_OUT, IdleConfig.ALL_IDEL_TIME_OUT),
						new AgentHandler(nettyClientMap,startup,hostMapper,
						testnetService,workTaskPool));
			}
		});
		
		bootstrap.bind(agentPort).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					logger.info("Agent server startup success, port " + agentPort + ", soBacklog " + soBacklog + " ......");
				} else {
					future.cause().printStackTrace();
					logger.info("Agent server startup failed, port " + agentPort + "......");
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
		
		logger.info("Agent server shutdown, port " + agentPort + "......");
	}

}
