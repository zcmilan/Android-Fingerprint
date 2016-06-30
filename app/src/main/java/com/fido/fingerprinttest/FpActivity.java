package com.fido.fingerprinttest;

import java.io.UnsupportedEncodingException;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.hardware.fingerprint.FingerprintManager.CryptoObject;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fido.fingerprint.FingerprintOperation;
import com.fido.fingerprint.FpMatcher;
import com.fido.utils.Logger;

@SuppressLint("NewApi")
public class FpActivity extends Activity {
	private static final String TAG = FpActivity.class.getSimpleName();

	public static final int MAX_DEFAULT = 5;
	public static final String REQUEST_AAID = "request_aaid";
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

	private static CryptoObject mCryptoObject;
	private FingerprintOperation mOperation;

	// three parameters
	// @param context
	// @param transaction text
	// @param uiParameters
	// @param Messenger
	public static void showUI(Context mContext, FpParameter parameter, Messenger msgner) {
		if (parameter != null) {
			mCryptoObject = parameter.getmCryptoObject();

			Intent intent = new Intent(mContext, FpActivity.class);
			// TODO transaction
			// intent.putExtra("isTransaction", isTransaction);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra(REQUEST_AAID, parameter.getAAID());
			intent.putExtra(REQUEST_MSNG, msgner);
			mContext.startActivity(intent);
		} else {
			Logger.e(TAG, "showUI error , paramter is null");
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
		if (!keyguardManager.isKeyguardSecure()) {
			Toast.makeText(this, "Lock screen security not enabled in Settings", Toast.LENGTH_LONG).show();
			triggerCallback(FpMatcher.FP_RESULT.ERRORAUTH, null);
			return;
		}
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
			Toast.makeText(this, "Fingerprint authentication permission not enabled", Toast.LENGTH_LONG).show();
			triggerCallback(FpMatcher.FP_RESULT.ERRORAUTH, null);
			return;
		}

		setContentView(R.layout.fpt_dialog);

		isManageStart = false;
		counter = 0;
		Intent intent = getIntent();
		if (intent.getIntExtra(REQUEST_COUNT, -1) != -1) {
			max_mismatch_times = intent.getIntExtra(REQUEST_COUNT, -1);
		}
		if (intent.getParcelableExtra(REQUEST_MSNG) != null) {
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
			Logger.d(TAG+"crypto in", "startIdentify:"+mCryptoObject);
			mOperation.startListening(mCryptoObject, new AuthenticationCallback() {
				@Override
				public void onAuthenticationError(int errorCode, CharSequence errString) {
					Logger.d(TAG, "onAuthenticationError errorCode:" + errorCode + " errString:" + errString);

					if (errorCode == 3) {
						updateTimeOutUI();
					} else if (errorCode == 7) {
						updateMismatchUI(true);
					} else if (errorCode != 5) {
						updateErrorUI();
					}
				}

				@Override
				public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
					Logger.d(TAG, "onAuthenticationHelp helpCode:" + helpCode + " helpString:" + helpString);
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

				public void onAuthenticationSucceeded(AuthenticationResult result) {
					Logger.d(TAG, "onAuthenticationSuccessed");
					if (result != null) {
						Logger.d(TAG+"crypto out", "onAuthenticationSucceeded:"+result.getCryptoObject());
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
		// success callback
		triggerCallback(FpMatcher.FP_RESULT.SUCCESS, crpt);
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
						Logger.i(TAG, "UNUSED!!!Mismatch " + max_mismatch_times + " times! UI closed.");
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

		// update time out
		triggerCallback(FpMatcher.FP_RESULT.TIMEOUT, null);
	}

	private void updateErrorUI() {
		finger_dialog_image.setImageResource(R.drawable.finger_wrong);
		finger_dialog_hint.setTextColor(Color.RED);
		finger_dialog_hint.setText(R.string.unknown_error);
		// update ERROR
		triggerCallback(FpMatcher.FP_RESULT.ERRORAUTH, null);
	}

	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.fpt_dialog_bottom_cancel) {
			isManageStart = false;
			// cancel
			triggerCallback(FpMatcher.FP_RESULT.CANCEL, null);
		} else if (id == R.id.fpt_dialog_bottom_ok_manage) {
			Logger.i(TAG, "OK to redirect to Settings.");
			isManageStart = true;
			Logger.d(TAG, "Go to FingerprintSetting");

			// jump to setting
		}

	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		// cancel
		triggerCallback(FpMatcher.FP_RESULT.CANCEL, null);
	}

	@Override
	public void onPause() {
		super.onPause();
		Logger.i(TAG, "onPause called");
		if (!isManageStart) {
			// cancel
			triggerCallback(FpMatcher.FP_RESULT.CANCEL, null);
		}
	}

	protected void onResume() {
		super.onResume();
		Logger.d(TAG, "onResume()");

		if (isManageStart) {
			isManageStart = false;
			if (!mOperation.hasEnrolledFingerprints()) {
				Logger.d(TAG, "Still No Enrolled Fingerprints");
				// cancel
				triggerCallback(FpMatcher.FP_RESULT.CANCEL, null);
			} else {
				setVerifyUI();
				startIdentify();
			}
		}
	}

	private void triggerCallback(FpMatcher.FP_RESULT FPResult, AuthenticationResult crypt) {
		Logger.d(TAG, "triggerCallback");
		Message msg = Message.obtain();
		msg.arg1 = FPResult.ordinal();
		msg.obj = new FpResult().setFPResult(FPResult).setCrypt(crypt);
		if (msng != null) {
			try {
				msng.send(msg);
				msng = null;
				this.finish();
			} catch (RemoteException e) {
				Log.d(TAG, "triggerCallback ERROR,e:" + e.getMessage());
				e.printStackTrace();
			}
		} else {
			Log.d(TAG, "triggerCallback ERROR");
		}
	}

	public static class FpResult {
		public AuthenticationResult crypt;
		public FpMatcher.FP_RESULT FPResult;

		public FpResult setCrypt(AuthenticationResult crypt) {
			this.crypt = crypt;
			return this;
		}

		public FpResult setFPResult(FpMatcher.FP_RESULT FPResult) {
			this.FPResult = FPResult;
			return this;
		}
	}

	public static class FpParameter {
		// String transaction , String uiParameters, Messenger messenger, String
		// AAID , CryptoObject cryptoObject

		private boolean isTransaction;
		private String transaction;
		private String title;
		private String hint;
		private String AAID;
		private CryptoObject mCryptoObject;

		private FpParameter() {
		}

		public static FpParameter Builder() {
			return new FpParameter();
		}

		public boolean isTransaction() {
			return isTransaction;
		}

		public FpParameter setTransaction(boolean isTransaction) {
			this.isTransaction = isTransaction;
			return this;
		}

		public String getTransaction() {
			return transaction;
		}

		public FpParameter setTransaction(String transaction) {
			this.transaction = transaction;
			return this;
		}

		public String getTitle() {
			return title;
		}

		public FpParameter setTitle(String title) {
			this.title = title;
			return this;
		}

		public String getHint() {
			return hint;
		}

		public FpParameter setHint(String hint) {
			this.hint = hint;
			return this;
		}

		public String getAAID() {
			return AAID;
		}

		public FpParameter setAAID(String aAID) {
			AAID = aAID;
			return this;
		}

		public CryptoObject getmCryptoObject() {
			return mCryptoObject;
		}

		public FpParameter setmCryptoObject(CryptoObject mCryptoObject) {
			this.mCryptoObject = mCryptoObject;
			return this;
		}

	}
}