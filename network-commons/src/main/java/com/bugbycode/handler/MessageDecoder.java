package com.bugbycode.handler;


import com.bugbycode.config.HandlerConst;
import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;
import com.util.StringUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class MessageDecoder extends LengthFieldBasedFrameDecoder {

	private static final int HEADER_SIZE = HandlerConst.LENGTH_FIELD_OFFSET + HandlerConst.LENGTH_FIELD_LENGTH;
	
	public MessageDecoder(int maxFrameLength, 
			int lengthFieldOffset, 
			int lengthFieldLength, 
			int lengthAdjustment,
			int initialBytesToStrip) {
		super(maxFrameLength, lengthFieldOffset, lengthFieldLength, 
				lengthAdjustment, initialBytesToStrip);
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {

		Message message = new Message();
		
		try {
			in = (ByteBuf) super.decode(ctx,in);
			
			if(in == null){
				return null;
	        }
			
			if(in.readableBytes() < HEADER_SIZE){
				return null;
	        }
			
			//读取消息类型总共1字节
			int type = in.readByte() & 0xFF;
			
			message.setType(type);
			//读取长度 总共4个字节
			int length = in.readInt();
			
			if(in.readableBytes() != length) {
				return null;
			}
			
			if(type != MessageCode.HEARTBEAT) {
				
				//读取token信息 总共16个字节
				byte[] token_buff = new byte[0x10];
				in.readBytes(token_buff); 
				
				int data_len = length - token_buff.length;
				byte[] data = {};
				
				if(data_len > 0) {
					
					data = new byte[data_len];
					
					in.readBytes(data);
				}
				
				String token = StringUtil.byteArrayToHexString(token_buff, token_buff.length);
				message.setToken(token);
				
				//以下是消息内容
				if(type == MessageCode.CONNECTION) {
					
					int port = ((data[0] << 0x08) & 0xFFFF) | (data[1] & 0xFF);
					
					String host = new String(data, 0x2, data.length - 0x2);
					
					message.setData(new ConnectionInfo(host,port));
					
				}else if(type == MessageCode.TRANSFER_DATA) {
					message.setData(data);
				}
			}
			
			return message;
		}catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if(in != null) {
				in.release();
			}
		}
	}
}
