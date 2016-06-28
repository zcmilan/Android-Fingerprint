package com.fido.fingerprinttest;

import java.io.UnsupportedEncodingException;
import java.security.PublicKey;
import java.security.Signature;

import javax.crypto.Cipher;

import android.app.Activity;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.hardware.fingerprint.FingerprintManager.CryptoObject;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.fido.fingerprint.CryptoOperation;
import com.fido.fingerprint.CryptoOperation.TYPE;
import com.fido.fingerprint.FingerprintControl;
import com.fido.fingerprint.FingerprintControl.FingerCallback;
import com.fido.fingerprint.FingerprintControl.FingerResult;
import com.fido.fingerprint.crypto.CipherHelper;
import com.fido.fingerprint.crypto.SignHelper;
import com.fido.utils.Logger;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;


public class MainActivity extends Activity {

	private static final String TAG = MainActivity.class.getSimpleName();

	private static final String KEY_SIGN = "sign_key11";
	private static final String TEST_SIGN = "testsign";

	private static final String KEY_CIPHER = "cipher_key17";
	private static final String TEST_CIPHER = "testcipher";


	private Button btnCipherDecrypt = null;
	private Button btnCipherEncrypt = null;
	private Button btnSign = null;
	private Button btnSignCreate = null;
	private Button btnMac = null;

	private byte[] ciphed = null;

	private CryptoOperation crypt;
	/**
	 * ATTENTION: This was auto-generated to implement the App Indexing API.
	 * See https://g.co/AppIndexing/AndroidStudio for more information.
	 */
	private GoogleApiClient client;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
		initData();
		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
	}

	private void initView() {
		btnCipherDecrypt = (Button) findViewById(R.id.btn_cipher_decrypt);
		btnCipherEncrypt = (Button) findViewById(R.id.btn_cipher_encrypt);
		btnSign = (Button) findViewById(R.id.btn_sign);
		btnSignCreate = (Button) findViewById(R.id.btn_sign_create);
		btnMac = (Button) findViewById(R.id.btn_mac);

		btnCipherDecrypt.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final CryptoObject object = crypt.createCryptoObject(TYPE.CIPHER_DECRYPT, KEY_CIPHER);
				new Thread(new Runnable() {
					@Override
					public void run() {
						FingerprintControl.getInstance().showUI(MainActivity.this, object, new FingerCallback() {
							@Override
							public void onResult(FingerResult result, String des, AuthenticationResult fingerResult) {
								if (result == FingerResult.SUCCESS) {
									Logger.d(TAG, "authenticate success");

									Cipher cipher = fingerResult.getCryptoObject().getCipher();
									byte[] original = CipherHelper.cipherDencrypt(cipher, ciphed);
									try {
										Logger.d(TAG, "authenticate data:" + new String(original,"utf-8"));
									} catch (UnsupportedEncodingException e) {
										e.printStackTrace();
									}
								} else {
									Logger.d(TAG, "authenticate failed");
								}
							}
						});
					}
				}).start();
			}
		});
		btnCipherEncrypt.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Logger.d(TAG, "create cipher");
				crypt.createKey(TYPE.CIPHER_ENCRYPT, KEY_CIPHER);
				final CryptoObject object = crypt.createCryptoObject(TYPE.CIPHER_ENCRYPT, KEY_CIPHER);
				new Thread(new Runnable() {
					@Override
					public void run() {
						FingerprintControl.getInstance().showUI(MainActivity.this, object, new FingerCallback() {
							@Override
							public void onResult(FingerResult result, String des, AuthenticationResult fingerResult) {
								if (result == FingerResult.SUCCESS) {
									Logger.d(TAG, "authenticate success");
									byte[] testStr = TEST_CIPHER.getBytes();

									Cipher cipher = fingerResult.getCryptoObject().getCipher();
									ciphed = CipherHelper.cipherEncrypt(cipher, testStr);
									Logger.d(TAG, "authenticate ciphered:" + Base64.encodeToString(ciphed, Base64.DEFAULT));

								} else {
									Logger.d(TAG, "authenticate failed");
								}
							}
						});
					}
				}).start();
			}
		});
		btnSignCreate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Logger.d(TAG, "create sign");
				crypt.createKey(TYPE.SIGN, KEY_SIGN);
				PublicKey publickey = SignHelper.signGetPublickey(KEY_SIGN);
				Logger.d(TAG, "create sign publickey:" + Base64.encodeToString(publickey.getEncoded(), Base64.DEFAULT));
			}
		});
		btnSign.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final CryptoObject object = crypt.createCryptoObject(TYPE.SIGN, KEY_SIGN);
				new Thread(new Runnable() {
					@Override
					public void run() {
						FingerprintControl.getInstance().showUI(MainActivity.this, object, new FingerCallback() {
							@Override
							public void onResult(FingerResult result, String des, AuthenticationResult fingerResult) {
								if (result == FingerResult.SUCCESS) {
									Logger.d(TAG, "authenticate success");
									byte[] testStr = TEST_SIGN.getBytes();

									Signature signature = fingerResult.getCryptoObject().getSignature();
									byte[] signed = SignHelper.signUpdate(signature, testStr);
									Logger.d(TAG, "authenticate signed:" + Base64.encodeToString(signed, Base64.DEFAULT));

									PublicKey publickey = SignHelper.signGetPublickey(KEY_SIGN);
									Logger.d(TAG, "authenticate publickey:" + Base64.encodeToString(publickey.getEncoded(), Base64.DEFAULT));
									boolean verifyResult = SignHelper.signVerify(publickey, testStr, signed);

									Logger.d(TAG, "authenticate verifyResult:" + verifyResult);
								} else {
									Logger.d(TAG, "authenticate failed");
								}
							}
						});
					}
				}).start();
			}
		});
		btnMac.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

			}
		});
	}

	private void initData() {
		crypt = new CryptoOperation();
	}

	@Override
	public void onStart() {
		super.onStart();

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		client.connect();
		Action viewAction = Action.newAction(
				Action.TYPE_VIEW, // TODO: choose an action type.
				"Main Page", // TODO: Define a title for the content shown.
				// TODO: If you have web page content that matches this app activity's content,
				// make sure this auto-generated web page URL is correct.
				// Otherwise, set the URL to null.
				Uri.parse("http://host/path"),
				// TODO: Make sure this auto-generated app URL is correct.
				Uri.parse("android-app://com.fido.fingerprinttest/http/host/path")
		);
		AppIndex.AppIndexApi.start(client, viewAction);
	}

	@Override
	public void onStop() {
		super.onStop();

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		Action viewAction = Action.newAction(
				Action.TYPE_VIEW, // TODO: choose an action type.
				"Main Page", // TODO: Define a title for the content shown.
				// TODO: If you have web page content that matches this app activity's content,
				// make sure this auto-generated web page URL is correct.
				// Otherwise, set the URL to null.
				Uri.parse("http://host/path"),
				// TODO: Make sure this auto-generated app URL is correct.
				Uri.parse("android-app://com.fido.fingerprinttest/http/host/path")
		);
		AppIndex.AppIndexApi.end(client, viewAction);
		client.disconnect();
	}
}
