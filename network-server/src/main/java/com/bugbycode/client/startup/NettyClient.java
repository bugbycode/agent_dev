package com.bugbycode.client.startup;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	
	private Channel clientChannel;
	
	private Bootstrap bs;
	
	private String token;
	
	private Channel serverChannel;
	
	private EventLoopGroup workGroup;
	
	private Map<String,NettyClient> nettyClientMap;
	
	private ConnectionInfo conn;

	private ThreadLocal<Integer> loss_connect_time = ThreadLocal.withInitial(() -> 0);
	
	public NettyClient(Message msg, Channel serverChannel,
			Map<String,NettyClient> nettyClientMap) {
		this.bs = new Bootstrap();
		this.token = msg.getToken();
		this.serverChannel = serverChannel;
		this.conn = (ConnectionInfo) msg.getData();
		this.workGroup = new NioEventLoopGroup();
		this.nettyClientMap = nettyClientMap;
		synchronized (this.nettyClientMap) {
			this.nettyClientMap.put(token, this);
		}
	}
	
	public void connection() {
		
		this.bs.group(workGroup).channel(NioSocketChannel.class);
		this.bs.option(ChannelOption.TCP_NODELAY, true);
		this.bs.option(ChannelOption.SO_KEEPALIVE, true);
		this.bs.option(ChannelOption.SO_REUSEADDR, true);
		this.bs.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new IdleStateHandler(IdleConfig.READ_IDEL_TIME_OUT, IdleConfig.WRITE_IDEL_TIME_OUT,
						IdleConfig.ALL_IDEL_TIME_OUT, TimeUnit.SECONDS));
				ch.pipeline().addLast(new ClientHandler(nettyClientMap,serverChannel,token,NettyClient.this));
			}
		});
		
		this.bs.connect(conn.getHost(), conn.getPort()).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				Message message = new Message(token, MessageType.CONNECTION_SUCCESS, null);
				if(future.isSuccess()) {
					logger.info("Connection to " + conn + " successfully.");
					message.setType(MessageType.CONNECTION_SUCCESS);
					serverChannel.writeAndFlush(message);
					clientChannel = future.channel();
				}else {
					logger.info("Connection to " + conn + " failed.");
					message.setType(MessageType.CONNECTION_ERROR);
					serverChannel.writeAndFlush(message);
					nettyClientMap.remove(token);
					close(true);
				}
			}
		});
	}
	
	public void writeAndFlush(byte[] data) {
		this.resetLossConnectTime();
		ByteBuf buff = clientChannel.alloc().buffer(data.length);
		buff.writeBytes(data);
		clientChannel.writeAndFlush(buff);
	}
	
	public void close(boolean sendClose) {
		
		this.nettyClientMap.remove(token);
		
		if(sendClose) {
			Message message = new Message(token, MessageType.CLOSE_CONNECTION, null);
			serverChannel.writeAndFlush(message);
		}
		
		if(clientChannel != null && clientChannel.isOpen()) {
			clientChannel.close();
		}
		
		logger.info("Disconnection to " + conn + ".");
		
		workGroup.shutdownGracefully();
	}
	
	public void resetLossConnectTime() {
		this.loss_connect_time.set(0);
	}
	
	public int getLossConnectTime() {
		return this.loss_connect_time.get();
	}
	
	public void addLossConnectTime() {
		this.loss_connect_time.set(this.loss_connect_time.get() + 1);
	}

	@Override
	public String toString() {
		return conn.toString();
	}
}
