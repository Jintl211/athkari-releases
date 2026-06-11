package com.salah.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import com.salah.app.receivers.FridayKahfReceiver;
import java.util.Calendar;

public class FridayKahfScheduler {
    private static final String TAG = "FridayKahfScheduler";
    private static final int[][] TIMES = {{13, 0}, {17, 30}};
    private static final int[] IDS = {600, 601};

    public static void schedule(Context ctx) {
        for (int i = 0; i < TIMES.length; i++) {
            scheduleOne(ctx, TIMES[i][0], TIMES[i][1], IDS[i]);
        }
        Log.i(TAG, "Kahf reminders scheduled x2");
    }

    private static void scheduleOne(Context ctx, int hour, int minute, int id) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(Calendar.WEEK_OF_YEAR, 1);
        }
        Intent intent = new Intent(ctx, FridayKahfReceiver.class);
        intent.putExtra("notif_id", id);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, id, intent,
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
            Log.i(TAG, "Scheduled Kahf id=" + id + " at " + c.getTime());
        } catch (Exception e) {
            Log.e(TAG, "Failed", e);
        }
    }

    public static void cancel(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        for (int id : IDS) {
            Intent intent = new Intent(ctx, FridayKahfReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, id, intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) am.cancel(pi);
        }
    }
}
