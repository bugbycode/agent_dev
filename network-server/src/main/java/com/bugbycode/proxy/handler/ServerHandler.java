package com.bugbycode.proxy.handler;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.config.IdleConfig;
import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageType;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ServerHandler extends ChannelInboundHandlerAdapter {
	
	private final Logger logger = LogManager.getLogger(ServerHandler.class);
	
	private int loss_connect_time = 0;
	
	private Map<String,NettyClient> nettyClientMap;
	
	public ServerHandler() {
		this.nettyClientMap = new Hashtable<String,NettyClient>();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("Client connected.");
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		loss_connect_time = 0;
		ctx.close();
		logger.info("Client disconnected.");
		List<NettyClient> list = new ArrayList<NettyClient>();
		for(Entry<String, NettyClient> entry : nettyClientMap.entrySet()) {
			NettyClient client = entry.getValue();
			if(client != null) {
				list.add(client);
			}
		};
		for(NettyClient client : list) {
			client.close(false);
		}
		
		nettyClientMap.clear();
		
		logger.debug("Close a total of " + list.size() + " connections.");
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Channel channel = ctx.channel();
		Message message = (Message)msg;
		MessageType type = message.getType();
		Object data = message.getData();
		String token = message.getToken();
		
		if(type == MessageType.HEARTBEAT) {
			//
			return;
		}
		
		// Connection
		if(type == MessageType.CONNECTION) {
			if(!(data instanceof ConnectionInfo)) {
				ctx.close();
				return;
			}
			
			NettyClient client = new NettyClient(message, channel,nettyClientMap);
			client.connection();
			return;
		}
		
		if(type == MessageType.CLOSE_CONNECTION) {
			NettyClient client = nettyClientMap.get(token);
			if(client != null) {
				client.close(false);
			}
			return;
		}
		
		if(type == MessageType.TRANSFER_DATA) {
			NettyClient client = nettyClientMap.get(token);
			if(client == null) {
				message.setToken(token);
				message.setType(MessageType.CLOSE_CONNECTION);
				message.setData(null);
				channel.writeAndFlush(message);
				return;
			}
			byte[] buffer = (byte[]) data;
			client.writeAndFlush(buffer);
		}
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				loss_connect_time++;
				if (loss_connect_time > IdleConfig.LOSS_CONNECT_TIME_COUNT) {
					logger.info("Channel timeout.");
					ctx.channel().close();
				}
			} else if (event.state() == IdleState.WRITER_IDLE) {//写超时事件触发时发送心跳通信
				Message msg = new Message();
				msg.setType(MessageType.HEARTBEAT);
				ctx.channel().writeAndFlush(msg);
			}
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
		logger.error(cause.getLocalizedMessage());
	}
}
