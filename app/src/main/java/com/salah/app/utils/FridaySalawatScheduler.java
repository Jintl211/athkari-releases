package com.salah.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import com.salah.app.receivers.FridaySalawatReceiver;
import java.util.Calendar;

public class FridaySalawatScheduler {
    private static final String TAG = "FridaySalawat";
    private static final int[][] TIMES = {{7, 0}, {14, 0}, {16, 0}};

    public static void scheduleAll(Context ctx) {
        for (int i = 0; i < TIMES.length; i++) {
            scheduleOne(ctx, TIMES[i][0], TIMES[i][1], i);
        }
        Log.i(TAG, "Friday salawat scheduled x3");
    }

    public static void reschedule(Context ctx, int index) {
        if (index < TIMES.length) {
            scheduleOne(ctx, TIMES[index][0], TIMES[index][1], index);
        }
    }

    private static void scheduleOne(Context ctx, int hour, int minute, int index) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(Calendar.WEEK_OF_YEAR, 1);
        }

        Intent intent = new Intent(ctx, FridaySalawatReceiver.class);
        intent.putExtra("hadith_index", index);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 600 + index, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
            }
            Log.i(TAG, "Scheduled index=" + index + " at " + c.getTime());
        } catch (Exception e) {
            Log.e(TAG, "Failed", e);
        }
    }

    public static void cancelAll(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        for (int i = 0; i < TIMES.length; i++) {
            Intent intent = new Intent(ctx, FridaySalawatReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, 600 + i, intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) am.cancel(pi);
        }
    }
}
