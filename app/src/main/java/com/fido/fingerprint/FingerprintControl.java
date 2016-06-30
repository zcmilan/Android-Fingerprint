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

	private FpMatcher mMatcher = null;

	private static FingerprintControl instance;
	public static FingerprintControl getInstance(Context ctx){
		if(instance == null){
			synchronized (FingerprintControl.class) {
				if(instance == null){
					if(ctx == null){
						throw new IllegalStateException("can't use null context to init FingerprintControl");
					}
					instance = new FingerprintControl(ctx);
				}
			}
		}
		return instance;
	}
	
	private FingerprintControl(Context ctx){
		mMatcher = new FpMatcher(ctx.getApplicationContext());
	}
	
	public void showUI(Context mContext,CryptoObject crypt,FingerCallback callback) {
		if(Looper.myLooper() == Looper.getMainLooper()){
			throw new IllegalStateException("can't use with main looper");
		}
		if(crypt == null){
			throw new IllegalStateException("can't use without FingerprintManager.Crypto");
		}
		if (callback == null){
			throw new IllegalStateException("can't use without callback");
		}
		FpActivity.FpParameter parameter = FpActivity.FpParameter.Builder().setmCryptoObject(crypt);
		FpActivity.FpResult fpResult = mMatcher.showUI(parameter);
		FingerResult result = FingerResult.FAILED;
		String des = "authenticate error";
		if(fpResult.FPResult == FpMatcher.FP_RESULT.SUCCESS){
			result = FingerResult.SUCCESS;
			des = "authenticate success";
		}else if(fpResult.FPResult == FpMatcher.FP_RESULT.CANCEL){
			result = FingerResult.CANCEL;
			des = "authenticate cancel";
		}else {
			result = FingerResult.FAILED;
		}
		callback.onResult(result,des,fpResult.crypt);
	}
	
	public static interface FingerCallback{
		public void onResult(FingerResult result,String des,AuthenticationResult fingerResult);
	}
	
	public static enum FingerResult{
		SUCCESS,FAILED,CANCEL;
	}
	
}
