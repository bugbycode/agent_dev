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
		return buf[3] & 0xFF | 
				buf[2] & 0xFF << 0x08 |
				buf[1] & 0xFF << 0x10 |
				buf[0] & 0xFF << 0x18 ;
	}
	
	/**
	 *  高位在前低位在后
	 * @param len
	 * @return
	 */
	public static byte[] toLenLong(long len) {
		byte[] buf = new byte[8];
		buf[0] = (byte)(len >>> 0x38);
		buf[1] = (byte)(len >>> 0x30);
		buf[2] = (byte)(len >>> 0x28);
		buf[3] = (byte)(len >>> 0x20);
		buf[4] = (byte)(len >>> 0x18);
		buf[5] = (byte)(len >>> 0x10);
		buf[6] = (byte)(len >>> 0x08);
		buf[7] = (byte)(len);
		return buf;
	}
	
	/**
	 * 高位在前低位在后
	 * @param buf
	 * @return
	 */
	public static long toLenLong(byte[] buf) {
		return (long)(buf[7] & 0xFF) | 
				(long)(buf[6] & 0xFF) << 0x08 |
				(long)(buf[5] & 0xFF) << 0x10 |
				(long)(buf[4] & 0xFF) << 0x18 |
				(long)(buf[3] & 0xFF) << 0x20 |
				(long)(buf[2] & 0xFF) << 0x28 |
				(long)(buf[1] & 0xFF) << 0x30 |
				(long)(buf[0] & 0xFF) << 0x38;
		
	}
}
