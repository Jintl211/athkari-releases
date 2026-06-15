package com.salah.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.salah.app.receivers.DuhaAlarmReceiver;
import java.util.Calendar;

public class DuhaReminderScheduler {
    private static final int REQ_DUHA_1 = 6001; // 6:20 صباحاً
    private static final int REQ_DUHA_2 = 6002; // 11:00 ظهراً

    public static void scheduleAll(Context ctx) {
        schedule(ctx, "duha_1", 6, 20, REQ_DUHA_1);
        schedule(ctx, "duha_2", 11, 0, REQ_DUHA_2);
    }

    private static void schedule(Context ctx, String type, int hour, int min, int reqCode) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, min);
        c.set(Calendar.SECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        Intent i = new Intent(ctx, DuhaAlarmReceiver.class);
        i.setAction("com.salah.app.DUHA_REMINDER");
        i.putExtra("duha_type", type);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, reqCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
        }
    }

    public static void cancelAll(Context ctx) {
        for (int req : new int[]{REQ_DUHA_1, REQ_DUHA_2}) {
            Intent i = new Intent(ctx, DuhaAlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, req, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am != null) am.cancel(pi);
        }
    }
}
