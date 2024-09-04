package com.bugbycode.service.testnet;

import org.springframework.http.HttpStatus;

public interface TestnetService {

	public HttpStatus testHttp(String url);
	
	public boolean checkHttpConnect(String url);
}
