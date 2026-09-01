package com.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Util {
	public static String md5(String input) {
        try {
            // 获取 MD5 消息摘要实例
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 将输入的字符串转换为字节数组，并计算其摘要
            byte[] messageDigest = md.digest(input.getBytes());

            // 将字节数组转换为十六进制的字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                // 每个字节转换为两位的十六进制数
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        String input = "admin123";
        String md5Hash = md5(input);
        System.out.println("MD5 hash of " + input + ": " + md5Hash);
    }
}
