package com.fido.fingerprint.crypto;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.CryptoObject;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.fido.utils.Logger;

public class SignHelper {
	
	private static final String aTAG = SignHelper.class.getSimpleName();

	public static void signCreate(String keyName) {
		Logger.d(aTAG, "createSign keyName:"+keyName);
		try {
			KeyPairGenerator mKeyGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
			KeyStore mKeyStore = KeyStore.getInstance("AndroidKeyStore");
			mKeyStore.load(null);
			PrivateKey key = (PrivateKey) mKeyStore.getKey(keyName, null);
			if (key == null) {
				mKeyGenerator.initialize(
						new KeyGenParameterSpec.Builder(keyName, KeyProperties.PURPOSE_SIGN)
							.setDigests(KeyProperties.DIGEST_SHA256)
							.setUserAuthenticationRequired(true)
							.setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))//set algorithm
						.build());
				mKeyGenerator.generateKeyPair();
			} else {
				Logger.e(aTAG, "already init");
			}
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | CertificateException | KeyStoreException | IOException e) {
			Logger.e(aTAG, "Failed to createKey: " + e);
			throw new RuntimeException(e);
		} catch (UnrecoverableKeyException e) {
			Logger.e(aTAG, "Failed to createKey UnrecoverableKeyException: " + e);
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			Logger.e(aTAG, "Failed to createKey NoSuchProviderException: " + e);
			e.printStackTrace();
		}
	}
	
	public static PublicKey signGetPublickey(String keyName){
		Logger.d(aTAG, "signGetPublickey keyName:"+keyName);
		PublicKey publicKey = null;
		KeyStore keyStore;
		try {
			keyStore = KeyStore.getInstance("AndroidKeyStore");
			keyStore.load(null);
			publicKey = keyStore.getCertificate(keyName).getPublicKey();
			//must use X509 to extract publickey
			KeyFactory factory = KeyFactory.getInstance(publicKey.getAlgorithm());
			X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKey.getEncoded());
			PublicKey verificationKey = factory.generatePublic(spec);
			return verificationKey;
		} catch (KeyStoreException e) {
			Logger.d(aTAG, "signGetPublickey KeyStoreException:"+e.getMessage());
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			Logger.d(aTAG, "signGetPublickey NoSuchAlgorithmException:"+e.getMessage());
			e.printStackTrace();
		} catch (CertificateException e) {
			Logger.d(aTAG, "signGetPublickey CertificateException:"+e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Logger.d(aTAG, "signGetPublickey IOException:"+e.getMessage());
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static byte[] signUpdate(Signature signature,byte[] str){
		Logger.d(aTAG, "signUpdate signature:"+signature + " str:" + Base64.encode(str, Base64.DEFAULT));
		byte[] sigBytes = null;
		try {
			signature.update(str);
			sigBytes = signature.sign();
		} catch (SignatureException e) {
			Logger.d(aTAG, "signUpdate SignatureException:"+e.getMessage());
			e.printStackTrace();
		}
		return sigBytes;
	}
	
	public static boolean signVerify(PublicKey key,byte[] str,byte[] singed){
		Logger.d(aTAG, "signVerify");
		boolean result = false;
		Signature verification;
		try {
			verification = Signature.getInstance("SHA256withECDSA");
			verification.initVerify(key);
			Logger.d(aTAG, "signVerify initVerify finish");
			verification.update(str);
			Logger.d(aTAG, "signVerify signature:"+verification);
			if(verification.verify(singed)){
				result = true;
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		}
		return result;
	}

	private static Signature initSignature(String keyName) {
		Logger.d(aTAG, "initSignature");
		Signature signature = null;
		try {
			KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
			signature = Signature.getInstance("SHA256withECDSA");
			keyStore.load(null);
			PrivateKey key = (PrivateKey)keyStore.getKey(keyName, null);
			signature.initSign(key);
			Logger.d(aTAG, "initSignature:"+signature);
		} catch (KeyPermanentlyInvalidatedException e) {
			Logger.e(aTAG, "KeyPermanentlyInvalidatedException: " + e);
			return null;
		} catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
			Logger.e(aTAG, "Failed to initCipher: " + e);
			throw new RuntimeException("Failed to init Cipher", e);
		}
		return signature;
	}
	
	public static CryptoObject createSignCryptoObject(String keyName) {
		Logger.d(aTAG, "createCryptoObject");
		CryptoObject mCryptoObject = null;
		try {
			Signature signature = initSignature(keyName);
			if (signature!=null) {
				mCryptoObject = new FingerprintManager.CryptoObject(signature);
				return mCryptoObject;
			} else {
				Logger.e(aTAG, "initCipher failed!");
				return null;
			}
		} catch (RuntimeException e) {
			Logger.e(aTAG, "Failed to initSignature: " + e);
			return null;
		}
	}
	
}
