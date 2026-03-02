package com.bugbycode.agent.handler;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.exception.AgentException;
import com.bugbycode.forward.client.StartupRunnable;
import com.bugbycode.mapper.host.HostMapper;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageType;
import com.bugbycode.module.Protocol;
import com.bugbycode.service.testnet.TestnetService;
import com.bugbycode.webapp.pool.WorkTaskPool;
import com.util.RandomUtil;
import com.util.StringUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class AgentHandler extends SimpleChannelInboundHandler<ByteBuf> {
	
	private final Logger logger = LogManager.getLogger(AgentHandler.class);
	
	private Map<String,NettyClient> nettyClientMap;
	
	private int port = 0;
	
	private String host = "";
	
	private boolean firstConnect = false;
	
	private Protocol protocol = Protocol.HTTP;
	
	private StartupRunnable startup;
	
	private final String token;
	
	private HostMapper hostMapper;
	
	private TestnetService testnetService;
	
	private WorkTaskPool workTaskPool;
	
	public AgentHandler(Map<String,NettyClient> nettyClientMap,
			StartupRunnable startup,
			HostMapper hostMapper,TestnetService testnetService,
			WorkTaskPool workTaskPool) {
		this.nettyClientMap = nettyClientMap;
		this.startup = startup;
		this.firstConnect = true;
		this.hostMapper = hostMapper;
		this.token = RandomUtil.GetGuid32();
		this.testnetService = testnetService;
		this.workTaskPool = workTaskPool;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		
		byte[] data = new byte[msg.readableBytes()];
		msg.readBytes(data);
		
		if(this.firstConnect) {

			this.firstConnect = false;
			
			this.protocol = Protocol.resolve(data[0]);
			
			if(this.protocol == Protocol.SOCKET_4) {//socket4
				
				// |VN|CD|DSTPORT|DSTIP|USERID|NULL|
				port = (data[2] << 0x08) &0xFF00 | data[3] & 0xFF;
				
				byte[] ipv4_buf = new byte[0x04];
				
				System.arraycopy(data, 0x04, ipv4_buf, 0, ipv4_buf.length);
				
				host = StringUtil.formatIpv4Address(ipv4_buf).trim();
				
				NettyClient client = new NettyClient(token, nettyClientMap, ctx.channel(), startup, hostMapper, testnetService, workTaskPool, data);
				client.connection(host, port, protocol);
				
				return;
			} else if(this.protocol == Protocol.SOCKET_5) { // socket5 setp1
				firstSendMessageToClient(new Message(token, MessageType.TRANSFER_DATA, new byte[] {0x05,0x00}), ctx);
				return;
			} else {

				this.protocol = Protocol.HTTP;
				
				String connectionStr = new String(data).trim();
				
				String[] connectArr = connectionStr.split("\r\n");
				
				for(String dataStr : connectArr) {
					if(dataStr.startsWith("GET") || dataStr.startsWith("POST")
							 || dataStr.startsWith("HEAD") || dataStr.startsWith("OPTIONS")
							 || dataStr.startsWith("PUT") || dataStr.startsWith("DELETE")
							 || dataStr.startsWith("TRACE")) {
						if(dataStr.contains("ftp:")) {
							this.protocol = Protocol.FTP;
						}
					}else if(dataStr.startsWith("CONNECT")) {
						this.protocol = Protocol.HTTPS;
						String[] serverinfo = dataStr.split(" ");
						if(serverinfo.length > 1) {
							String[] serverArr = serverinfo[1].split(":");
							int len = serverArr.length;
							if(len == 2) {
								port = Integer.valueOf(serverArr[1]);
								host = serverArr[0].trim();
							}
						}
					}else if(dataStr.startsWith("Host:")) {
						String[] serverArr = dataStr.split(":");
						int len = serverArr.length;
						if(len == 2) {
							if(this.protocol == Protocol.HTTPS) {
								port = 443;
							}else if(this.protocol == Protocol.HTTP) {
								port = 80;
							}else if(this.protocol == Protocol.FTP) {
								port = 21;
							}
							host = serverArr[1].trim();
						}else if(len == 3) {
							port = Integer.valueOf(serverArr[2]);
							host = serverArr[1].trim();
						}else {
							throw new AgentException("Host error.");
						}
					}else if(dataStr.startsWith("Proxy-Connection:")) {
						dataStr = dataStr.replace("Proxy-Connection:", "Connection:");
					}
				}
			}
			
			NettyClient client = new NettyClient(token, nettyClientMap, ctx.channel(), startup, hostMapper, testnetService, workTaskPool, data);
			client.connection(host, port, protocol);
			
		} else if(this.protocol == Protocol.SOCKET_5 && (StringUtil.isBlank(host) || port == 0)) { // socket5
			byte ver = data[0];
			byte cmd = data[1];
			//byte rsv = data[2];
			byte atyp = data[3];
			if(!(ver == 0x05 && cmd == 0x01)) {
				ctx.close();
				return;
			}
			
			if(atyp == 0x01) { // ipv4
				
				byte[] ipv4_buf = new byte[0x04];
				
				System.arraycopy(data, 0x04, ipv4_buf, 0, ipv4_buf.length);
				
				host = StringUtil.formatIpv4Address(ipv4_buf);
				
			} else if(atyp == 0x03) { // domain name
				
				byte addr_len = data[0x04];
				byte[] addr_buf = new byte[addr_len];
				
				System.arraycopy(data, 0x05, addr_buf, 0, addr_buf.length);
				host = new String(addr_buf);
				
			} else if(atyp == 0x04) { // proxy ipv6 and remote ipv6
				
				int addr_len = data.length - 0x06;
				byte[] ip_buf = new byte[addr_len];
				
				System.arraycopy(data, 0x04, ip_buf, 0, addr_len);
				
				host = InetAddress.getByAddress(ip_buf).getHostAddress();
				
			} else if(atyp == 0x06) { //proxy ipv4 and remote ipv6
				byte[] ipv6_buf = new byte[0x10];
				System.arraycopy(data, 0x04, ipv6_buf, 0, ipv6_buf.length);
				host = InetAddress.getByAddress(ipv6_buf).getHostAddress();
			}
			
			port = (data[data.length - 2] << 0x08) & 0xFF00 | data[data.length - 1] & 0xFF;
			
			NettyClient client = new NettyClient(token, nettyClientMap, ctx.channel(), startup, hostMapper, testnetService, workTaskPool, data);
			client.connection(host, port, protocol);
			
		} else {
			NettyClient client = nettyClientMap.get(token);
			if(client == null) {
				throw new AgentException("token error.");
			}
			client.writeAndFlush(data);
		}
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.close();
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if(cause instanceof IOException || cause instanceof AgentException) {
			logger.error(cause.getMessage());
		} else {
			logger.error(cause.getMessage(), cause);
		}
		if(ctx != null) {
			ctx.close();
		}
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if(event.state() == IdleState.ALL_IDLE) {//通信超时
				ctx.close();
				logger.debug("Channel timeout.");
			}
		}
	}
	
	private void firstSendMessageToClient(Message msg, ChannelHandlerContext ctx) {
		
		if(msg.getType() == MessageType.TRANSFER_DATA) {
			
			Channel channel = ctx.channel();
			byte[] data = (byte[]) msg.getData();
			ByteBuf buff = channel.alloc().buffer(data.length);
			buff.writeBytes(data);
			channel.writeAndFlush(buff);
			
		}
	}
	
}
