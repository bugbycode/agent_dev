package com.bugbycode.https;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public final class HostX509TrustManager implements X509TrustManager {

	private X509TrustManager manager;

	public HostX509TrustManager(String keystorePath, String password) {

		char[] passwordArray = (password == null) ? null : password.toCharArray();

		InputStream in = null;
		try {
			KeyStore ks = KeyStore.getInstance("JKS");
			in = new FileInputStream(keystorePath);
			ks.load(in, passwordArray);

			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509", "SunJSSE");
			tmf.init(ks);
			TrustManager tms[] = tmf.getTrustManagers();

			for (int i = 0; i < tms.length; i++) {
				if (tms[i] instanceof X509TrustManager) {
					manager = (X509TrustManager) tms[i];
					return;
				}
			}

		} catch (KeyStoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (CertificateException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} finally {
			try {
				if(in != null) {
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		//manager.checkClientTrusted(chain, authType);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		//manager.checkServerTrusted(chain, authType);
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return manager.getAcceptedIssuers();
	}

}
