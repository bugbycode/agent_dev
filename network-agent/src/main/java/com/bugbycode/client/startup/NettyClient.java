package com.bugbycode.client.startup;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.agent.handler.AgentHandler;
import com.bugbycode.client.handler.ClientHandler;
import com.bugbycode.config.IdleConfig;
import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageType;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

public class NettyClient {
	
	private final Logger logger = LogManager.getLogger(NettyClient.class);
	
	private Bootstrap bs;
	
	private EventLoopGroup workGroup;
	
	private Map<String,NettyClient> nettyClientMap;
	
	private Map<String,AgentHandler> agentHandlerMap;
	
	private String token = "";
	
	private Channel clientChannel;
	
	private ConnectionInfo conn;
	
	public NettyClient(Message msg,Map<String,NettyClient> nettyClientMap,
			Map<String,AgentHandler> agentHandlerMap) {
		this.bs = new Bootstrap();
		this.workGroup = new NioEventLoopGroup();
		this.nettyClientMap = nettyClientMap;
		this.nettyClientMap.put(msg.getToken(), this);
		this.agentHandlerMap = agentHandlerMap;
		this.token = msg.getToken();
		conn = (ConnectionInfo) msg.getData();
	}
	
	public void connection() {
		
		this.bs.group(workGroup).channel(NioSocketChannel.class);
		this.bs.option(ChannelOption.SO_REUSEADDR, true);
		this.bs.option(ChannelOption.TCP_NODELAY, true);
		this.bs.option(ChannelOption.SO_KEEPALIVE, true);
		this.bs.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new IdleStateHandler(IdleConfig.READ_IDEL_TIME_OUT, IdleConfig.WRITE_IDEL_TIME_OUT,
						IdleConfig.ALL_IDEL_TIME_OUT, TimeUnit.SECONDS));
				ch.pipeline().addLast(new ClientHandler(agentHandlerMap,token,NettyClient.this));
			}
		});
		
		this.bs.connect(conn.getHost(), conn.getPort()).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				Message message = new Message(token, MessageType.CONNECTION_SUCCESS, null);
				if(future.isSuccess()) {
					logger.info("Connection to " + conn + " success.");
					message.setType(MessageType.CONNECTION_SUCCESS);
					clientChannel = future.channel();
				}else {
					nettyClientMap.remove(token);
					logger.info("Connection to " + conn + " failed.");
					message.setType(MessageType.CONNECTION_ERROR);
				}
				AgentHandler handler = agentHandlerMap.get(token);
				if(handler != null) {
					handler.sendMessage(message);
				}
			}
		});
	}
	
	public void writeAndFlush(byte[] data) {
		if(clientChannel == null) {
			return;
		}
		ByteBuf buff = clientChannel.alloc().buffer(data.length);
		buff.writeBytes(data);
		clientChannel.writeAndFlush(buff);
	}
	
	public void close() {
		
		if(clientChannel != null && clientChannel.isOpen()) {
			clientChannel.close();
		}
		
		Message message = new Message(token, MessageType.CLOSE_CONNECTION, null);
		AgentHandler handler = agentHandlerMap.get(token);
		if(handler != null) {
			handler.sendMessage(message);
		}
		
		nettyClientMap.remove(token);
		
		logger.info("Disconnection to " + conn + ".");
		
		workGroup.shutdownGracefully();
	}

	@Override
	public String toString() {
		return conn.toString();
	}
}
