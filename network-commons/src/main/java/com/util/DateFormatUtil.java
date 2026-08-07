package com.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateFormatUtil {

	private static final ThreadLocal<SimpleDateFormat> sdf = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS XXX"));
	
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
	
	/**
	 * 根据时区和时间获取时间信息
	 * @param zone 时区信息 例如：Asia/Shanghai
	 * @param time
	 * @return
	 */
	public static Date getDate(TimeZone zone, long time) {
		Calendar c = Calendar.getInstance(zone);
		c.setTime(new Date(time));
		return c.getTime();
	}
	
	/**
	 * 根据时区和时间获取时间信息
	 * @param ID 时区信息 例如：Asia/Shanghai
	 * @param time
	 * @return
	 */
	public static Date getDate(String ID, long time) {
		return getDate(TimeZone.getTimeZone(ID), time);
	}
	
	/**
	 * 格式化时间
	 * @param time
	 * @return
	 */
	public static String format(long time) {
		return format(new Date(time));
	}
	
	/**
	 * 格式化时间
	 * @param date
	 * @return
	 */
	public static String format(Date date) {
		return sdf.get().format(date);
	}
}
