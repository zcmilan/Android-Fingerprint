package com.fido.crypto;

import android.hardware.fingerprint.FingerprintManager.CryptoObject;

import com.fido.crypto.impl.CipherHelper;
import com.fido.crypto.impl.SignHelper;
import com.fido.utils.Logger;

import javax.crypto.Cipher;

public class CryptoOperation {

    private static String aTAG = CryptoOperation.class.getSimpleName();

    private CryptoObject mCryptoObject;

    public CryptoOperation() {
    }

    public static enum TYPE {
        SIGN,//asymmetric key
        MAC,
        CIPHER_ENCRYPT,//symmetric key ,encrypt
        CIPHER_DECRYPT//symmetric key ,decrypt
        ;
    }

    public CryptoObject createCryptoObject(TYPE type, String keyName) {
        Logger.d(aTAG, "createCryptoObject");
        switch (type) {
            case SIGN:
                mCryptoObject = SignHelper.createSignCryptoObject(keyName);
                break;
            case MAC:
                break;
            case CIPHER_ENCRYPT:
                mCryptoObject = CipherHelper.createCipherCryptoObject(keyName,Cipher.ENCRYPT_MODE);
                break;
            case CIPHER_DECRYPT:
                mCryptoObject = CipherHelper.createCipherCryptoObject(keyName, Cipher.DECRYPT_MODE);
                break;
        }
        return mCryptoObject;
    }

    public void createKey(TYPE type, String keyName) {
        Logger.d(aTAG, "createKey");
        switch (type) {
            case SIGN:
                SignHelper.signCreate(keyName);
                break;
            case CIPHER_ENCRYPT:
                CipherHelper.cipherCreate(keyName);
                break;
            case MAC:
                break;
            default:
                break;
        }
    }

    private void cipherCreate() {

    }
}
