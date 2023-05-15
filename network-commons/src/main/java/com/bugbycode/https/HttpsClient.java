package com.bugbycode.https;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import sun.misc.BASE64Encoder;

public class HttpsClient {
	
	private SSLContext sslContext;
	
	public HttpsClient(String keystorePath, String password) {
		TrustManager[] tm = {new HostX509TrustManager(keystorePath,password)};
		KeyManager[] km = {new HostX509KeyManager(keystorePath,password)};
		try {
			this.sslContext = SSLContext.getInstance("SSLv3");
			sslContext.init(km, tm, new SecureRandom());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (KeyManagementException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	public HttpsURLConnection getHttpsURLConnection(String url) throws MalformedURLException, IOException, NoSuchAlgorithmException {
		HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
		SSLSocketFactory ssf = sslContext.getSocketFactory();
		conn.setSSLSocketFactory(ssf);
		conn.setHostnameVerifier(new TrustAnyHostnameVerifier());
		return conn;
	}
	
	public String checkToken(String url,String client_id,String client_secret,String token) {
		token = "token=" + token;

		String auth = client_id + ":" + client_secret;
		
		return sendData(url,auth,token.getBytes(),null);
	}
	
	public String getToken(String url,String grant_type,String client_id,String client_secret,String scope) {
		
		StringBuilder build = new StringBuilder();
		
		build.append("grant_type=" + grant_type);
		build.append("&client_id=" + client_id);
		build.append("&client_secret=" + client_secret);
		build.append("&scope=" + scope);
		
		return sendData(url,null,build.toString().getBytes(),null);
	}
	
	public String getResource(String url,String token,Map<String,Object> data) {
		StringBuilder builder = new StringBuilder();
		Iterator<Entry<String,Object>> it = data.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String,Object> entry = it.next();
			String key = entry.getKey();
			String value = entry.getValue().toString();
			if(builder.length() > 0) {
				builder.append('&');
			}
			builder.append(key);
			builder.append("=");
			builder.append(value);
		}
		return sendData(url, null, builder.toString().getBytes(), token);
	}
	
	public String sendData(String url,String auth,byte[] data,String token) {
		HttpsURLConnection conn = null;
		InputStream in = null;
		OutputStream out = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		
		StringBuilder build = new StringBuilder();
		
		try {
			conn = getHttpsURLConnection(url);
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setConnectTimeout(5000);
			if(auth != null) {
				conn.setRequestProperty("accept", "*/*");
				conn.setRequestProperty("connection", "Keep-Alive");
				conn.setRequestProperty("Authorization", "Basic " + new BASE64Encoder().encode(auth.getBytes()));
			}else if(token != null) {
				conn.setRequestProperty("Authorization", "Bearer " + token);
			}
			
			out = conn.getOutputStream();
			out.write(data);
			out.flush();
			int code = conn.getResponseCode();
			if(code == 200) {
				in = conn.getInputStream();
			}else {
				in = conn.getErrorStream();
			}
			isr = new InputStreamReader(in, "UTF-8");
			br = new BufferedReader(isr);
			
			String line = null;
			while((line = br.readLine()) != null) {
				build.append(line);
			}
			return build.toString();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} finally {
			try {
				if(br != null) {
					br.close();
				}
				
				if(isr != null) {
					isr.close();
				}
				
				if(in != null) {
					in.close();
				}
				
				if(out != null) {
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(conn != null) {
				conn.disconnect();
			}
		}
	}
}
