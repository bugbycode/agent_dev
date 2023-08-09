package com.bugbycode.agent.handler;

import java.net.InetAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.forward.client.StartupRunnable;
import com.bugbycode.mapper.host.HostMapper;
import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;
import com.bugbycode.module.Protocol;
import com.bugbycode.module.host.HostModule;
import com.util.RandomUtil;
import com.util.StringUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;

public class AgentHandler extends SimpleChannelInboundHandler<ByteBuf> {
	
	private final byte[] HTTP_PROXY_RESPONSE = "HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(); 

	private final Logger logger = LogManager.getLogger(AgentHandler.class);
	
	private Map<String,AgentHandler> agentHandlerMap;
	
	private Map<String,AgentHandler> forwardHandlerMap;
	
	private Map<String,NettyClient> nettyClientMap;
	
	private boolean firstConnect = false;
	
	private boolean isForward = false;
	
	private byte protocol = Protocol.HTTP;
	
	private EventLoopGroup remoteGroup;
	
	private StartupRunnable startup;
	
	private final String token;
	
	private LinkedList<Message> queue;
	
	private boolean isClosed;
	
	private HostMapper hostMapper;
	
	public AgentHandler(Map<String, AgentHandler> agentHandlerMap, 
			Map<String,AgentHandler> forwardHandlerMap,
			Map<String,NettyClient> nettyClientMap,
			EventLoopGroup remoteGroup,
			StartupRunnable startup,
			HostMapper hostMapper) {
		this.agentHandlerMap = agentHandlerMap;
		this.forwardHandlerMap = forwardHandlerMap;
		this.nettyClientMap = nettyClientMap;
		this.remoteGroup = remoteGroup;
		this.startup = startup;
		this.firstConnect = true;
		this.hostMapper = hostMapper;
		this.queue = new LinkedList<Message>();
		this.token = RandomUtil.GetGuid32();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		byte[] data = new byte[msg.readableBytes()];
		msg.readBytes(data);
		
		if(this.firstConnect) {

			this.firstConnect = false;
			
			new WorkThread(ctx).start();
			
			int port = 0;
			String host = "";

			this.protocol = data[0];
			if(this.protocol == Protocol.SOCKET_4) {//socket4

				this.protocol = Protocol.HTTPS;
				
				// |VN|CD|DSTPORT|DSTIP|USERID|NULL|
				port = (data[2] << 0x08) &0xFF00 | data[3] & 0xFF;
				
				byte[] ipv4_buf = new byte[0x04];
				
				System.arraycopy(data, 0x04, ipv4_buf, 0, ipv4_buf.length);
				
				//host = InetAddress.getByAddress(ipv4_buf).getHostAddress();
				
				host = StringUtil.formatIpv4Address(ipv4_buf);
				
				Message message = connection(host, port);
				
				message.setType(MessageCode.TRANSFER_DATA);
				message.setData(new byte[] {0x00,0x5A,0x00,0x00,0x00,0x00,0x00,0x00});
				
				sendMessage(message);
				
				return;
			} else if(this.protocol == Protocol.SOCKET_5) { // socket5 setp1
				sendMessage(new Message(token, MessageCode.TRANSFER_DATA, new byte[] {0x05,0x00}));
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
								host = serverArr[0];
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
							host = serverArr[1];
						}else if(len == 3) {
							port = Integer.valueOf(serverArr[2]);
							host = serverArr[1];
						}else {
							throw new RuntimeException("Host error.");
						}
					}else if(dataStr.startsWith("Proxy-Connection:")) {
						dataStr = dataStr.replace("Proxy-Connection:", "Connection:");
					}
				}
			}

			Message message = connection(host, port);
			
			message.setType(MessageCode.TRANSFER_DATA);
			message.setToken(token);
			
			if(this.protocol == Protocol.HTTPS || this.protocol == Protocol.HTTP || 
					this.protocol == Protocol.FTP) {
				message.setData(HTTP_PROXY_RESPONSE);
				sendMessage(message);
			}else{
				if(isForward) {
					message.setType(MessageCode.TRANSFER_DATA);
					message.setData(data);
					message.setToken(token);
					startup.writeAndFlush(message);
				}else {
					NettyClient client = nettyClientMap.get(token);
					if(client == null) {
						throw new RuntimeException("token error.");
					}
					client.writeAndFlush(data);
				}
			}
		} else if(this.protocol == Protocol.SOCKET_5) { // socket5
			byte ver = data[0];
			byte cmd = data[1];
			//byte rsv = data[2];
			byte atyp = data[3];
			if(!(ver == 0x05 && cmd == 0x01)) {
				ctx.close();
				return;
			}
			String host = "";
			int port = 0;
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
			
			this.protocol = Protocol.HTTPS;
			
			Message message = connection(host, port);
			data[1] = 0;
			data[2] = 0;
			message.setData(data);
			message.setType(MessageCode.TRANSFER_DATA);
			
			sendMessage(message);
		} else {
			if(isForward) {
				Message message = new Message();
				message.setType(MessageCode.TRANSFER_DATA);
				message.setData(data);
				message.setToken(token);
				startup.writeAndFlush(message);
			}else {
				NettyClient client = nettyClientMap.get(token);
				if(client == null) {
					throw new RuntimeException("token error.");
				}
				client.writeAndFlush(data);
			}
		}
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.close();
		//关闭连接
		logger.info("Browser Closed Connection.");
		NettyClient client = nettyClientMap.get(token);
		if(client != null) {
			client.close();
		}
		agentHandlerMap.remove(token);
		if(isForward) {
			Message message = new Message(token, MessageCode.CLOSE_CONNECTION, null);
			startup.writeAndFlush(message);
		}
		forwardHandlerMap.remove(token);
		this.isClosed = true;
		notifyTask();
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		this.isClosed = false;
		agentHandlerMap.put(token, this);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		String error = cause.getMessage();
		if(StringUtil.isBlank(error)) {
			cause.printStackTrace();
		}
		logger.error(error);
		if(ctx != null) {
			ctx.channel().close();
			ctx.close();
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
			while(!isClosed) {
				try {
					Message msg = read();
					if(msg.getType() == MessageCode.CLOSE_CONNECTION) {
						ctx.close();
						continue;
					}
					
					if(msg.getType() != MessageCode.TRANSFER_DATA) {
						continue;
					}
					
					byte[] data = (byte[]) msg.getData();
					ByteBuf buff = channel.alloc().buffer(data.length);
					buff.writeBytes(data);
					channel.writeAndFlush(buff);
				} catch (InterruptedException e) {
					logger.info(e.getMessage());
				}
			}
		}
		
	}
	
	private Message connection(String host,int port) throws Exception {
		Message message = null;
		host = host.trim();
		
		if(StringUtil.isBlank(host) || port == 0) {
			throw new RuntimeException("Protocol error.");
		}
		
		ConnectionInfo con = new ConnectionInfo(host, port);
		Message conMsg = new Message(token, MessageCode.CONNECTION, con);
		
		HostModule hostModule = hostMapper.queryByHost(host);
		
		boolean isNewHost = false;
		
		Date now = new Date();
		
		if(hostModule == null) {
			isNewHost = true;
			hostModule = new HostModule();
			hostModule.setHost(host);
			hostModule.setForward(0);
			hostModule.setConnTime(now);
			try {
				hostMapper.insert(hostModule);
			}catch (Exception e) {
				logger.info(e.getLocalizedMessage());
			}
		}
		
		if(!(hostModule == null || hostModule.getForward() == 0)) {
			
			forwardHandlerMap.put(token, this);
			
			startup.writeAndFlush(conMsg);
			
			message = read();
			
			if(message.getType() == MessageCode.CONNECTION_ERROR) {
				try {
					hostMapper.updateResultDatetimeByHost(host, 0, now);
				}catch (Exception e) {
					logger.info(e.getLocalizedMessage());
				}
				
				throw new RuntimeException("Connection to " + host + ":" + port + " failed.");
			}
			
			isForward = true;
		} else {
			new NettyClient(conMsg, nettyClientMap, agentHandlerMap,remoteGroup)
			.connection();
			
			message = read();
			
			if(message.getType() == MessageCode.CONNECTION_ERROR) {
				
				//不是新访问的站点则默认不转发
				if(!isNewHost) {
					try {
						hostMapper.updateResultDatetimeByHost(host, 0, now);
					}catch (Exception e) {
						logger.info(e.getLocalizedMessage());
					}
					
					throw new RuntimeException("Connection to " + host + ":" + port + " failed.");
				}
				
				forwardHandlerMap.put(token, this);
				
				startup.writeAndFlush(conMsg);
				
				message = read();
				
				if(message.getType() == MessageCode.CONNECTION_ERROR) {
					try {
						hostMapper.updateResultDatetimeByHost(host, 0, now);
					}catch (Exception e) {
						logger.info(e.getLocalizedMessage());
					}
					
					throw new RuntimeException("Connection to " + host + ":" + port + " failed.");
				}
				
				hostModule = hostMapper.queryByHost(host);
				try {
					hostMapper.updateForwardById(hostModule.getId(), 1);
				}catch (Exception e) {
					logger.info(e.getLocalizedMessage());
				}
				
				isForward = true;
			}
		}
		
		try {
			hostMapper.updateResultDatetimeByHost(host, 1, now);
		}catch (Exception e) {
			logger.info(e.getLocalizedMessage());
		}
		
		return message;
	}
}
