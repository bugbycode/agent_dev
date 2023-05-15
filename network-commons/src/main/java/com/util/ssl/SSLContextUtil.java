package com.util.ssl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

public class SSLContextUtil {
	public static SslContext getServerContext(String keystorePath,String password) {
		SslContext sslContext;
		TrustManagerFactory tf;
		KeyManagerFactory keyManagerFactory = null;
		try {
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(new FileInputStream(keystorePath), password.toCharArray());
			keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
			keyManagerFactory.init(keyStore,password.toCharArray());
			
			tf = TrustManagerFactory.getInstance("SunX509");
            tf.init(keyStore);
			
			sslContext = SslContextBuilder.forServer(keyManagerFactory).trustManager(tf).build();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (KeyStoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (SSLException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (CertificateException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
		return sslContext;
	}
	
	public static SslContext getClientContext(String keystorePath,String password) {
		TrustManagerFactory tf = null; 
		SslContext sslContext = null;
		try {
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(new FileInputStream(keystorePath), password.toCharArray());
			tf = TrustManagerFactory.getInstance("SunX509");
            tf.init(keyStore);
            sslContext = SslContextBuilder.forClient().trustManager(tf).build();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (CertificateException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (KeyStoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
		return sslContext;
	}
}
