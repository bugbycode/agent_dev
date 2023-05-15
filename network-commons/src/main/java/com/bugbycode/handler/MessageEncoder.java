package com.bugbycode.handler;

import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;
import com.util.StringUtil;
import com.bugbycode.module.Authentication;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessageEncoder extends MessageToByteEncoder<Message> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
		String token = msg.getToken();
		//首先发送token
		byte[] token_buf;
		if(token == null) {
			token_buf = new byte[0x10];
		}else {
			token_buf = StringUtil.hexStringToByteArray(token);
		}
		
		out.writeBytes(token_buf);
		//发送消息类型
		int type = msg.getType();
		out.writeByte(type);
		//计算长度
		Object obj = msg.getData();
		byte[] body;
		if(type == MessageCode.AUTH) {
			Authentication auth = (Authentication) obj;
			String authInfo = auth.toString();
			body = authInfo.getBytes();
		}else if(type == MessageCode.CONNECTION) {
			ConnectionInfo conn = (ConnectionInfo) obj;
			String connInfo = conn.toString();
			body = connInfo.getBytes();
		}else if(type == MessageCode.TRANSFER_DATA) {
			body = (byte[]) obj;
		}else {
			body = new byte[0];
		}
		
		int length = body.length;
		//发送消息长度和内容
		out.writeInt(length);
		if(length > 0) {
			out.writeBytes(body);
		}
	}

}