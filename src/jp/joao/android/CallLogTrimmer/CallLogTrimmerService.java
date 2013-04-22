
package jp.joao.android.CallLogTrimmer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

public class CallLogTrimmerService extends WakefulIntentService {

    private static final String LOG_TAG = "CallLogTrimmerService";

    public static final void startService(Context context) {
        CallLogTrimmerService.sendWakefulWork(context,
                CallLogTrimmerService.class);
    }

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

    private final int WAIT_INTERVAL = 6000;

    @Override
    protected void doWakefulWork(Intent intent) {
        DebugLog.setDebugLogging(true,
                new File(Environment.getExternalStorageDirectory(),
                        "CallLogTrimmer.txt"));

        DebugLog.f(LOG_TAG, "wakefulwork." + new Date().toString());

        try {
            Thread.sleep(WAIT_INTERVAL);
        } catch (InterruptedException e) {
        }

        trimLog(intent);
    }

    private void trimLog(Intent intent) {
        DebugLog.f(LOG_TAG, "trimLog." + new Date().toString());

        try {
            ContentResolver contentResolver = getContentResolver();

            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(this);

            showNotifications = prefs.getBoolean(
                    Common.PREFERENCES_SHOW_NOTIFICATIONS, false);
            callTrimmerEnabled = prefs.getBoolean(
                    Common.PREFERENCES_ENABLE_LIMIT_BY_COUNT, false);
            String maximumCallsToKeepString = prefs.getString(
                    Common.PREFERENCES_MAXIMUM_COUNT, "50");

            if ("zero".equals(maximumCallsToKeepString)) {
                maximumCallsToKeep = 0;
            } else {
                try {
                    maximumCallsToKeep = Integer
                            .parseInt(maximumCallsToKeepString);
                } catch (NumberFormatException e) {
                    callTrimmerEnabled = false;
                }
            }

            colapseByContacts = prefs.getBoolean(
                    Common.PREFERENCES_ENABLE_COLAPSE_BY_CONTACTS, false);

            String maxiumCallToKeepPerContactString = prefs.getString(
                    "maxium_per_contact", "5");
            try {
                maxiumCallsToKeepPerContact = Integer
                        .parseInt(maxiumCallToKeepPerContactString);
            } catch (NumberFormatException e) {
                colapseByContacts = false;
            }

            boolean showToast = false;

            HashSet<Long> itemsToDelete = new HashSet<Long>();

            if (colapseByContacts) {

                HashMap<String, Integer> counter = new HashMap<String, Integer>();

                Cursor c = contentResolver.query(Calls.CONTENT_URI, null, null,
                        null, Calls.DEFAULT_SORT_ORDER);

                if (c.moveToFirst()) {
                    do {

                        long id = c.getLong(c.getColumnIndex(Calls._ID));
                        String number = c.getString(c
                                .getColumnIndex(Calls.NUMBER));

                        // count number of calls per contact
                        if (counter.containsKey(number)) {
                            Integer count = counter.get(number);
                            counter.put(number, count.intValue() + 1);
                        } else {
                            counter.put(number, 1);
                        }

                        // set items to delete
                        if (counter.get(number).intValue() > maxiumCallsToKeepPerContact) {
                            itemsToDelete.add(id);
                        }

                    } while (c.moveToNext());
                }

                c.close();

            }

            // delete call log
            if (itemsToDelete.size() > 0) {

                // NotificationManager
                if (showNotifications) {
                    String ns = Context.NOTIFICATION_SERVICE;
                    notificationManager = (NotificationManager) getSystemService(ns);
                    notification = new Notification(R.drawable.ic_launcher,
                            getString(R.string.notification_message),
                            System.currentTimeMillis());

                    rv = new RemoteViews(this.getPackageName(),
                            R.layout.custom_notificationbar);
                    rv.setImageViewResource(R.id.notifyImage, R.drawable.ic_launcher);
                    rv.setTextViewText(R.id.notifyText, String.format(
                            getString(R.string.notification_progress), 0,
                            itemsToDelete.size()));
                    rv.setProgressBar(R.id.customProgressBar,
                            itemsToDelete.size(), 0, false);
                    notification.contentView = rv;

                    Intent notificationIntent = new Intent(this,
                            Preferences.class);
                    PendingIntent contentIntent = PendingIntent.getActivity(
                            this, 0, notificationIntent, 0);
                    notification.contentIntent = contentIntent;

                    notificationManager.notify(NOTIFICATION_ID, notification);
                }

                // delete call log
                int i = 0;
                for (Long item : itemsToDelete) {
                    i++;
                    contentResolver.delete(Calls.CONTENT_URI, Calls._ID + "=?",
                            new String[] {
                                String.valueOf(item.longValue())
                            });
                    if (showNotifications) {
                        rv.setProgressBar(R.id.customProgressBar,
                                itemsToDelete.size(), i, false);
                        rv.setTextViewText(R.id.notifyText, String.format(
                                getString(R.string.notification_progress), i,
                                itemsToDelete.size()));
                        notificationManager.notify(NOTIFICATION_ID,
                                notification);
                    }
                }

                showToast = true;

                DebugLog.v(LOG_TAG, "no of calls deleted (collapse) "
                        + itemsToDelete.size());
            }

            if (callTrimmerEnabled) {
                int noOfCallsDeleted = contentResolver.delete(
                        Calls.CONTENT_URI, "_id IN "
                                + "(SELECT _id FROM calls ORDER BY "
                                + Calls.DEFAULT_SORT_ORDER
                                + " LIMIT -1 OFFSET " + maximumCallsToKeep
                                + ")", null);

                if (noOfCallsDeleted > 0) {
                    showToast = true;
                }

                DebugLog.v(LOG_TAG, "no of calls deleted " + noOfCallsDeleted);
            }

            if (showNotifications && showToast) {
                DebugLog.v(LOG_TAG, "show toast");
                
                Toast.makeText(getApplicationContext(),
                        R.string.call_log_trimmed_toast, Toast.LENGTH_LONG)
                        .show();
            }

            if (showNotifications) {
                if (notificationManager != null) {
                    notificationManager.cancel(NOTIFICATION_ID);
                }
            }

            DebugLog.f(LOG_TAG, "endtrimlog." + new Date().toString());

        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            StackTraceElement[] stacks = e.getStackTrace();
            for (int i = 0; i < stacks.length; i++) {
                StackTraceElement stack = stacks[i];
                sb.setLength(0);
                sb.append(stack.getClassName()).append("#");// クラス名
                sb.append(stack.getMethodName()).append(":");// メソッド名
                sb.append(stack.getLineNumber());// 行番号
            }

            if (notificationManager != null) {
                try {
                    notificationManager.cancel(NOTIFICATION_ID);
                } catch (Exception e2) {
                }
            }

            DebugLog.e(LOG_TAG, e.getMessage(), e);
        }

    }

}
