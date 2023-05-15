package com.bugbycode.proxy.initializer;

import javax.net.ssl.SSLEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import com.bugbycode.config.HandlerConst;
import com.bugbycode.config.IdleConfig;
import com.bugbycode.handler.MessageDecoder;
import com.bugbycode.handler.MessageEncoder;
import com.bugbycode.proxy.handler.ServerHandler;
import com.util.ssl.SSLContextFactory;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;

@Configuration
@Service("serverChannelInitializer")
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

	@Value("${spring.keystore.path:server.keystore}")
	private String keyStorePath;
	
	@Value("${spring.keystore.password:changeit}")
	private String keyStorePassword;
	
	@Autowired
	private NioEventLoopGroup remoteGroup;
	
	public ServerChannelInitializer() {
		
	}
	
	@Override
	protected void initChannel(SocketChannel sc) throws Exception {
		ChannelPipeline p = sc.pipeline();
		
		//SSL双向认证
		SSLEngine engine = SSLContextFactory.getContext(keyStorePath, keyStorePassword).createSSLEngine();
		engine.setNeedClientAuth(true);
		engine.setUseClientMode(false);
        p.addLast("ssl",new SslHandler(engine));
        
		p.addLast(
				new IdleStateHandler(IdleConfig.READ_IDEL_TIME_OUT, IdleConfig.WRITE_IDEL_TIME_OUT, IdleConfig.ALL_IDEL_TIME_OUT),
				new MessageDecoder(HandlerConst.MAX_FRAME_LENGTH, HandlerConst.LENGTH_FIELD_OFFSET, 
						HandlerConst.LENGTH_FIELD_LENGTH, HandlerConst.LENGTH_AD_JUSTMENT, 
						HandlerConst.INITIAL_BYTES_TO_STRIP),
				new MessageEncoder(),
				new ServerHandler(remoteGroup)
		);
	}
}
