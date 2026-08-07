package com.bugbycode.handler;

import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageType;
import com.util.DateFormatUtil;
import com.util.StringUtil;
import com.util.TransferUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessageEncoder extends MessageToByteEncoder<Message> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
		
		//消息格式
		// 类型|消息总长度|时间长度|时间信息|时区长度|时区信息|token长度|token信息|body长度|body信息
		
		//发送消息类型
		MessageType type = msg.getType();
		
		int length = 0;//消息总长度
		byte[] body = {};//body信息
		
		//时间信息
		long time = DateFormatUtil.getDate().getTime();
		byte[] time_buf = TransferUtil.toLenLong(time);
		//时区信息
		byte[] timezone_buf = DateFormatUtil.getTimeZoneId().getBytes("UTF-8");
		
		String token = msg.getToken();
		
		byte[] token_buf;
		if(token == null) {
			token_buf = new byte[0x10];
		}else {
			token_buf = StringUtil.hexStringToByteArray(token);
		}
		
		if(type == MessageType.HEARTBEAT){
			
		} else {
			
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
			
		}
		
		//计算总长度
		length = 4 + time_buf.length + 4 + timezone_buf.length + 4 + token_buf.length +
				4 + body.length;
		
		//发送消息类型
		out.writeByte(type.getValue());
		//发送消息总长度
		out.writeInt(length);
		//发送时间长度
		out.writeInt(time_buf.length);
		//发送时间信息
		out.writeBytes(time_buf);
		//发送时区长度
		out.writeInt(timezone_buf.length);
		//发送时区信息
		out.writeBytes(timezone_buf);
		//发送token长度
		out.writeInt(token_buf.length);
		//发送token信息
		out.writeBytes(token_buf);
		//发送body长度
		out.writeInt(body.length);
		//发送body信息
		out.writeBytes(body);
	}

}