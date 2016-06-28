package com.fido.fingerprint;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.hardware.fingerprint.FingerprintManager.CryptoObject;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.fido.fingerprinttest.FpActivity;
import com.fido.utils.Logger;

public class FingerprintControl {

	private static final String TAG = FingerprintControl.class.getSimpleName();
	
	private static CryptoObject crypt = null;
	
	private volatile FingerCallback callback = null;
	private Object lockObject = new Object();
	
	private static FingerprintControl instance;
	public static FingerprintControl getInstance(){
		if(instance == null){
			synchronized (FingerprintControl.class) {
				if(instance == null){
					instance = new FingerprintControl();
				}
			}
		}
		return instance;
	}
	
	private FingerprintControl(){}
	
	public void showUI(Context mContext,CryptoObject crypt,FingerCallback callback) {
		if(Looper.myLooper() == Looper.getMainLooper()){
			throw new IllegalStateException("can't use with main looper");
		}
		FingerprintControl.crypt = crypt;
		this.callback = callback;
		Intent intent = new Intent(mContext, FpActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		CallbackHandler handler = new CallbackHandler(Looper.getMainLooper());
		handler.setMatcher(this);
		intent.putExtra(FpActivity.REQUEST_MSNG, new Messenger(handler));
		intent.putExtra(FpActivity.REQUEST_COUNT,5);
		mContext.startActivity(intent);
		Logger.d(TAG, "showUI prepare lock");
		lock();
		Logger.d(TAG, "showUI prepare going");
		FingerprintControl.crypt = null;
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

	private static class CallbackHandler extends Handler {
		private WeakReference<FingerprintControl> wake = null;

		public CallbackHandler(Looper loop) {
			super(loop);
		}

		public void setMatcher(FingerprintControl matcher) {
			wake = new WeakReference<FingerprintControl>(matcher);
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
			if (wake != null && wake.get() != null) {
				FingerprintControl matcher = wake.get();
				matcher.unlock();
				if(msg.arg1 == FingerResult.SUCCESS.ordinal()){
					matcher.triggerCallback(FingerResult.SUCCESS,null,(AuthenticationResult)msg.obj);
				}else if(msg.arg1 == FingerResult.FAILED.ordinal()){
					matcher.triggerCallback(FingerResult.FAILED,null,null);
				}else if(msg.arg1 == FingerResult.CANCEL.ordinal()){
					matcher.triggerCallback(FingerResult.FAILED,null,null);
				}else {
					matcher.triggerCallback(FingerResult.FAILED,null,null);
				}
			} else {
				Logger.e(TAG, "wake is null");
			}
		}
	}
	
	private void triggerCallback(FingerResult result,String des,AuthenticationResult fingerResul){
		if(callback!=null){
			callback.onResult(result, des, fingerResul);
		}else {
			Log.e(TAG, "triggerCallback error , callback is null");
		}
	}
	
	public static interface FingerCallback{
		public void onResult(FingerResult result,String des,AuthenticationResult fingerResult);
	}
	
	public static enum FingerResult{
		SUCCESS,FAILED,CANCEL;
	}
	
	public static CryptoObject getCrypt(){
		return crypt;
	}
	
}
