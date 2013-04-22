package jp.joao.android.CallLogTrimmer;

import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;

public class AlarmUtils {

	private static final String LOG_TAG = "AlarmUtils";

	private AlarmUtils() {
	}

	public static int scheduleInHours( Context context , PendingIntent pi , int hours ) {

		int period = hours * 60 * 60 * 1000; // in millis
		//int period = hours * 60 * 1000; // in millis // TODO test in minutes

		AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		long alarmTime = System.currentTimeMillis() + period;

		DebugLog.f(LOG_TAG, "next alarm schedule to:" + new Date(alarmTime).toString());

		mgr.set(AlarmManager.RTC_WAKEUP, alarmTime, pi);

		return period;
	}

}
