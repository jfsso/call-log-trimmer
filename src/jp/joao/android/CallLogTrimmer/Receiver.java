package jp.joao.android.CallLogTrimmer;

import java.io.File;
import java.util.Date;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

public class Receiver extends BroadcastReceiver {

	private static final String LOG_TAG = "Receiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		DebugLog.setDebugLogging(true, new File(Environment.getExternalStorageDirectory(), "CallLogTrimmer.txt"));

		DebugLog.f(LOG_TAG, "call." + new Date().toString());

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

		String value = sharedPreferences.getString(Common.PREFERENCE_TRIM_FREQUENCY, "0");

		int hours = 0;
		try {
			hours = Integer.parseInt(value);
		} catch (NumberFormatException e) {
		}

		if(hours != 0) { return; }

		String intentAction = intent.getAction();

		if (intentAction.equals(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
			String pstn_state = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_STATE);

			if (pstn_state.equals(android.telephony.TelephonyManager.EXTRA_STATE_IDLE))
			{
				WakefulIntentService.sendWakefulWork(context, CallLogTrimmerService.class);
			}

		}
	}

}
