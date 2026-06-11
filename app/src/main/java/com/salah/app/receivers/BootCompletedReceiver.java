package com.salah.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.salah.app.utils.AlarmScheduler;

/**
 * Re-schedules all alarms after device reboot or app upgrade.
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
            || "android.intent.action.QUICKBOOT_POWERON".equals(action)
            || "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)
            || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
            || Intent.ACTION_TIME_CHANGED.equals(action)
            || Intent.ACTION_DATE_CHANGED.equals(action)
            || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            Log.i(TAG, "Re-scheduling alarms after " + action);
            AlarmScheduler.rescheduleAll(context);
            try {
                com.salah.app.utils.SleepAdhkarScheduler.schedule(context);
                com.salah.app.utils.FastingReminderScheduler.scheduleAll(context);
                com.salah.app.utils.HourlyDuaScheduler.scheduleNext(context);
                com.salah.app.utils.KahfReminderScheduler.schedule(context);
                com.salah.app.utils.FridaySalawatScheduler.scheduleAll(context);
                com.salah.app.utils.FridayJumuahScheduler.schedule(context);
                com.salah.app.utils.FridayKahfScheduler.schedule(context);
            } catch (Throwable ignored) {}
        }
    }
}
