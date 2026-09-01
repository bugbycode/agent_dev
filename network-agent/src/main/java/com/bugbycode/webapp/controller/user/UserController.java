package com.bugbycode.webapp.controller.user;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugbycode.mapper.user.UserMapper;
import com.bugbycode.module.user.UserInfo;
import com.bugbycode.webapp.controller.base.BaseController;
import com.util.MD5Util;
import com.util.StringUtil;

@RestController
@RequestMapping("/api/user")
public class UserController extends BaseController{

	@Autowired
	private UserMapper userMapper;
	
	@PostMapping("/updatePassword")
	public Map<String,Object> updatePassword(String oldPassword, String newPassword, String confirmPassword) {
		
		Map<String,Object> map = new HashMap<String,Object>();
		
		int code = 0;
		String message = "修改密码成功";
		
		UserInfo user = getUserInfo();
		UserInfo dbUser = userMapper.loadUserByUsername(user.getUsername());
		if(StringUtil.isEmpty(oldPassword)) {
			code = 1;
			message = "请输入旧密码";
		} else if(StringUtil.isEmpty(newPassword)) {
			code = 1;
			message = "请输入新密码";
		} else if(StringUtil.isEmpty(confirmPassword)) {
			code = 1;
			message = "请确认新密码";
		} else if(!newPassword.equals(confirmPassword)) {
			code = 1;
			message = "两次输入的密码不一致";
		} else if(!MD5Util.md5(oldPassword).equals(dbUser.getPassword())) {
			code = 1;
			message = "旧密码错误";
		} else {
			userMapper.updatePassword(dbUser.getUsername(), MD5Util.md5(newPassword));
		}
		
		map.put("code", code);
		map.put("message", message);
		return map;
	}
}
