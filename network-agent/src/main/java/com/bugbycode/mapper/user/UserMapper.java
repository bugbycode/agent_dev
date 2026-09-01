package com.bugbycode.mapper.user;

import org.apache.ibatis.annotations.Param;

import com.bugbycode.module.user.UserInfo;

public interface UserMapper {

	public UserInfo loadUserByUsername(@Param("username") String username);
	
	public int insert(UserInfo user);
	
	public int updatePassword(@Param("username") String username, @Param("password") String password);
}
