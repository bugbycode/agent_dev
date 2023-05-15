package com.util;

public class TransferUtil {

	/**
	 *  高位在前低位在后
	 * @param len
	 * @return
	 */
	public static byte[] toLen(int len) {
		byte[] buf = new byte[4];
		buf[0] = (byte)(len >>> 0x18);
		buf[1] = (byte)(len >>> 0x10);
		buf[2] = (byte)(len >>> 0x08);
		buf[3] = (byte)(len);
		return buf;
	}
	
	/**
	 * 高位在前低位在后
	 * @param buf
	 * @return
	 */
	public static int toLen(byte[] buf) {
		return (buf[3] & 0xFF) | ((buf[2] << 0x08) & 0xFF00)
				| ((buf[1] << 0x10) & 0xFF0000) | ((buf[0] << 0x18) & 0xFF000000);
	}
}
