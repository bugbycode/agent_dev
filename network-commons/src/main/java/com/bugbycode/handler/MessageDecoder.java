package com.bugbycode.handler;


import com.bugbycode.config.HandlerConst;
import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageType;
import com.util.StringUtil;
import com.util.TransferUtil;

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

		//消息格式
		// 类型|消息总长度|时间长度|时间信息|时区长度|时区信息|token长度|token信息|body长度|body信息
		
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
			
			MessageType messageType = message.resolve(type);
			
			message.setType(messageType);
			
			//读取长度 总共4个字节
			int length = in.readInt();
			
			if(in.readableBytes() != length) {
				return null;
			}
			
			//读取时间长度
			int time_length = in.readInt();
			byte[] time_buf = new byte[time_length];
			//读取时间信息
			in.readBytes(time_buf);
			message.setTime(TransferUtil.toLenLong(time_buf));
			
			//读取时区长度
			int timezone_length = in.readInt();
			byte[] timezone_buf = new byte[timezone_length];
			//读取时区信息
			in.readBytes(timezone_buf);
			message.setTimezone(new String(timezone_buf));
			
			//读取token长度
			int token_len = in.readInt();
			//读取token信息
			byte[] token_buff = new byte[token_len];
			in.readBytes(token_buff); 
			
			//读取body长度
			int body_length = in.readInt();
			//读取body信息
			byte[] data = new byte[body_length];
			in.readBytes(data);
			
			String token = StringUtil.byteArrayToHexString(token_buff, token_buff.length);
			message.setToken(token);
			
			if(messageType == MessageType.HEARTBEAT) {
				
			} else if(messageType == MessageType.CONNECTION) {
					
				int port = ((data[0] << 0x08) & 0xFFFF) | (data[1] & 0xFF);
				
				String host = new String(data, 0x2, data.length - 0x2);
				
				message.setData(new ConnectionInfo(host,port));
				
			}else if(messageType == MessageType.TRANSFER_DATA) {
				message.setData(data);
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
