package com.bugbycode.mapper.setting;

import com.bugbycode.module.setting.Setting;

public interface SettingMapper {

	public int insert(Setting setting);
	
	public Setting getSetting();
	
	public int update(Setting setting);
}
