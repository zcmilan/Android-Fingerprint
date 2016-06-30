package com.fido.fingerprint;

import java.lang.ref.WeakReference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.CryptoObject;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.telecom.Call;

import com.fido.fingerprinttest.FpActivity;
import com.fido.utils.Logger;

@SuppressLint("NewApi")
public class FpMatcher {

	private Context mContext;
	static final String TAG = FpMatcher.class.getSimpleName();

	// param input
	private FpActivity.FpParameter paramIn;
	private CryptoObject mCryptoObjectIn;// show UI cryptoObject
	private Messenger mMessenger;
	// param ouput
	private FpActivity.FpResult paramOut;
	private FP_RESULT mFPResult;// show UI FPResult
	private FingerprintManager.AuthenticationResult mAuthenticationOut;
	private CryptoObject mCryptoObjectOut;//show UI FPResult cryptoObject
	
	private Object lockObject = new Object();

	public FpMatcher(Context context) {
		this.mContext = context;
		mFPResult = FP_RESULT.ERRORAUTH;
	}

	public FpActivity.FpResult showUI(FpActivity.FpParameter parameter) {
		paramIn = parameter;
		mCryptoObjectIn = parameter.getmCryptoObject();
		CallbackHandler mHandler = new CallbackHandler(Looper.getMainLooper());
		mHandler.setMatcher(this);
		mMessenger = new Messenger(mHandler);

		FpActivity.showUI(mContext, parameter, mMessenger);

		Logger.d(TAG, "showUI prepare lock");
		lock();
		Logger.d(TAG, "showUI prepare going");
		return paramOut;
	}

	private void lock() {
		Logger.d(TAG, "lock lockObject:" + lockObject);
		synchronized (lockObject) {
			try {
				lockObject.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private synchronized void unlock() {
		Logger.d(TAG, "unlock lockObject:" + lockObject);
		synchronized (lockObject) {
			lockObject.notify();
		}
	}

	private void setResult(FpActivity.FpResult result) {
		Logger.d(TAG, "setResult FPResult:" + result);
		this.paramOut = result;
		this.mFPResult = result.FPResult;
		if(mFPResult == FP_RESULT.SUCCESS){
			this.mCryptoObjectOut = result.crypt.getCryptoObject();
		}
	}

	private static class CallbackHandler extends Handler {
		private WeakReference<FpMatcher> wake = null;
		
		public CallbackHandler(Looper looper){
			super(looper);
		}

		public void setMatcher(FpMatcher matcher) {
			wake = new WeakReference<FpMatcher>(matcher);
			if (wake != null && wake.get() != null) {
				Logger.e(TAG, "init wake is:" + wake.get());
			} else {
				Logger.e(TAG, "init wake is null");
			}
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Logger.d(TAG, "handleMessage");
			FpActivity.FpResult result = (FpActivity.FpResult) msg.obj;
			if (wake != null && wake.get() != null) {
				FpMatcher matcher = wake.get();
				matcher.setResult(result);
				matcher.unlock();
			} else {
				Logger.e(TAG, "wake is null");
			}
		}
	}

	public static enum FP_RESULT {
		SUCCESS, CANCEL, CHANGE_AUTHENTICATOR, CHANGE_TRANSACTION, MISMATCH, TOOMANYATTEMPTS, TIMEOUT, ERRORAUTH, USER_LOCKOUT;

		private FP_RESULT() {}
	}
}