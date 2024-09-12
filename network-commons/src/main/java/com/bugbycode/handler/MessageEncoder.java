package com.bugbycode.handler;

import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageType;
import com.util.StringUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessageEncoder extends MessageToByteEncoder<Message> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
		
		//发送消息类型
		MessageType type = msg.getType();
		out.writeByte(type.getValue());
		int length = 0;;
		byte[] body = {};
		
		if(type == MessageType.HEARTBEAT){
			out.writeInt(0);
		}else {
			
			String token = msg.getToken();
			
			byte[] token_buf;
			if(token == null) {
				token_buf = new byte[0x10];
			}else {
				token_buf = StringUtil.hexStringToByteArray(token);
			}
			
			//计算长度
			Object obj = msg.getData();
			if(type == MessageType.CONNECTION) {
				
				ConnectionInfo conn = (ConnectionInfo) obj;
				
				byte[] host_buf = conn.getHost().getBytes();
				int port = conn.getPort() & 0XFFFF; // 0~65535
				
				body = new byte[host_buf.length + 2];
				
				body[0] = (byte)((port >> 0x08) & 0xFF);
				body[1] = (byte)(port & 0xFF);
				
				System.arraycopy(host_buf, 0, body, 2, host_buf.length);
				
				
			}else if(type == MessageType.TRANSFER_DATA) {
				body = (byte[]) obj;
			}else {
				body = new byte[0];
			}

			//数据长度
			length = token_buf.length + body.length;
			
			//发送消息长度和内容
			out.writeInt(length);
			
			out.writeBytes(token_buf);
			out.writeBytes(body);
			
		}
		
	}

}