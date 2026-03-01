package com.bugbycode.config;

public class IdleConfig {
	  public static final int READ_IDEL_TIME_OUT = 60; // 读超时 超过60秒未收到任何数据传输时将关闭连接
	  public static final int WRITE_IDEL_TIME_OUT = 20;// 写超时 每20秒发送一次心跳通信
	  public static final int ALL_IDEL_TIME_OUT = 60; // 没有数据传输的时间间隔
}
