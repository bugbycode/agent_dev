package com.util;

public class StringUtil {

	private static final char[] HEX_CHAR_ARR = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
			'E', 'F' };

	public static boolean isBlank(String str) {
		int strLen;
		if (str == null || (strLen = str.length()) == 0)
			return true;
		for (int i = 0; i < strLen; i++)
			if (!Character.isWhitespace(str.charAt(i)))
				return false;
		return true;
	}

	public static boolean isNotBlank(String str) {
		return !isBlank(str);
	}

	public static boolean isEmpty(String str) {
		return str == null || str.length() == 0;
	}

	public static boolean isNotEmpty(String str) {
		return !isEmpty(str);
	}


	public static String byteToHexString(byte[] buff, int len) {
		StringBuilder builder = new StringBuilder();
		int index = 0;
		while (index < len) {
			if (index > 0) {
				builder.append(' ');
			}

			int number = buff[index++] & 0xFF;

			if (number < 0x10) {
				builder.append(0x0);
			}

			builder.append(Integer.toHexString(number));
		}

		return builder.toString();
	}

	public static String byteArrayToHexString(byte[] buff, int len) {
		StringBuilder builder = new StringBuilder();
		int index = 0;
		while (index < len) {

			int number = buff[index++] & 0xFF;

			if (number < 0x10) {
				builder.append(0x0);
			}

			builder.append(Integer.toHexString(number));
		}

		return builder.toString();
	}

	public static byte[] hexStringToByteArray(String str) {
		str = str.toUpperCase();
		int str_len = str.length();
		int buf_len = str_len / 2;
		byte[] buf = {};
		if (str_len % 2 == 0) {
			buf = new byte[buf_len];
			for (int index = 0; index < buf_len; index++) {
				int hex_index = index * 2;
				byte fh = hexCharToByte(str.charAt(hex_index++));
				byte lh = hexCharToByte(str.charAt(hex_index));
				buf[index] = (byte) ((fh << 0x04) | (lh & 0xFF));
			}
		}
		return buf;
	}

	public static byte hexCharToByte(char hex) {
		byte result = 0;
		for (byte index = 0; index < HEX_CHAR_ARR.length; index++) {
			if (HEX_CHAR_ARR[index] == hex) {
				result = index;
				break;
			}
		}
		return result;
	}
	
	public static String formatIpv4Address(byte[] data) {
		return String.format("%d.%d.%d.%d", data[0] & 0xFF,data[1] & 0xFF,data[2] & 0xFF,data[3] & 0xFF);
	}

}
