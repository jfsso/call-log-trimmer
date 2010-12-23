package jp.joao.android.CallLogTrimmer;

import java.io.File;
import java.util.Date;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

public class OnBootReceiver extends BroadcastReceiver {

	private static final String LOG_TAG = "OnBootReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {

		DebugLog.setDebugLogging(true, new File(Environment.getExternalStorageDirectory(), "CallLogTrimmer.txt"));

		DebugLog.f(LOG_TAG, "boot." + new Date().toString());

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

		String value = sharedPreferences.getString(Common.PREFERENCE_TRIM_FREQUENCY, "0");

		int hours = 0;
		try {
			hours = Integer.parseInt(value);
		} catch (NumberFormatException e) {
		}

		if(hours == 0) { return; }

		Intent i = new Intent(context, OnAlarmReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

		AlarmUtils.scheduleInHours(context, pi, hours);

	}
}