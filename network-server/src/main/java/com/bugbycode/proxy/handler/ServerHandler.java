package com.bugbycode.proxy.handler;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ServerHandler extends ChannelInboundHandlerAdapter {
	
	private final Logger logger = LogManager.getLogger(ServerHandler.class);
	
	private int loss_connect_time = 0;
	
	private Map<String,NettyClient> nettyClientMap;
	
	private NioEventLoopGroup remoteGroup;
	
	public ServerHandler(NioEventLoopGroup remoteGroup) {
		this.nettyClientMap = new Hashtable<String,NettyClient>();
		this.remoteGroup = remoteGroup;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("Agent connection.");
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.close();
		logger.info("Agent connection closed.");
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
		
		logger.info("Close a total of " + list.size() + " connections.");
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		loss_connect_time = 0;
		Channel channel = ctx.channel();
		Message message = (Message)msg;
		int type = message.getType();
		Object data = message.getData();
		String token = message.getToken();
		
		if(type == MessageCode.HEARTBEAT) {
			//
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
			NettyClient client = nettyClientMap.get(token);
			if(client != null) {
				client.close(false);
			}
			return;
		}
		
		if(type == MessageCode.TRANSFER_DATA) {
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
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				loss_connect_time++;
				if (loss_connect_time > 3) {
					logger.info("Channel timeout.");
					ctx.channel().close();
				}
			} else if (event.state() == IdleState.WRITER_IDLE) {
				Message msg = new Message();
				msg.setType(MessageCode.HEARTBEAT);
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
