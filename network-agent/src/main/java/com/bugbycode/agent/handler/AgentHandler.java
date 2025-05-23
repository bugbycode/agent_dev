package com.bugbycode.agent.handler;

import java.net.InetAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.exception.AgentException;
import com.bugbycode.forward.client.StartupRunnable;
import com.bugbycode.mapper.host.HostMapper;
import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageType;
import com.bugbycode.module.Protocol;
import com.bugbycode.module.host.HostModule;
import com.bugbycode.service.testnet.TestnetService;
import com.bugbycode.webapp.pool.WorkTaskPool;
import com.bugbycode.webapp.pool.task.host.InsertHostTask;
import com.bugbycode.webapp.pool.task.host.UpdateForwardTask;
import com.bugbycode.webapp.pool.task.host.UpdateResultTask;
import com.util.RandomUtil;
import com.util.StringUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class AgentHandler extends SimpleChannelInboundHandler<ByteBuf> {
	
	private final byte[] HTTP_PROXY_RESPONSE = "HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(); 

	private final Logger logger = LogManager.getLogger(AgentHandler.class);
	
	private Map<String,AgentHandler> agentHandlerMap;
	
	private Map<String,AgentHandler> forwardHandlerMap;
	
	private Map<String,NettyClient> nettyClientMap;
	
	private int loss_connect_time = 0;
	
	private int port = 0;
	
	private String host = "";
	
	private boolean firstConnect = false;
	
	private boolean isForward = false;
	
	private Protocol protocol = Protocol.HTTP;
	
	private StartupRunnable startup;
	
	private final String token;
	
	private LinkedList<Message> queue;
	
	private boolean isClosed;
	
	private HostMapper hostMapper;
	
	private TestnetService testnetService;
	
	private WorkTaskPool workTaskPool;
	
	public AgentHandler(Map<String, AgentHandler> agentHandlerMap, 
			Map<String,AgentHandler> forwardHandlerMap,
			Map<String,NettyClient> nettyClientMap,
			StartupRunnable startup,
			HostMapper hostMapper,TestnetService testnetService,
			WorkTaskPool workTaskPool) {
		this.agentHandlerMap = agentHandlerMap;
		this.forwardHandlerMap = forwardHandlerMap;
		this.nettyClientMap = nettyClientMap;
		this.startup = startup;
		this.firstConnect = true;
		this.hostMapper = hostMapper;
		this.queue = new LinkedList<Message>();
		this.token = RandomUtil.GetGuid32();
		this.testnetService = testnetService;
		this.workTaskPool = workTaskPool;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		
		loss_connect_time = 0;
		
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
				
				Message message = connection(host, port, ctx);
				
				byte[] res_buf = new byte[0x08];
				System.arraycopy(data, 0, res_buf, 0, res_buf.length);
				res_buf[0x01] = 0x5A;
				
				message.setType(MessageType.TRANSFER_DATA);
				message.setData(res_buf);
				
				sendMessage(message);
				
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
			
			Message message = connection(host, port, ctx);
			
			message.setType(MessageType.TRANSFER_DATA);
			message.setToken(token);
			
			if(this.protocol == Protocol.HTTPS) {
				message.setData(HTTP_PROXY_RESPONSE);
				sendMessage(message);
			}else if(isForward) {
				message.setType(MessageType.TRANSFER_DATA);
				message.setData(data);
				message.setToken(token);
				startup.writeAndFlush(message);
			}else {
				NettyClient client = nettyClientMap.get(token);
				if(client == null) {
					throw new AgentException("token error.");
				}
				client.writeAndFlush(data);
			}
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
			
			Message message = connection(host, port, ctx);
			
			data[1] = 0;
			data[2] = 0;
			message.setData(data);
			message.setType(MessageType.TRANSFER_DATA);
			
			sendMessage(message);
		} else {
			if(isForward) {
				Message message = new Message();
				message.setType(MessageType.TRANSFER_DATA);
				message.setData(data);
				message.setToken(token);
				startup.writeAndFlush(message);
			}else {
				NettyClient client = nettyClientMap.get(token);
				if(client == null) {
					throw new AgentException("token error.");
				}
				client.writeAndFlush(data);
			}
		}
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.close();
		//关闭连接
		NettyClient client = nettyClientMap.get(token);
		if(client != null) {
			client.close();
		}
		agentHandlerMap.remove(token);
		if(isForward) {
			Message message = new Message(token, MessageType.CLOSE_CONNECTION, null);
			startup.writeAndFlush(message);
			logger.info("Disconnection to " + host + ":" + port + ".");
		}
		forwardHandlerMap.remove(token);
		this.isClosed = true;
		notifyTask();
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.isClosed = false;
		agentHandlerMap.put(token, this);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if(cause instanceof AgentException) {
			logger.info(cause.getMessage());
		} else {
			logger.error(cause.getMessage(), cause);
		}
		notifyTask();
		if(ctx != null) {
			ctx.channel().close();
			ctx.close();
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
				msg.setType(MessageType.HEARTBEAT);
				ctx.channel().writeAndFlush(msg);
			}
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
					
				} catch (InterruptedException e) {
					logger.debug(e.getMessage());
				}
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
	
	private Message connection(String host,int port,ChannelHandlerContext ctx) throws Exception {
		Message message = null;
		host = host.trim();
		
		if(StringUtil.isBlank(host) || port == 0) {
			throw new AgentException("Protocol error.");
		}
		
		ConnectionInfo con = new ConnectionInfo(host, port);
		Message conMsg = new Message(token, MessageType.CONNECTION, con);
		
		HostModule hostModule = hostMapper.queryByHost(host);
		
		boolean isNewHost = false;
		
		Date now = new Date();
		
		if(hostModule == null) {
			isNewHost = true;
			hostModule = new HostModule();

			hostModule.setHost(host);
			hostModule.setForward(0);
			hostModule.setConnTime(now);
			
			if(protocol == Protocol.HTTP || protocol == Protocol.HTTPS) {
				
				String url = (protocol == Protocol.HTTP ? "http://" : "https://") + host + ":" + port;
				
				if(!testnetService.checkHttpConnect(url)) {
					hostModule.setForward(1);
				}
				
			} else if(protocol == Protocol.SOCKET_4 || protocol == Protocol.SOCKET_5) {
				//socket5 or socket5 default forward
				hostModule.setForward(1);
			}
			
			workTaskPool.add(new InsertHostTask(hostMapper, hostModule));
			
		}
		
		if(!(hostModule == null || hostModule.getForward() == 0)) {
			
			forwardHandlerMap.put(token, this);
			
			startup.writeAndFlush(conMsg);
			
			message = read();
			
			if(message.getType() == MessageType.CONNECTION_ERROR) {
				
				workTaskPool.add(new UpdateResultTask(hostMapper, host, 0, now));
				
				throw new AgentException("Connection to " + host + ":" + port + " failed.");
				
			} else if(message.getType() == MessageType.CONNECTION_SUCCESS) {
				
				logger.info("Connection to " + host + ":" + port + " success.");
				
			}
			
			isForward = true;
		} else {
			new NettyClient(conMsg, nettyClientMap, agentHandlerMap)
			.connection();
			
			message = read();
			
			if(message.getType() == MessageType.CONNECTION_ERROR) {
				
				//不是新访问的站点则默认不转发
				if(!isNewHost) {
					
					workTaskPool.add(new UpdateResultTask(hostMapper, host, 0, now));
					
					throw new AgentException("Connection to " + host + ":" + port + " failed.");
				}
				
				forwardHandlerMap.put(token, this);
				
				startup.writeAndFlush(conMsg);
				
				message = read();
				
				if(message.getType() == MessageType.CONNECTION_ERROR) {
					
					workTaskPool.add(new UpdateResultTask(hostMapper, host, 0, now));
					
					throw new AgentException("Connection to " + host + ":" + port + " failed.");
					
				} else if(message.getType() == MessageType.CONNECTION_SUCCESS) {
					
					logger.info("Connection to " + host + ":" + port + " success.");
					
				}
				
				workTaskPool.add(new UpdateForwardTask(host, 1, hostMapper));
				
				isForward = true;
				
			} /*else {

				if(protocol == Protocol.HTTP || protocol == Protocol.HTTPS) {
					
					hostModule = hostMapper.queryByHost(host);
					
					if(hostModule != null && hostModule.getForward() == 0) {
						
						String url = (protocol == Protocol.HTTP ? "http://" : "https://") + host + ":" + port;
						
						workTaskPool.add(new TestNetConnectTask(testnetService, url, 
								new UpdateForwardTask(host, 1, hostMapper)));
						
					}
				}
			}*/
		}
		
		workTaskPool.add(new UpdateResultTask(hostMapper, host, 1, now));
		
		new WorkThread(ctx).start();
		
		return message;
	}
}
