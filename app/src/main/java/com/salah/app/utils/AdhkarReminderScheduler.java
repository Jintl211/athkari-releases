package com.salah.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.salah.app.receivers.AthkarAlarmReceiver;
import com.salah.app.models.UserSettings;

import java.util.Calendar;

public class AdhkarReminderScheduler {
    private static final int REQ_MORNING = 5001;
    private static final int REQ_EVENING = 5002;
    private static final int REQ_SLEEP = 5003;

    public static void scheduleAll(Context ctx) {
        UserSettings s = PreferencesManager.load(ctx);
        
        if (s.morningAthkarEnabled) {
            schedule(ctx, "morning", 6, 0, REQ_MORNING);
        }
        if (s.eveningAthkarEnabled) {
            schedule(ctx, "evening", 18, 0, REQ_EVENING);
        }
        if (s.sleepAthkarEnabled) {
            schedule(ctx, "sleep", 21, 0, REQ_SLEEP);
        }
    }

    private static void schedule(Context ctx, String type, int hour, int min, int reqCode) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, min);
        c.set(Calendar.SECOND, 0);
        
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent i = new Intent(ctx, AthkarAlarmReceiver.class);
        i.setAction(AlarmScheduler.ACTION_ATHKAR);
        i.putExtra(AlarmScheduler.EXTRA_ATHKAR_TYPE, type);
        
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
        cancel(ctx, REQ_MORNING);
        cancel(ctx, REQ_EVENING);
        cancel(ctx, REQ_SLEEP);
    }
    
    private static void cancel(Context ctx, int reqCode) {
        Intent i = new Intent(ctx, AthkarAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, reqCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi);
    }
}
