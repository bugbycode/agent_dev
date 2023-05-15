package com.util.ssl;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class SSLContextFactory {
	
	public static SSLContext getContext(String keystorePath,String keystorePassword) {
		FileInputStream in = null;
		KeyStore keystore = null;
		SSLContext context = null;
		TrustManagerFactory tmf = null;
		KeyManagerFactory kmf = null;
		try {
			context = SSLContext.getInstance("ssl");
			keystore = KeyStore.getInstance("JKS");
			tmf = TrustManagerFactory.getInstance("SunX509");
			kmf = KeyManagerFactory.getInstance("SunX509");
			in = new FileInputStream(keystorePath);
			
			keystore.load(in, keystorePassword.toCharArray());
			tmf.init(keystore);
			kmf.init(keystore, keystorePassword.toCharArray());
			
			context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | 
				IOException | UnrecoverableKeyException | KeyManagementException e) {
			e.printStackTrace();
		} finally {
			try {
				if(in != null) {
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return context;
	}
}
