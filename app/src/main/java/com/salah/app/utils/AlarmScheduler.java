package com.salah.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.net.Uri;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.provider.Settings;
import android.net.Uri;
import android.util.Log;

import com.salah.app.models.Location;
import com.salah.app.models.PrayerTime;
import com.salah.app.models.UserSettings;
import com.salah.app.receivers.AthkarAlarmReceiver;
import com.salah.app.receivers.PrayerAlarmReceiver;
import com.salah.app.utils.FastingReminderScheduler;

import java.util.Calendar;
import java.util.TimeZone;

public class AlarmScheduler {
    private static final String TAG = "AlarmScheduler";
    public static void rescheduleAll(Context ctx) {
        // Android 12+ (API 31+) - التحقق من صلاحية الأذان الدقيقة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarm permission NOT granted - opening settings");
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + ctx.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    ctx.startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to open settings", e);
                }
                // نكمل الجدولة حتى بدون exact alarm permission
            }
        }
        
        // Android 13+ (API 33+) - التحقق من صلاحية الإشعارات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission NOT granted");
            }
        }

        cancelAll(ctx);
        UserSettings settings = PreferencesManager.load(ctx);
        Location loc = PreferencesManager.loadLocation(ctx);
        
        if (loc == null) {
            Log.w(TAG, "No saved location - skipping alarm scheduling");
            return;
        }

        // جدولة الأذان
        if (settings.adhanEnabled) {
            PrayerTime next = PrayerTimesCalculator.nextPrayer(loc, settings);
            if (next != null) schedulePrayer(ctx, next);
        }

        // جدولة أذكار الصباح الساعة 6:00 ص
        if (settings.morningAthkarEnabled) {
            scheduleAthkar(ctx, "morning", 6, 0);
        }

        // جدولة أذكار المساء الساعة 6:00 م (18:00)
        if (settings.eveningAthkarEnabled) {
            scheduleAthkar(ctx, "evening", 18, 0);
        }

        // جدولة أذكار النوم الساعة 9:00 م (21:00)
        scheduleAthkar(ctx, "sleep", 21, 0);
        
        FastingReminderScheduler.scheduleAll(ctx);
        FridaySalawatScheduler.scheduleAll(ctx);
        FridayJumuahScheduler.schedule(ctx);
        FridayKahfScheduler.schedule(ctx);
        Log.i(TAG, "All alarms rescheduled successfully");
    }
    public static final String ACTION_PRAYER = "com.salah.app.PRAYER_ALARM";
    public static final String ACTION_ATHKAR = "com.salah.app.ATHKAR_ALARM";
    public static final String EXTRA_PRAYER_ID = "prayer_id";
    public static final String EXTRA_PRAYER_NAME_AR = "prayer_name_ar";
    public static final String EXTRA_ATHKAR_TYPE = "athkar_type";


    public static void schedulePrayer(Context ctx, PrayerTime p) {
        Intent intent = new Intent(ctx, PrayerAlarmReceiver.class);
        intent.setAction(ACTION_PRAYER);
        intent.putExtra(EXTRA_PRAYER_ID, p.prayer.id);
        intent.putExtra(EXTRA_PRAYER_NAME_AR, p.getArabicName());
        
        PendingIntent pi = PendingIntent.getBroadcast(
            ctx, requestCodeForPrayer(p.prayer.id), intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        setExact(ctx, p.time.getTime(), pi);
        Log.i(TAG, "Scheduled " + p.prayer.id + " at " + p.time);
    }

    public static void scheduleAthkar(Context ctx, String type, int hour, int minute) {
        Calendar c = Calendar.getInstance(TimeZone.getDefault());
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent intent = new Intent(ctx, AthkarAlarmReceiver.class);
        intent.setAction(ACTION_ATHKAR);
        intent.putExtra(EXTRA_ATHKAR_TYPE, type);
        
        PendingIntent pi = PendingIntent.getBroadcast(
            ctx, requestCodeForAthkar(type), intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        setExact(ctx, c.getTimeInMillis(), pi);
        Log.i(TAG, "Scheduled " + type + " athkar at " + c.getTime());
    }

    public static void cancelAll(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        
        for (String id : new String[]{"fajr", "dhuhr", "asr", "maghrib", "isha"}) {
            Intent i = new Intent(ctx, PrayerAlarmReceiver.class).setAction(ACTION_PRAYER);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, requestCodeForPrayer(id), i,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) am.cancel(pi);
        }
        
        for (String t : new String[]{"morning", "evening", "sleep"}) {
            Intent i = new Intent(ctx, AthkarAlarmReceiver.class).setAction(ACTION_ATHKAR);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, requestCodeForAthkar(t), i,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) am.cancel(pi);
        }
    }

    private static void setExact(Context ctx, long triggerAtMillis, PendingIntent pi) {
        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.w(TAG, "Attempted to schedule alarm in the past, skipping");
            return;
        }
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to set exact alarm", e);
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
        }
    }

    private static int requestCodeForPrayer(String prayerId) {
        return 1000 + Math.abs(prayerId.hashCode() % 1000);
    }

    private static int requestCodeForAthkar(String type) {
        return 2000 + Math.abs(type.hashCode() % 1000);
    }
}
