package jp.joao.android.CallLogTrimmer;

import java.io.File;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class OnAlarmReceiver extends BroadcastReceiver {

	private static final String LOG_TAG = "OnAlarmReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {

		DebugLog.setDebugLogging(true, new File(Environment.getExternalStorageDirectory(), "CallLogTrimmer.txt"));

		DebugLog.f(LOG_TAG, "alarm." + new Date().toString());

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

		String value = sharedPreferences.getString(Common.PREFERENCE_TRIM_FREQUENCY, "0");

		int hours = 0;
		try {
			hours = Integer.parseInt(value);
		} catch (NumberFormatException e) {
		}

		if (hours != 0) {

			// schedule next

			WakefulIntentService.sendWakefulWork(context,CallLogTrimmerService.class);

			Intent i = new Intent(context, OnAlarmReceiver.class);
			PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

			AlarmUtils.scheduleInHours(context, pi, hours);

		} else {

			// cancel alarm

			AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Intent i = new Intent(context, OnAlarmReceiver.class);
			PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

			mgr.cancel(pi);
		}

	}
}