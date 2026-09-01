package com.bugbycode.webapp.controller.base;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bugbycode.module.user.UserInfo;

public class BaseController {

	protected UserInfo getUserInfo() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		UserInfo user = null;
		if(auth.isAuthenticated()) {
			user = (UserInfo)auth.getPrincipal();
			user.setPassword("");
		}
		return user;
	}
}
