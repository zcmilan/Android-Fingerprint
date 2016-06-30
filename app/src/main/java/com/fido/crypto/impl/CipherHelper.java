package com.fido.crypto.impl;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.CryptoObject;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import com.fido.utils.Logger;
/*

	secertkey 的使用,全部需要进行验证,包括加密/解密.

 */
public class CipherHelper {

	private static final String aTAG = CipherHelper.class.getSimpleName();

	private static byte[] iv = null;

	private static byte[] getRawKey(byte[] seed) throws Exception {
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG", "Crypto");
		sr.setSeed(seed);
		kgen.init(128, sr); // 192 and 256 bits may not be available
		SecretKey skey = kgen.generateKey();
		byte[] raw = skey.getEncoded();
		return raw;

	}

	public static void cipherCreate(String keyName) {
		Logger.d(aTAG, "cipherCreate keyName:" + keyName);
		try {
			KeyStore mKeyStore = KeyStore.getInstance("AndroidKeyStore");
			mKeyStore.load(null);
			SecretKey key = (SecretKey)mKeyStore.getKey(keyName, null);
			if (key == null) {
				//keystore can't get secert key
				KeyGenerator mKeyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
				mKeyGenerator.init(new KeyGenParameterSpec.Builder(keyName, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
						.setBlockModes(KeyProperties.BLOCK_MODE_CBC)
						.setUserAuthenticationRequired(true)
						.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
						.build());
				mKeyGenerator.generateKey();
			} else {
				Logger.e(aTAG, "already init");
			}

		} catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e) {
			Logger.e(aTAG, "Failed to createKey: " + e);
			throw new RuntimeException(e);
		} catch (UnrecoverableKeyException e) {
			Logger.e(aTAG, "Failed to createKey UnrecoverableKeyException: " + e);
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			Logger.e(aTAG, "Failed to createKey NoSuchProviderException: " + e);
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
	}

	public static byte[] cipherEncrypt(Cipher mCipher, byte[] str) {
		byte[] result = null;
		try {
			mCipher.update(str);
			iv = mCipher.getIV();
			result = mCipher.doFinal();
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
			Logger.d(aTAG, "cipherEncrypt error");
		}
		return result;
	}

	public static byte[] cipherDencrypt(Cipher mCipher, byte[] str) {
		byte[] result = null;
		try {
			mCipher.update(str);
			result = mCipher.doFinal();
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
			Logger.d(aTAG, "cipherDecrypt error");
		}
		return result;
	}

	public static Cipher getCipher(String keyName, int cipherType) {
		Logger.d(aTAG, "initCipher");
		Cipher mCipher = null;
		try {
			mCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
		} catch (NoSuchAlgorithmException e) {
			Logger.e(aTAG, "Failed to initCipher: " + e);
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}
		return mCipher;
	}
	

	public static CryptoObject createCipherCryptoObject(String keyName,int cipherType) {
		Logger.d(aTAG, "createCryptoObject");
		CryptoObject mCryptoObject = null;
		try {
			Cipher mCipher = getCipher(keyName, cipherType);
			KeyStore mKeyStore = KeyStore.getInstance("AndroidKeyStore");
			mKeyStore.load(null);
			SecretKey key = (SecretKey) mKeyStore.getKey(keyName, null);
			if(cipherType == Cipher.DECRYPT_MODE){
				mCipher.init(cipherType,key,new IvParameterSpec(iv));

			}else{
				mCipher.init(cipherType,key);
			}
			if (mCipher!=null) {
				mCryptoObject = new FingerprintManager.CryptoObject(mCipher);
				return mCryptoObject;
			} else {
				Logger.e(aTAG, "initCipher failed!");
				return null;
			}
		} catch (RuntimeException e) {
			Logger.e(aTAG, "Failed to initCipher: " + e);
			return null;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		return mCryptoObject;
	}

}
