package com.bugbycode.client.startup;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.client.handler.ClientHandler;
import com.bugbycode.config.IdleConfig;
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
import com.util.StringUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

public class NettyClient {
	
	private final Logger logger = LogManager.getLogger(NettyClient.class);
	
	private final byte[] HTTP_PROXY_RESPONSE = "HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(); 
	
	private EventLoopGroup workGroup;
	
	private Map<String,NettyClient> nettyClientMap;
	
	private String token = "";
	
	private Channel clientChannel;
	
	private ConnectionInfo conn;
	
	// new version ======================= start
	
	private StartupRunnable startup; 
	private HostMapper hostMapper; 
	private TestnetService testnetService; 
	private WorkTaskPool workTaskPool;
	
	private Channel agentChannel;
	
	private String host; 
	private int port; 
	private Protocol protocol;
	private boolean isForward = false;
	private boolean isNewHost = false;
	private byte[] data;
	
	// new version ======================= end
	
	public NettyClient(String token,Map<String,NettyClient> nettyClientMap, Channel agentChannel,
			StartupRunnable startup, HostMapper hostMapper, TestnetService testnetService, 
			WorkTaskPool workTaskPool, byte[] data) {
		this.token = token;
		this.nettyClientMap = nettyClientMap;
		this.nettyClientMap.put(this.token, this);
		this.agentChannel = agentChannel;
		this.startup = startup;
		this.hostMapper = hostMapper;
		this.testnetService = testnetService;
		this.workTaskPool = workTaskPool;
		this.data = data;
	}
	
	public void connection(String host, int port, Protocol protocol) {
		
		this.host = host;
		this.port = port;
		this.protocol = protocol;
		
		if(StringUtil.isBlank(host) || port == 0) {
			throw new AgentException("Protocol error.");
		}
		
		HostModule hostModule = hostMapper.queryByHost(host);
		
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
		
		conn = new ConnectionInfo(host, port);
		
		if(!(hostModule == null || hostModule.getForward() == 0)) {
			
			forwardConnection();// forward connection.
			
		} else {
			
			connection();//local connection
			
		}
	}
	
	public void forwardConnection() {
		isForward = true;
		Message conMsg = new Message(token, MessageType.CONNECTION, conn);
		startup.writeAndFlush(conMsg);// forward connection.
	}
	
