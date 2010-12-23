package jp.joao.android.CallLogTrimmer;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DebugLog.setDebugLogging(true, new File(Environment.getExternalStorageDirectory(), "CallLogTrimmer.txt"));

        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    protected void onStart() {
    	super.onStart();
    	getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {


		if(key.equals(Common.PREFERENCE_TRIM_FREQUENCY)) {
			String value = sharedPreferences.getString(Common.PREFERENCE_TRIM_FREQUENCY, "0");

			int hours = 0;
			try {
				hours = Integer.parseInt(value);
			} catch (NumberFormatException e) {
			}

			if(hours > 0) {
				// set alarm

				Intent i = new Intent(this, OnAlarmReceiver.class);
				PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);

				int period = AlarmUtils.scheduleInHours(this, pi, hours);

				Date date = new Date(new Date().getTime() + period);
				DateFormat df = DateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM);
				Toast.makeText(Preferences.this, getString(R.string.toast_trim_frequency, df.format(date)), Toast.LENGTH_LONG).show();

			} else {

				// cancel alarm
				AlarmManager mgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
				Intent i = new Intent(this, OnAlarmReceiver.class);
				PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);

				mgr.cancel(pi);
			}

		}

	}

}