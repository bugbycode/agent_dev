package com.bugbycode.proxy.handler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.module.Authentication;
import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;
import com.util.StringUtil;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ServerHandler extends ChannelInboundHandlerAdapter {
	
	private final Logger logger = LogManager.getLogger(ServerHandler.class);
	
	private int loss_connect_time = 0;
	
	private String username;
	
	private String password;
	
	private ChannelGroup channelGroup;
	
	private Map<String,NettyClient> nettyClientMap;
	
	private Map<String, Channel> onlineAgentMap;
	
	private NioEventLoopGroup remoteGroup;
	
	public ServerHandler(ChannelGroup channelGroup,
			NioEventLoopGroup remoteGroup,
			Map<String, Channel> onlineAgentMap) {
		this.channelGroup = channelGroup;
		this.nettyClientMap = Collections.synchronizedMap(new HashMap<String,NettyClient>());
		this.onlineAgentMap = onlineAgentMap;
		this.remoteGroup = remoteGroup;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		logger.info("Agent connection...");
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		ctx.close();
		ctx.channel().close();
		logger.info("Agent connection closed... " + username);
		onlineAgentMap.remove(username);
		channelGroup.remove(ctx.channel());
		Set<String> keySet = nettyClientMap.keySet();
		synchronized(nettyClientMap) {
			Iterator<String> it = keySet.iterator();
			while(it.hasNext()) {
				String key = it.next();
				nettyClientMap.get(key).close(false);
			}
			nettyClientMap.clear();
		}
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		loss_connect_time = 0;
		Channel channel = ctx.channel();
		Message message = (Message)msg;
		int type = message.getType();
		Object data = message.getData();
		String token = message.getToken();
		if(type == MessageCode.AUTH) {
			if(data == null || !(data instanceof Authentication)) {
				ctx.close();
				return;
			}
			Authentication authInfo = (Authentication)data;
			
			String username = authInfo.getUsername();
			String password = authInfo.getPassword();
			
			Channel clientAgent = onlineAgentMap.get(username);
			if(!(clientAgent == null)) {
				message.setType(MessageCode.AUTH_ERROR);
				message.setData(null);
				channel.writeAndFlush(message);
				ctx.close();
				return;
			}
			
			this.username = username;
			this.password = password;
			
			if(StringUtil.isBlank(this.password)) {
				message.setType(MessageCode.AUTH_ERROR);
				message.setData(null);
				channel.writeAndFlush(message);
				ctx.close();
				return;
			}
			
			message.setType(MessageCode.AUTH_SUCCESS);
			message.setData(null);
			channel.writeAndFlush(message);
			
			onlineAgentMap.put(username, channel);
			channelGroup.add(channel);
			return;
		}
		
		channel = channelGroup.find(channel.id());
		if(channel == null) {
			ctx.close();
			return;
		}
		
		if(type == MessageCode.HEARTBEAT) {
			//
			//System.out.println(message);
			return;
		}
		
		// Connection
		if(type == MessageCode.CONNECTION) {
			if(!(data instanceof ConnectionInfo)) {
				ctx.close();
				return;
			}
			
			NettyClient client = new NettyClient(message, channel,this.remoteGroup,
					nettyClientMap);
			client.connection();
			return;
		}
		
		if(type == MessageCode.CLOSE_CONNECTION) {
			synchronized (nettyClientMap) {
				NettyClient client = nettyClientMap.get(token);
				if(client != null) {
					client.close(false);
				}
			}
			return;
		}
		
		if(type == MessageCode.TRANSFER_DATA) {
			synchronized (nettyClientMap) {
				NettyClient client = nettyClientMap.get(token);
				if(client == null) {
					message.setToken(token);
					message.setType(MessageCode.CLOSE_CONNECTION);
					message.setData(null);
					channel.writeAndFlush(message);
					return;
				}
				byte[] buffer = (byte[]) data;
				client.writeAndFlush(buffer);
			}
		}
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		super.channelReadComplete(ctx);
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				loss_connect_time++;
				//logger.info("Read heartbeat timeout.");
				if (loss_connect_time > 2) {
					logger.info("Channel timeout.");
					ctx.channel().close();
				}
			}
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		super.exceptionCaught(ctx, cause);
		ctx.close();
		String error = cause.getMessage();
		if(StringUtil.isBlank(error)) {
			cause.printStackTrace();
		}
		logger.error(error);
	}
}
