package com.bugbycode.forward.handler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.forward.client.StartupRunnable;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageType;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ClientHandler extends ChannelInboundHandlerAdapter {
	
	private final Logger logger = LogManager.getLogger(ClientHandler.class);
	
	private Map<String,NettyClient> nettyClientMap;
	
	public ClientHandler(StartupRunnable startup,Map<String,NettyClient> nettyClientMap) {
		this.nettyClientMap = nettyClientMap;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("Connection server success.");
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		if(ctx != null) {
			ctx.close();
		}
		logger.info("Connection closed.");
		
		Set<String> keySet = nettyClientMap.keySet();
		Set<String> tokens = new HashSet<String>();
		
		for(String token : keySet) {
			tokens.add(token);
		}
		
		for(String token : tokens) {
			NettyClient client = nettyClientMap.get(token);
			Message message = new Message(token, MessageType.CLOSE_CONNECTION, null);
			if(client.isForward()) {
				client.recv(message);
			}
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		
		Message message = (Message) msg;
		String token = message.getToken();
		MessageType type = message.getType();
		
		if(type == MessageType.HEARTBEAT) {
			//
		}else {
			NettyClient client = nettyClientMap.get(token);
			if(client == null) {
				if(type != MessageType.CLOSE_CONNECTION) {
					message.setData(null);
					message.setType(MessageType.CLOSE_CONNECTION);
					message.setToken(token);
					ctx.writeAndFlush(message);
				}
				return;
			}
			client.recv(message);
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.WRITER_IDLE) {//超过一定时间未发送数据时 自动发送心跳通信数据
				Message msg = new Message();
				msg.setType(MessageType.HEARTBEAT);
				ctx.channel().writeAndFlush(msg);
			} else if(event.state() == IdleState.READER_IDLE) {//读超时 未收到任何数据传输时 自动关闭连接
				ctx.close();
				logger.error("Channel timeout.");
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
		logger.error(cause.getMessage());
	}
	
}
