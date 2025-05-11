package com.bugbycode.client.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.forward.handler.ForwardHandler;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageType;
import com.util.StringUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

	private final Logger logger = LogManager.getLogger(ClientHandler.class);
	
	private ForwardHandler forwardHandler;
	
	private NettyClient client;
	
	public ClientHandler(ForwardHandler forwardHandler, NettyClient client) {
		this.forwardHandler = forwardHandler;
		this.client = client;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		byte[] data = new byte[msg.readableBytes()];
		msg.readBytes(data);
		logger.debug(StringUtil.byteToHexString(data, data.length));
		Message message = new Message(null, MessageType.TRANSFER_DATA, data);
		this.forwardHandler.sendMessage(message);
	}

	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception{
		ctx.close();
		this.client.close();
	}
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception{
		ctx.close();
		if(this.forwardHandler != null) {
			this.forwardHandler.close();
		}
		logger.error(cause.getMessage());
    }
}
