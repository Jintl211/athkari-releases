package com.salah.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.salah.app.receivers.DuaReminderReceiver;

import java.util.Calendar;

public class DuaScheduler {
    private static final int REQ_DUA = 6000;
    private static final long INTERVAL_TWO_HOURS = 2 * 60 * 60 * 1000; // ساعتين
    
    public static void scheduleNext(Context context) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MILLISECOND, (int)INTERVAL_TWO_HOURS);
        
        Intent i = new Intent(context, DuaReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, REQ_DUA, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
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
    
    public static void start(Context context) {
        // إلغاء القديم وبدء جديد
        cancel(context);
        scheduleNext(context);
    }
    
    public static void cancel(Context context) {
        Intent i = new Intent(context, DuaReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, REQ_DUA, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi);
    }
}
