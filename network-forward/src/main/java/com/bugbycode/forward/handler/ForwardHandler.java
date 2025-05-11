package com.bugbycode.forward.handler;

import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageType;
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
	
	private LinkedList<Message> queue;
	
	private boolean isClosed;
	
	private boolean firstConnect = false;
	
	private int loss_connect_time = 0;
	
	public ForwardHandler(String host, int port) {
		this.queue = new LinkedList<Message>();
		this.firstConnect = true;
		this.client = new NettyClient(host, port, this);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("Client connected.");
		this.isClosed = false;
		this.forwardChannel = ctx.channel();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.info("Client disconnected.");
		this.isClosed = true;
		this.firstConnect = true;
		ctx.close();
		notifyTask();
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
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		loss_connect_time = 0;
		byte[] data = new byte[msg.readableBytes()];
		msg.readBytes(data);

		logger.debug(StringUtil.byteToHexString(data, data.length));
		
		if(this.firstConnect) {
			this.firstConnect = false;
			this.client.connection();
			Message message = read();
			logger.debug(message);
			if(message.getType() == MessageType.CONNECTION_ERROR) {
				throw new RuntimeException("Connection " + this.client.getHost() + ":" + this.client.getPort() + " failed.");
			}
			new WorkThread(ctx).start();
		}
		
		this.client.writeAndFlush(data);
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
				msg.setType(MessageType.HEARTBEAT);
				ctx.channel().writeAndFlush(msg);
			}
		}
	}
	
	public void close() {
		if(this.forwardChannel != null && this.forwardChannel.isOpen()) {
			this.forwardChannel.close();
		}
	}
	
	public synchronized void sendMessage(Message msg) {
		queue.addLast(msg);
		notifyTask();
	}
	
	private synchronized void notifyTask() {
		this.notifyAll();
	}
	
	private synchronized Message read() throws InterruptedException {
		while(queue.isEmpty()) {
			wait();
			if(isClosed) {
				throw new InterruptedException("Connetion closed.");
			}
		}
		
		return queue.removeFirst();
	}
	
	private class WorkThread extends Thread{

		private ChannelHandlerContext ctx;
		
		public WorkThread(ChannelHandlerContext ctx) {
			this.ctx = ctx;
		}
		
		@Override
		public void run() {
			Channel channel = ctx.channel();
			
			notifyTask();
			
			while(!isClosed) {
				try {
					
					Message msg = read();
					
					if(msg.getType() == MessageType.CLOSE_CONNECTION) {
						ctx.close();
						continue;
					}
					
					if(msg.getType() != MessageType.TRANSFER_DATA) {
						continue;
					}
					
					byte[] data = (byte[]) msg.getData();
					ByteBuf buff = channel.alloc().buffer(data.length);
					buff.writeBytes(data);
					channel.writeAndFlush(buff);
					
					loss_connect_time = 0;
					
					logger.debug(StringUtil.byteToHexString(data, data.length));
					
				} catch (InterruptedException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		
	}
}
