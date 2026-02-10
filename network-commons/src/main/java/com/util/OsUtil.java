package com.util;

import com.bugbycode.module.SysOs;

public class OsUtil {

	public static SysOs getOsInfo() {
		return SysOs.parse(System.getProperty("os.name"));
	}
}
