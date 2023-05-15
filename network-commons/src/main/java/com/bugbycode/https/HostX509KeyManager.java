package com.bugbycode.https;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509KeyManager;

public final class HostX509KeyManager implements X509KeyManager {

	private X509KeyManager x509KeyManager;

	public HostX509KeyManager(String keystorePath, String password) {

		char[] passwordArray = (password == null) ? null : password.toCharArray();

		InputStream in = null;

		try {
			KeyStore ks = KeyStore.getInstance("JKS");
			in = new FileInputStream(keystorePath);
			ks.load(in, passwordArray);
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509", "SunJSSE");
			kmf.init(ks, "changeit".toCharArray());
			KeyManager[] kms = kmf.getKeyManagers();
			for (int i = 0; i < kms.length; i++) {
				if (kms[i] instanceof X509KeyManager) {
					x509KeyManager = (X509KeyManager) kms[i];
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
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
		return x509KeyManager.chooseClientAlias(keyType, issuers, socket);
	}

	@Override
	public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
		return x509KeyManager.chooseServerAlias(keyType, issuers, socket);
	}

	@Override
	public X509Certificate[] getCertificateChain(String alias) {
		return x509KeyManager.getCertificateChain(alias);
	}

	@Override
	public String[] getClientAliases(String keyType, Principal[] issuers) {
		return x509KeyManager.getClientAliases(keyType, issuers);
	}

	@Override
	public PrivateKey getPrivateKey(String alias) {
		return x509KeyManager.getPrivateKey(alias);
	}

	@Override
	public String[] getServerAliases(String keyType, Principal[] issuers) {
		return x509KeyManager.getServerAliases(keyType, issuers);
	}

}
