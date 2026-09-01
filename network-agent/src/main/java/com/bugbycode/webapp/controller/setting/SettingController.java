package com.bugbycode.webapp.controller.setting;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bugbycode.conf.AppConfig;
import com.bugbycode.mapper.setting.SettingMapper;
import com.bugbycode.module.setting.Setting;
import com.util.OsUtil;
import com.util.ProxyUtil;
import com.util.StringUtil;

@RestController
@RequestMapping("/api/setting")
public class SettingController {

	@Autowired
	private SettingMapper settingMapper;
	
	@GetMapping("/getSetting")
	public Setting getSetting() {
		
		Setting setting = settingMapper.getSetting();
		
		if(setting == null) {
			setting = new Setting();
		}
		
		return setting;
	}
	
	@PostMapping("/update")
	public Map<String,Object> update(
			@RequestParam("agentPort") int agentPort,
			@RequestParam("soBacklog") int soBacklog,
			@RequestParam("serverPort") int serverPort,
			@RequestParam("serverAddress") String serverAddress,
			@RequestParam("proxyStatus") int proxyStatus,
			@RequestParam("keystorePassword") String keystorePassword,
			@RequestParam(value = "file", required = false) MultipartFile file) {
		
		int code = 0;
		String message = "修改成功";
		
		try {
			
			Setting setting = new Setting();
			setting.setAgentPort(agentPort);
			setting.setSoBacklog(soBacklog);
			setting.setServerPort(serverPort);
			setting.setServerAddress(serverAddress);
			setting.setProxyStatus(proxyStatus);
			setting.setKeystorePassword(keystorePassword);
			
			if(!(file == null || file.isEmpty())) {
				if(!"application/x-pkcs12".equals(file.getContentType())) {
					throw new RuntimeException("请选择并上传pkcs12格式证书");
				}
				
				String cerDir = OsUtil.getUserHome() + File.separator + ".agentCer";
				File f = new File(cerDir);
				f.mkdirs();
				
				String filePath = cerDir + File.separator + "client.p12";
				
				System.out.println(filePath);
				
				file.transferTo(new File(filePath));
			}
			
			Setting dbSetting = settingMapper.getSetting();
			
			if(dbSetting == null) {
				settingMapper.insert(setting);
			} else {
				if(StringUtil.isEmpty(keystorePassword)) {
					setting.setKeystorePassword(dbSetting.getKeystorePassword());
				}
				setting.setId(dbSetting.getId());
				settingMapper.update(setting);
			}
			
			AppConfig.AGENT_PORT = setting.getAgentPort();
			AppConfig.SO_BACK_LOG = setting.getSoBacklog();
			AppConfig.SERVER_PORT = setting.getServerPort();
			AppConfig.SERVER_ADDRESS = setting.getServerAddress();
			AppConfig.PROXY_STATUS = setting.getProxyStatus();
			AppConfig.KEYSTORE_PASSWORD = setting.getKeystorePassword();
			
			AppConfig.START_UP.restart();
			AppConfig.SERVER.restart();
			
			ProxyUtil.setProxy("localhost", AppConfig.AGENT_PORT, AppConfig.PROXY_STATUS);
		} catch (Exception e) {
			code = 1;
			message = e.getMessage();
		}

		Map<String,Object> map = new HashMap<String,Object>();
		map.put("code", code);
		map.put("message", message);
		
		return map;
	}
}
