package com.util;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateFormatUtil {

	public static void main(String[] args) {
		Date now = getDate();
		long t = now.getTime();
		System.out.println(t);
		byte[] buf = TransferUtil.toLenLong(t);
		System.out.println(StringUtil.byteToHexString(buf, buf.length));
		long t2 = TransferUtil.toLenLong(buf);
		System.out.println(t2);
	}
	
	/**
	 * 获取本地时区信息 
	 * @return 返回时区信息 例如：Asia/Shanghai
	 */
	public static String getTimeZoneId() {
		return TimeZone.getDefault().getID();
	}
	
	/**
	 * 根据时区信息获取时间
	 * @param ID 时区信息 例如：Asia/Shanghai
	 * @return
	 */
	public static Date getDate(String ID) {
		return getDate(TimeZone.getTimeZone(ID));
	}
	
	/**
	 * 根据时区信息获取时间
	 * @param zone 时区信息 例如：Asia/Shanghai
	 * @return
	 */
	public static Date getDate(TimeZone zone) {
		return Calendar.getInstance(zone).getTime();
	}
	
	/**
	 * 获取本地时间
	 * @return
	 */
	public static Date getDate() {
		return Calendar.getInstance().getTime();
	}
}
