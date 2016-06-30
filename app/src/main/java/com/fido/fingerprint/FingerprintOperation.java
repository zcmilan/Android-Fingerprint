package com.fido.fingerprint;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.CryptoObject;
import android.os.CancellationSignal;

import com.fido.utils.Logger;


@SuppressLint("NewApi")
public class FingerprintOperation {
	
	private static final String TAG = FingerprintOperation.class.getSimpleName();

	private FingerprintManager fptMngr;
	private CancellationSignal cancelSignal;
	
	public FingerprintOperation(Context context){
		fptMngr = context.getSystemService(FingerprintManager.class);
		cancelSignal = new CancellationSignal();
	}

	public void startListening(CryptoObject mCryptoObject,AuthenticationCallback callback) {
		Logger.d(TAG, "startListening called");
		Logger.d(TAG+"crypto in", "startListening:"+mCryptoObject);
		fptMngr.authenticate(mCryptoObject, cancelSignal, 0, callback, null);
	}

	public void stopListening() {
		Logger.d(TAG, "stopListening called");
		cancelSignal.cancel();
	}

	public boolean hasEnrolledFingerprints() {
		Logger.d(TAG, "hasEnrolledFingerprints called");
		return fptMngr.hasEnrolledFingerprints();
	}

	public boolean isHardwareDetected() {
		Logger.d(TAG, "isHardwareDetected called");
		return fptMngr.isHardwareDetected();
	}

}
