package com.bugbycode.service.user.impl;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bugbycode.mapper.user.UserMapper;
import com.bugbycode.module.user.UserInfo;
import com.bugbycode.service.user.UserService;
import com.util.StringUtil;

import jakarta.annotation.Resource;

@Service("userDetailsService")
public class UserServiceImpl implements UserService {

	@Resource
	private UserMapper userMapper;
	
	@Resource
	private PasswordEncoder passwordEncoder;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserInfo user = userMapper.loadUserByUsername(username);
		if(user == null) {
			throw new UsernameNotFoundException("用户名密码错误");
		}
		if(StringUtil.isNotEmpty(user.getPassword())) {
			user.setPassword(passwordEncoder.encode(user.getPassword()));
		}
		return user;
	}

}