	public void connection() {
		Bootstrap bs = new Bootstrap();
		this.workGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
		bs.group(workGroup).channel(NioSocketChannel.class);
		bs.option(ChannelOption.TCP_NODELAY, true);
		bs.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new IdleStateHandler(IdleConfig.READ_IDEL_TIME_OUT, IdleConfig.WRITE_IDEL_TIME_OUT,
						IdleConfig.ALL_IDEL_TIME_OUT, TimeUnit.SECONDS));
				ch.pipeline().addLast(new ClientHandler(token, NettyClient.this));
			}
		});
		
		bs.connect(conn.getHost(), conn.getPort()).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				Message message = new Message(token, MessageType.CONNECTION_SUCCESS, null);
				if(future.isSuccess()) {
					message.setType(MessageType.CONNECTION_SUCCESS);
					clientChannel = future.channel();
				}else {
					message.setType(MessageType.CONNECTION_ERROR);
				}
				
				recv(message);
			}
		});
	}
	
	/**
	 * Send data to server
	 * @param data
	 */
	public void writeAndFlush(byte[] data) {
		try {
			if(isForward) {
				Message message = new Message();
				message.setType(MessageType.TRANSFER_DATA);
				message.setData(data);
				message.setToken(token);
				startup.writeAndFlush(message);
			} else {
				if(clientChannel == null || !clientChannel.isOpen()) {
					return;
				}
				ByteBuf buff = clientChannel.alloc().buffer(data.length);
				buff.writeBytes(data);
				clientChannel.writeAndFlush(buff);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			closeClient();
		}
		
	}
	
	public void agentClientWriteAndFlush(Message message) {
		
		if(!(agentChannel != null && agentChannel.isOpen())) {
			return;
		}
		if(message.getType() == MessageType.TRANSFER_DATA) {
			byte[] data = (byte[]) message.getData();
			ByteBuf buff = agentChannel.alloc().buffer(data.length);
			buff.writeBytes(data);
			agentChannel.writeAndFlush(buff);
		}
	}
	
	/**
	 * Send data to client
	 * @param data
	 */
	public void recv(Message message) {
		
		if(message == null || !token.equals(message.getToken())) {
			return;
		}
		
		Date now = new Date();
		
		MessageType type = message.getType();
		
		if(message.getType() == MessageType.HEARTBEAT) { // 心跳通信
			//
			return;
		} else if(message.getType() == MessageType.CLOSE_CONNECTION) {//关闭连接
			if(agentChannel != null && agentChannel.isOpen()) {
				closeClient();
			}
		} else if(message.getType() == MessageType.CONNECTION_ERROR) {
			
			if(isNewHost && !isForward) {//新访问的站点连接失败时尝试转发连接
				forwardConnection();
			} else {
				closeClient();
				logger.info("Connection {}:{} failed.", host, port);
			}
			
			workTaskPool.add(new UpdateResultTask(hostMapper, host, 0, now));
			
			//关闭客户端
			
		} else if(message.getType() == MessageType.CONNECTION_SUCCESS) {
			
			if(isForward) {
				workTaskPool.add(new UpdateForwardTask(host, 1, hostMapper));
			}
			
			workTaskPool.add(new UpdateResultTask(hostMapper, host, 1, now));
			
			logger.info("Connection {}:{} success.", host, port);
			
			//连接成功后处理后续逻辑 比如响应客户端
			if(this.protocol == Protocol.SOCKET_4) {//socket4
				byte[] res_buf = new byte[0x08];
				System.arraycopy(data, 0, res_buf, 0, res_buf.length);
				res_buf[0x01] = 0x5A;
				
				message.setType(MessageType.TRANSFER_DATA);
				message.setData(res_buf);
				
				agentClientWriteAndFlush(message);
			} else if(this.protocol == Protocol.SOCKET_5) {// socket5
				data[1] = 0;
				data[2] = 0;
				message.setData(data);
				message.setType(MessageType.TRANSFER_DATA);
				
				agentClientWriteAndFlush(message);
			} else { // 其他协议 如：http/https
				message.setType(MessageType.TRANSFER_DATA);
				message.setToken(token);
				if(this.protocol == Protocol.HTTPS) {
					message.setData(HTTP_PROXY_RESPONSE);
					agentClientWriteAndFlush(message);
				} else if(isForward) {
					message.setType(MessageType.TRANSFER_DATA);
					message.setData(data);
					message.setToken(token);
					startup.writeAndFlush(message);
				} else {
					writeAndFlush(data);
				}
			}
			
		} else if(type == MessageType.TRANSFER_DATA) {

			if(agentChannel == null || !agentChannel.isOpen()) {
				closeClient();
			} else {
				byte[] data = (byte[]) message.getData();
				ByteBuf buff = agentChannel.alloc().buffer(data.length);
				buff.writeBytes(data);
				agentChannel.writeAndFlush(buff);
			}
		}
	}
	
	public void closeClient() {
		
		if(clientChannel != null && clientChannel.isOpen()) {
			clientChannel.close();
		}
		
		if(agentChannel != null && agentChannel.isOpen()) {
			
			agentChannel.close();
			
		}
		
		if(isForward) {//通知转发服务关闭连接
			Message message = new Message(token, MessageType.CLOSE_CONNECTION, null);
			try {
				startup.writeAndFlush(message);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}

		if(workGroup != null) {
			workGroup.shutdownGracefully();
		}
		
		nettyClientMap.remove(token);
		
		if(conn != null) {
			logger.info("Disconnection {}.", conn);
		}
		
	}
	
	public boolean isForward() {
		return this.isForward;
	}
	
	@Override
	public String toString() {
		return conn.toString();
	}
}
