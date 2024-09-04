package com.bugbycode.agent.server;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.agent.handler.AgentHandler;
import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.config.HandlerConst;
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
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class AgentServer implements Runnable {
	
	private final Logger logger = LogManager.getLogger(AgentServer.class);

	private int agentPort = 0;
	
	private EventLoopGroup boss;
	
	private EventLoopGroup worker;
	
	private Map<String,AgentHandler> agentHandlerMap;
	
	private Map<String,AgentHandler> forwardHandlerMap;
	
	private Map<String,NettyClient> nettyClientMap;
	
	private StartupRunnable startup;
	
	private HostMapper hostMapper;
	
	private TestnetService testnetService;
	
	private WorkTaskPool workTaskPool;
	
	public AgentServer(int agentPort,Map<String,AgentHandler> agentHandlerMap,
			Map<String,AgentHandler> forwardHandlerMap,
			Map<String,NettyClient> nettyClientMap,
			StartupRunnable startup,
			HostMapper hostMapper,TestnetService testnetService,
			WorkTaskPool workTaskPool) {
		this.agentPort = agentPort;
		this.agentHandlerMap = agentHandlerMap;
		this.forwardHandlerMap = forwardHandlerMap;
		this.nettyClientMap = nettyClientMap;
		this.startup = startup;
		this.hostMapper = hostMapper;
		this.testnetService = testnetService;
		this.workTaskPool = workTaskPool;
	}
	
	@Override
	public void run() {
		ServerBootstrap bootstrap = new ServerBootstrap();
		boss = new NioEventLoopGroup();
		worker = new NioEventLoopGroup();
		bootstrap.group(boss, worker).channel(NioServerSocketChannel.class)
		.option(ChannelOption.TCP_NODELAY, true)
		.option(ChannelOption.SO_KEEPALIVE, true)
		.childOption(ChannelOption.TCP_NODELAY, true)
		.childOption(ChannelOption.SO_KEEPALIVE, true)
		.childHandler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(HandlerConst.MAX_FRAME_LENGTH));
				ch.pipeline().addLast(new AgentHandler(agentHandlerMap,
						forwardHandlerMap,nettyClientMap,startup,hostMapper,
						testnetService,workTaskPool));
			}
		});
		
		bootstrap.bind(agentPort).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					logger.info("Agent server startup successfully, port " + agentPort + "......");
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
