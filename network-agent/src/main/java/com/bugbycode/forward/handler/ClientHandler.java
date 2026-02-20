package com.bugbycode.forward.handler;

import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.agent.handler.AgentHandler;
import com.bugbycode.forward.client.StartupRunnable;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageType;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ClientHandler extends ChannelInboundHandlerAdapter {
	
	private final Logger logger = LogManager.getLogger(ClientHandler.class);
	
	private Map<String,AgentHandler> agentHandlerMap;
	
	public ClientHandler(StartupRunnable startup,Map<String, AgentHandler> agentHandlerMap) {
		this.agentHandlerMap = agentHandlerMap;
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
		Set<String> set = agentHandlerMap.keySet();
		for(String token : set) {
			AgentHandler handler = agentHandlerMap.get(token);
			Message message = new Message(token, MessageType.CLOSE_CONNECTION, null);
			handler.sendMessage(message);
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
			AgentHandler handler = agentHandlerMap.get(token);
			if(handler == null) {
				if(type != MessageType.CLOSE_CONNECTION) {
					message.setData(null);
					message.setType(MessageType.CLOSE_CONNECTION);
					message.setToken(token);
					ctx.writeAndFlush(message);
				}
				return;
			}
			handler.sendMessage(message);
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {//读取数据超时 自动关闭连接
				ctx.close();
				logger.error("Channel timeout.");
			} else if(event.state() == IdleState.ALL_IDLE) {//没有数据传输的时间间隔 自动发送心跳通信
				Message msg = new Message();
				msg.setType(MessageType.HEARTBEAT);
				ctx.channel().writeAndFlush(msg);
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(cause.getLocalizedMessage());
		this.channelInactive(ctx);
	}
	
}
