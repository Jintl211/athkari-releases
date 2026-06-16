package com.salah.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.net.Uri;
import android.util.Log;

import com.salah.app.models.Location;
import com.salah.app.models.PrayerTime;
import com.salah.app.models.UserSettings;
import com.salah.app.receivers.AthkarAlarmReceiver;
import com.salah.app.receivers.PrayerAlarmReceiver;
import com.salah.app.receivers.PrePrayerReceiver;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class AlarmScheduler {
    private static final String TAG = "AlarmScheduler";

    public static final String ACTION_PRAYER      = "com.salah.app.PRAYER_ALARM";
    public static final String ACTION_ATHKAR      = "com.salah.app.ATHKAR_ALARM";
    public static final String EXTRA_PRAYER_ID    = "prayer_id";
    public static final String EXTRA_PRAYER_NAME_AR = "prayer_name_ar";
    public static final String EXTRA_ATHKAR_TYPE  = "athkar_type";

    public static void rescheduleAll(Context ctx) {
        // Android 12+ - التحقق من صلاحية الأذان الدقيقة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarm permission NOT granted");
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + ctx.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { ctx.startActivity(intent); } catch (Exception e) { Log.e(TAG, "Failed to open settings", e); }
            }
        }

        cancelAll(ctx);

        UserSettings settings = PreferencesManager.load(ctx);
        Location loc = PreferencesManager.loadLocation(ctx);

        if (loc == null) {
            Log.w(TAG, "No saved location - skipping alarm scheduling");
            return;
        }

        // ✅ جدولة كل صلوات اليوم والغد (مش صلاة وحدة)
        if (settings.adhanEnabled) {
            scheduleAllPrayers(ctx, loc, settings);
        }

        // ✅ أذكار الصباح 6:00 ص
        if (settings.morningAthkarEnabled) {
            scheduleAthkar(ctx, "morning", 5, 30);
        }

        // ✅ أذكار المساء 6:00 م
        if (settings.eveningAthkarEnabled) {
            scheduleAthkar(ctx, "evening", 17, 30);
        }

        // ✅ أذكار النوم 10:00 م
        scheduleAthkar(ctx, "sleep", 22, 0);

        FastingReminderScheduler.scheduleAll(ctx);
        FridaySalawatScheduler.scheduleAll(ctx);
        FridayJumuahScheduler.schedule(ctx);
        FridayKahfScheduler.schedule(ctx);

        Log.i(TAG, "All alarms rescheduled successfully");
    }

    /** ✅ جدولة كل الصلوات الخمس لليوم والغد */
    public static void scheduleAllPrayers(Context ctx, Location loc, UserSettings settings) {
        long now = System.currentTimeMillis();

        // صلوات اليوم
        List<PrayerTime> todayPrayers = PrayerTimesCalculator.getTodayTimes(loc, settings);
        for (PrayerTime p : todayPrayers) {
            if (p.prayer == PrayerTime.Prayer.SUNRISE) continue;
            if (p.time.getTime() > now) {
                schedulePrayer(ctx, p);
            }
        }

        // صلوات الغد (لضمان الفجر دائماً مجدول)
        List<PrayerTime> tomorrowPrayers = PrayerTimesCalculator.getTomorrowTimes(loc, settings);
        for (PrayerTime p : tomorrowPrayers) {
            if (p.prayer == PrayerTime.Prayer.SUNRISE) continue;
            // جدول غد فقط لو ما جدلنا نفس الصلاة اليوم
            boolean scheduledToday = false;
            for (PrayerTime tp : todayPrayers) {
                if (tp.prayer == p.prayer && tp.time.getTime() > now) {
                    scheduledToday = true;
                    break;
                }
            }
            if (!scheduledToday) {
                schedulePrayer(ctx, p);
            }
        }
    }

    public static void schedulePrayer(Context ctx, PrayerTime p) {
        if (p.time.getTime() <= System.currentTimeMillis()) return;

        // جدولة تنبيه قبل الصلاة بـ 10 دقائق
        long prePrayerTime = p.time.getTime() - (10 * 60 * 1000);
        if (prePrayerTime > System.currentTimeMillis()) {
            Intent preIntent = new Intent(ctx, PrePrayerReceiver.class);
            preIntent.setAction("com.salah.app.PRE_PRAYER");
            preIntent.putExtra(PrePrayerReceiver.EXTRA_PRAYER_AR, p.getArabicName());
            preIntent.putExtra(PrePrayerReceiver.EXTRA_MINUTES, 10);
            PendingIntent prePi = PendingIntent.getBroadcast(
                ctx, requestCodeForPrayer(p.prayer.id) + 100, preIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            setExact(ctx, prePrayerTime, prePi);
        }

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
        if (am == null) return;

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

    public static int requestCodeForPrayer(String prayerId) {
        switch (prayerId) {
            case "fajr":    return 1001;
            case "dhuhr":   return 1002;
            case "asr":     return 1003;
            case "maghrib": return 1004;
            case "isha":    return 1005;
            default:        return 1000;
        }
    }

    public static int requestCodeForAthkar(String type) {
        switch (type) {
            case "morning": return 2001;
            case "evening": return 2002;
            case "sleep":   return 2003;
            default:        return 2000;
        }
    }
}
