package com.bugbycode.service.testnet.impl;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.bugbycode.service.testnet.TestnetService;

@Service("testnetService")
public class TestnetServiceImpl implements TestnetService {

	private final Logger logger = LogManager.getLogger(TestnetServiceImpl.class);
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Override
	public HttpStatus testHttp(String url) {
		HttpStatus status = HttpStatus.REQUEST_TIMEOUT;
		try {
			ResponseEntity<String> result = restTemplate.getForEntity(new URI(url), String.class);
			status = HttpStatus.resolve(result.getStatusCode().value());
		} catch (RestClientException e) {
			logger.error(e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			logger.error(e.getLocalizedMessage());
		}
		return status == null ? HttpStatus.OK : status;
	}

	@Override
	public boolean checkHttpConnect(String url) {
		
		boolean result = true;
		
		HttpStatus status = testHttp(url);
		switch (status) {
		case FORBIDDEN:
			result = false;
			break;
		case BAD_GATEWAY:
			result = false;
			break;
		case NOT_FOUND:
			result = false;
			break;
		case REQUEST_TIMEOUT:
			result = false;
			break;
		default:
			break;
		}
		
		return result;
	}

}
