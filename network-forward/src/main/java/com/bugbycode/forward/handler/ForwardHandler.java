package com.bugbycode.forward.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.client.startup.NettyClient;
import com.util.StringUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ForwardHandler extends SimpleChannelInboundHandler<ByteBuf> {

	private final Logger logger = LogManager.getLogger(ForwardHandler.class);
	
	private Channel forwardChannel;
	
	private NettyClient client;
	
	private String host;
	
	private int port;
	
	public ForwardHandler(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("Client connected.");
		this.forwardChannel = ctx.channel();
		this.client = new NettyClient(host, port, forwardChannel);
		this.client.connection();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.info("Client disconnected.");
		if(this.client != null) {
			this.client.close();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(cause.getMessage(), cause);
		ctx.close();
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if(event.state() == IdleState.ALL_IDLE) {//通信超时
				ctx.close();
				logger.info("Channel timeout.");
			}
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		byte[] data = new byte[msg.readableBytes()];
		msg.readBytes(data);

		logger.debug(StringUtil.byteToHexString(data, data.length));
		
		this.client.writeAndFlush(data);
	}
	
	public void close() {
		if(this.forwardChannel != null && this.forwardChannel.isOpen()) {
			this.forwardChannel.close();
		}
	}
	
}
