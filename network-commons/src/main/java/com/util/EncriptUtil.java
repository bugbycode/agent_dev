package com.util;

public class EncriptUtil {

	private byte[] KEY = {0x59,(byte)0xFE,0x17};
	
	public void encriptArray(byte[] buff) {
		int len = buff.length;
		int key_len = KEY.length;
		for(int index = 0;index < len;index++) {
			for(int key_index = 0;key_index < key_len;key_index++) {
				buff[index] = (byte)((buff[index] ^ KEY[key_index]) & 0xFF);
			}
		}
	}
	
}
