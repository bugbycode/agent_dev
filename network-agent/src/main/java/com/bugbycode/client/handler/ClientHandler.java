package com.bugbycode.client.handler;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.agent.handler.AgentHandler;
import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageType;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

	private final Logger logger = LogManager.getLogger(ClientHandler.class);
	
	private Map<String,AgentHandler> agentHandlerMap;
	
	private String token;
	
	private NettyClient client;
	
	public ClientHandler(Map<String,AgentHandler> agentHandlerMap,String token,NettyClient client) {
		this.agentHandlerMap = agentHandlerMap;
		this.token = token;
		this.client = client;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		byte[] data = new byte[msg.readableBytes()];
		msg.readBytes(data);
		Message message = new Message(token, MessageType.TRANSFER_DATA, data);
		AgentHandler handler = agentHandlerMap.get(token);
		if(handler == null) {
			throw new RuntimeException("Connetion closed.");
		}
		handler.sendMessage(message);
	}

	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception{
		ctx.close();
		client.close();
	}
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception{
		logger.error(cause.getMessage());
		ctx.close();
    }
}
