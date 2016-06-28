package com.fido.fingerprinttest;

import java.io.UnsupportedEncodingException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.hardware.fingerprint.FingerprintManager.CryptoObject;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fido.fingerprint.FingerprintControl;
import com.fido.fingerprint.FingerprintControl.FingerResult;
import com.fido.fingerprint.FingerprintOperation;
import com.fido.utils.Logger;

public class FpActivity extends Activity {

	private static final String TAG = FpActivity.class.getSimpleName();

	public static final int MAX_DEFAULT = 5;
	public static final String REQUEST_COUNT = "request_count";
	public static final String REQUEST_MSNG = "request_msng";

	ImageView finger_dialog_image;
	TextView finger_dialog_hint;
	LinearLayout manage_ok_btn_box;

	boolean isManageStart;
	private int counter = 0;
	String title;
	int max_mismatch_times = MAX_DEFAULT;

	private Messenger msng;
	
	private CryptoObject mCryptoObject;
	private FingerprintOperation mOperation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fpt_dialog);

		isManageStart = false;
		mCryptoObject = FingerprintControl.getCrypt();
		
		counter = 0;
		Intent intent = getIntent();
		if (intent.getIntExtra(REQUEST_COUNT, -1) != -1) {
			max_mismatch_times = intent.getIntExtra(REQUEST_COUNT, -1);
		}
		if(intent.getParcelableExtra(REQUEST_MSNG)!=null){
			msng = intent.getParcelableExtra(REQUEST_MSNG);
		}
		finger_dialog_image = (ImageView) findViewById(R.id.finger_imageview);
		finger_dialog_hint = (TextView) findViewById(R.id.fpt_dialog_hint_text);
		manage_ok_btn_box = (LinearLayout) findViewById(R.id.manage_ok_button_box);

		if (max_mismatch_times == 0 || max_mismatch_times > 5) {
			max_mismatch_times = 5;
		}
		Logger.d(TAG, "title: " + title);
		Logger.d(TAG, "max_mismatch_times: " + max_mismatch_times);

		mOperation = new FingerprintOperation(this);
		if (!mOperation.hasEnrolledFingerprints()) {
			Logger.d(TAG, "No Enrolled Fingerprints");
			setManageUI();
		} else {
			setVerifyUI();
			startIdentify();
		}

	}

	public void setManageUI() {

		Logger.d(TAG, "setManageUI called");

		manage_ok_btn_box.setVisibility(View.VISIBLE);

		finger_dialog_image.setImageResource(R.drawable.finger_wrong);
		finger_dialog_hint.setTextColor(Color.RED);
		finger_dialog_hint.setText(R.string.manage_hint);
	}

	public void setVerifyUI() {
		TextView title_view = (TextView) findViewById(R.id.fpt_dialog_title);

		manage_ok_btn_box.setVisibility(View.GONE);

		finger_dialog_image.setImageResource(R.drawable.finger);
		finger_dialog_hint.setTextColor(Color.parseColor("#666666"));
		finger_dialog_hint.setText(R.string.touch_finger_hint);

		if (title != null && title.length() > 0) {

			try {

				byte[] title_object = Base64.decode(title, 11);
				String title_text = new String(title_object, "UTF-8");
				title_view.setText(title_text);

				if (title_text != null && title_text.length() > 0) {
					title_view.setVisibility(View.VISIBLE);
				}

			} catch (UnsupportedEncodingException e) {
				Logger.e(TAG, "UnsupportedEncodingException: " + e);
			}

		}

	}

	public void startIdentify() {
		if (mCryptoObject != null) {
			mOperation.startListening(mCryptoObject,new AuthenticationCallback() {
				@Override
				public void onAuthenticationError(int errorCode,
						CharSequence errString) {
					Logger.d(TAG, "onAuthenticationError errorCode:"
							+ errorCode + " errString:" + errString);

					if (errorCode == 3) {
						updateTimeOutUI();
					} else if (errorCode == 7) {
						updateMismatchUI(true);
					} else if (errorCode != 5) {
						updateErrorUI();
					}
				}
				@Override
				public void onAuthenticationHelp(int helpCode,
						CharSequence helpString) {
					Logger.d(TAG, "onAuthenticationHelp helpCode:" + helpCode
							+ " helpString:" + helpString);
				}

				@Override
				public void onAuthenticationFailed() {
					Logger.d(TAG, "onAuthenticationFailed");
					counter++;
					Logger.d(TAG, "counter: " + counter);

					if (counter < 5) {
						if (counter >= max_mismatch_times) {
							updateMismatchUI(true);
						} else {
							updateMismatchUI(false);
						}

					}

				}

				public void onAuthenticationSucceeded(
						AuthenticationResult result) {
					Logger.d(TAG, "onAuthenticationSuccessed");
					if (result != null ) {
						updateSuccessUI(result);
					} else {
						Logger.e(TAG, "FingerprintId is not available!");
						updateErrorUI();
					}
				}
			});
		} else {
			Logger.e(TAG, "CryptoOject is null or opID is 0!");
			updateErrorUI();
		}
	}

	private void updateSuccessUI(AuthenticationResult crpt) {
		finger_dialog_image.setImageResource(R.drawable.finger_right);
		finger_dialog_hint.setTextColor(Color.rgb(23, 209, 38));
		finger_dialog_hint.setText(R.string.fingerprint_matched);
		
		//success callback
		triggerCallback(FingerResult.SUCCESS,crpt);
	}

	private void updateMismatchUI(boolean lockout) {
		// counter++;
		finger_dialog_image.setImageResource(R.drawable.finger_wrong);
		finger_dialog_hint.setTextColor(Color.RED);

		if (lockout) {
			finger_dialog_hint.setText(R.string.toomanyattempts);
			Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
			finger_dialog_hint.startAnimation(shake);
			new Thread() {
				public void run() {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					if (counter >= max_mismatch_times) {
						mOperation.stopListening();
					} else {
						Logger.i(TAG, "UNUSED!!!Mismatch " + max_mismatch_times
								+ " times! UI closed.");
					}

				}
			}.start();
		} else {
			finger_dialog_hint.setText(R.string.fingerprint_mismatched);
			Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);// 鍔犺浇鍔ㄧ敾璧勬簮鏂囦欢
			finger_dialog_hint.startAnimation(shake); // 缁欑粍浠舵挱鏀惧姩鐢绘晥鏋�
		}

	}

	private void updateTimeOutUI() {
		finger_dialog_image.setImageResource(R.drawable.finger_wrong);
		finger_dialog_hint.setTextColor(Color.RED);
		finger_dialog_hint.setText(R.string.timeout);

		//update time out
		triggerCallback(FingerResult.FAILED,null);
	}

	private void updateErrorUI() {
		finger_dialog_image.setImageResource(R.drawable.finger_wrong);
		finger_dialog_hint.setTextColor(Color.RED);
		finger_dialog_hint.setText(R.string.unknown_error);
		//update ERROR
		triggerCallback(FingerResult.FAILED,null);
	}

	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.fpt_dialog_bottom_cancel) {
			isManageStart = false;
			//cancel
			triggerCallback(FingerResult.FAILED,null);
		} else if (id == R.id.fpt_dialog_bottom_ok_manage) {
			Logger.i(TAG, "OK to redirect to Settings.");
			isManageStart = true;
			Logger.d(TAG, "Go to FingerprintSetting");
			
			//jump to setting
		}

	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		//cancel
		triggerCallback(FingerResult.CANCEL,null);
	}

	@Override
	public void onPause() {
		super.onPause();
		Logger.i(TAG, "onPause called");
		if (!isManageStart) {
			//cancel
			triggerCallback(FingerResult.CANCEL,null);
		}
	}

	protected void onResume() {
		super.onResume();
		Logger.d(TAG, "onResume()");

		if (isManageStart) {
			isManageStart = false;
			if (!mOperation.hasEnrolledFingerprints()) {
				Logger.d(TAG, "Still No Enrolled Fingerprints");
				//cancel
				triggerCallback(FingerResult.CANCEL,null);
			} else {
				setVerifyUI();
				startIdentify();
			}
		}
	}
	
	private void triggerCallback(FingerResult result,AuthenticationResult crypt){
		Logger.d(TAG, "triggerCallback");
		Message msg = Message.obtain();
		msg.arg1 = result.ordinal();
		msg.obj = crypt;
		if(msng!=null){
			try {
				msng.send(msg);
				msng = null;
				this.finish();
			} catch (RemoteException e) {
				Log.d(TAG, "triggerCallback ERROR");
				e.printStackTrace();
			}
		}else {
			Log.d(TAG, "triggerCallback ERROR");
		}
	}
}