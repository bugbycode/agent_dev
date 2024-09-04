package com.test;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestClientException;

import com.bugbycode.service.testnet.TestnetService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class TestApp {
	
	private final Logger logger = LogManager.getLogger(TestApp.class);
	
	@Autowired
	private TestnetService testnetService;
	
	@Test
	public void testMain() {
		HttpStatus result = testnetService.testHttp("https://www.binance.com1:443");
		logger.info(result);
	}
}
