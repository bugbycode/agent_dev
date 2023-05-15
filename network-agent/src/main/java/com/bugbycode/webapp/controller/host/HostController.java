package com.bugbycode.webapp.controller.host;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bugbycode.mapper.host.HostMapper;
import com.bugbycode.module.host.HostModule;
import com.util.StringUtil;
import com.util.page.Page;
import com.util.page.SearchResult;

@RestController
public class HostController {
	
	@Autowired
	private HostMapper hostMapper;

	@GetMapping("/query")
	public SearchResult<HostModule> query(
			@RequestParam(name = "queryParam", defaultValue = "")
			String keyword,
			@RequestParam(name = "offset", defaultValue = "0")
			int offset,
			@RequestParam(name = "limit", defaultValue = "20")
			int limit) {
		SearchResult<HostModule> sr = new SearchResult<HostModule>();
		
		if(StringUtil.isNotEmpty(keyword)) {
			keyword = "%" + keyword + "%";
		}
		
		int totalCount = hostMapper.count(keyword);
		List<HostModule> list = hostMapper.query(keyword, offset, limit);
		
		Page page = new Page(limit, offset);
		page.setTotalCount(totalCount);
		sr.setPage(page);
		sr.setList(list);
		return sr;
	}
	
	@PostMapping("/updateForwardById")
	public int updateForwardById(int id,int forward) {
		return hostMapper.updateForwardById(id, forward);
	}
}
