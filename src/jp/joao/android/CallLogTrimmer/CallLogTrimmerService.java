package jp.joao.android.CallLogTrimmer;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.widget.RemoteViews;
import android.widget.Toast;

public class CallLogTrimmerService extends WakefulIntentService {

	private static final String LOG_TAG = "CallLogTrimmerService";

	public CallLogTrimmerService() {
		super("CallLogTrimmerService");
	}

	private NotificationManager notificationManager;
	private static final int NOTIFICATION_ID = 1;
	private Notification notification;
	private RemoteViews rv;

	private boolean showNotifications = false;
	private boolean callTrimmerEnabled = false;
	private int maximumCallsToKeep = 50;
	private boolean colapseByContacts = false;
	private int maxiumCallsToKeepPerContact = 5;

	private static Uri callsProvider = Calls.CONTENT_URI;

    private final int WAIT_INTERVAL = 6000;
    private final int MESSAGE_WHAT = 100;

    private Intent intent;

    /*
	private Handler handler = new Handler() {

		public void handleMessage(Message msg) {
			DebugLog.f(LOG_TAG, "handle." + new Date().toString());
			trimLog(intent);
		}

	};*/


	@Override
	protected void doWakefulWork(Intent intent) {
		DebugLog.setDebugLogging(true, new File(Environment.getExternalStorageDirectory(), "CallLogTrimmer.txt"));

		DebugLog.f(LOG_TAG, "wakefulwork." + new Date().toString());

		this.intent = intent;

        //Message message = new Message();
        //message.what = MESSAGE_WHAT;

        //handler.sendMessageDelayed(message, WAIT_INTERVAL);

		try {
			Thread.sleep(WAIT_INTERVAL);
		} catch (InterruptedException e) {
		}

		trimLog(intent);
	}

	private void trimLog(Intent intent) {
		DebugLog.f(LOG_TAG, "trimLog." + new Date().toString());

		try {

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

			showNotifications = prefs.getBoolean("show_notifications", false);
			callTrimmerEnabled = prefs.getBoolean("limit_by_count", false);
			String maximumCallsToKeepString = prefs.getString("maximum_count", "50");

			if("zero".equals(maximumCallsToKeepString)) {
				maximumCallsToKeep = 0;
			} else {
				try {
					maximumCallsToKeep = Integer.parseInt(maximumCallsToKeepString);
				} catch (NumberFormatException e) {
					callTrimmerEnabled = false;
				}
			}

			colapseByContacts = prefs.getBoolean("colapse_by_contacts", false);

			String maxiumCallToKeepPerContactString = prefs.getString(	"maxium_per_contact", "5");
			try {
				maxiumCallsToKeepPerContact = Integer.parseInt(maxiumCallToKeepPerContactString);
			} catch(NumberFormatException e) {
				colapseByContacts = false;
			}

			boolean showToast = false;

			HashSet<Long> itemsToDelete = new HashSet<Long>();

			if (colapseByContacts) {

				HashMap<String, Integer> counter = new HashMap<String, Integer>();

				ContentResolver contentResolver = getContentResolver();
				Cursor c = contentResolver.query(callsProvider, null, null, null, null);

				if (c.moveToLast()) {

					do {

						String number = c.getString(c.getColumnIndex(Calls.NUMBER));

						// count number of calls per contact
						if (counter.containsKey(number)) {
							Integer count = counter.get(number);
							counter.put(number, count.intValue() + 1);
						} else {
							counter.put(number, 1);
						}

						// set items to delete
						if (counter.get(number).intValue() > maxiumCallsToKeepPerContact) {
							itemsToDelete.add(c.getLong(c.getColumnIndex(Calls._ID)));
						}

					} while (c.moveToPrevious());

				}

				c.close();

			}

			if (callTrimmerEnabled) {

				ContentResolver contentResolver = getContentResolver();
				Cursor c = contentResolver.query(callsProvider, null, null, null, null);

				maximumCallsToKeep = c.getCount() - maximumCallsToKeep - itemsToDelete.size();

				if (c.moveToFirst()) {

					int i = 0;

					do {

						if (i >= maximumCallsToKeep)
							break;

						if (!itemsToDelete.contains(c.getLong(c
								.getColumnIndex(Calls._ID)))) {
							itemsToDelete.add(c.getLong(c
									.getColumnIndex(Calls._ID)));
						}

						i++;

					} while (c.moveToNext());

				}
				c.close();

			}

			// delete call log
			if (itemsToDelete.size() > 0) {

				// NotificationManager
				if( showNotifications ) {
					String ns = Context.NOTIFICATION_SERVICE;
					notificationManager = (NotificationManager) getSystemService(ns);
					notification = new Notification(R.drawable.icon,
							getString(R.string.notification_message), System
									.currentTimeMillis());

					rv = new RemoteViews(this.getPackageName(), R.layout.custom_notificationbar);
					rv.setImageViewResource(R.id.notifyImage, R.drawable.icon);
					rv.setTextViewText(R.id.notifyText, String.format(getString(R.string.notification_progress), 0, itemsToDelete.size()));
					rv.setProgressBar(R.id.customProgressBar, itemsToDelete.size(), 0, false);
					notification.contentView = rv;

					Intent notificationIntent = new Intent(this, Preferences.class);
					PendingIntent contentIntent = PendingIntent.getActivity(this,0, notificationIntent, 0);
					notification.contentIntent = contentIntent;

					notificationManager.notify(NOTIFICATION_ID, notification);
				}

				// delete call log
				ContentResolver contentResolver = getContentResolver();
				Cursor c = contentResolver.query(callsProvider, null, null, null, null);

				int i = 0;
				for (Long item : itemsToDelete) {
					i++;
					contentResolver.delete(callsProvider, "_id="
							+ item.longValue(), null);
					if (showNotifications) {
						rv.setProgressBar(R.id.customProgressBar, itemsToDelete.size(), i, false);
						rv.setTextViewText(R.id.notifyText, String.format(getString(R.string.notification_progress), i, itemsToDelete.size()));
						notificationManager.notify(NOTIFICATION_ID, notification);
					}
				}

				if (showNotifications) {
					notificationManager.cancel(NOTIFICATION_ID);
				}

				c.close();

				showToast = true;
			}

			if (showNotifications) {
				Toast.makeText(CallLogTrimmerService.this,
						getString(R.string.call_log_trimmed_toast),
						Toast.LENGTH_LONG).show();
			}

			DebugLog.f(LOG_TAG, "endtrimlog." + new Date().toString());
			//stopSelf();

		} catch (Exception e) {
			StringBuilder sb = new StringBuilder();
			StackTraceElement[] stacks = e.getStackTrace();
			for(int i = 0; i< stacks.length; i++) {
		        StackTraceElement stack = stacks[i];
		        sb.setLength(0);
		        sb.append(stack.getClassName()).append("#");//クラス名
		        sb.append(stack.getMethodName()).append(":");//メソッド名
		        sb.append(stack.getLineNumber());//行番号
			}

			if(notificationManager != null) {
				try {
					notificationManager.cancel(NOTIFICATION_ID);
				} catch (Exception e2) {
				}
			}

			DebugLog.f(LOG_TAG, "exception." + new Date().toString());
			DebugLog.f(LOG_TAG, sb.toString());
		}

	}

}
