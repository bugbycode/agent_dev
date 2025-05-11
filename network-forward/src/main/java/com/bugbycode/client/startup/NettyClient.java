package com.bugbycode.client.startup;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.client.handler.ClientHandler;
import com.bugbycode.config.IdleConfig;
import com.bugbycode.forward.handler.ForwardHandler;
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
	
	private String host;
	
	private int port;
	
	private Channel clientChannel;
	
	private ForwardHandler forwardHandler;
	
	public NettyClient(String host, int port, ForwardHandler forwardHandler) {
		this.host = host;
		this.port = port;
		this.forwardHandler = forwardHandler;
	}
	
	public void connection() {
		this.bs = new Bootstrap();
		this.workGroup = new NioEventLoopGroup();
		this.bs.group(workGroup).channel(NioSocketChannel.class);
		this.bs.option(ChannelOption.SO_REUSEADDR, true);
		this.bs.option(ChannelOption.TCP_NODELAY, true);
		this.bs.option(ChannelOption.SO_KEEPALIVE, true);
		this.bs.handler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new IdleStateHandler(IdleConfig.READ_IDEL_TIME_OUT, IdleConfig.WRITE_IDEL_TIME_OUT,
						IdleConfig.ALL_IDEL_TIME_OUT, TimeUnit.SECONDS));
				ch.pipeline().addLast(new ClientHandler(forwardHandler, NettyClient.this));
			}
			
		});
		this.bs.connect(this.host, this.port).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				Message message = new Message(null, MessageType.CONNECTION_ERROR, null);
				if(future.isSuccess()) {
					message.setType(MessageType.CONNECTION_SUCCESS);
					clientChannel = future.channel();
					logger.info("Connection {}:{} success.", host, port);
				} else {
					logger.info("Connection {}:{} failed.", host, port);
				}
				forwardHandler.sendMessage(message);
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
		
		Message message = new Message(null, MessageType.CLOSE_CONNECTION, null);
		
		this.forwardHandler.sendMessage(message);
		
		logger.info("Disconnection {}:{}.", host, port);
		
		workGroup.shutdownGracefully();
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}
	
}
