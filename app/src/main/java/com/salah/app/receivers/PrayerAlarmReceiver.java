package com.salah.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.salah.app.activities.SyncedAdhanActivity;
import com.salah.app.services.AdhanService;
import com.salah.app.utils.AlarmScheduler;
import com.salah.app.utils.PreferencesManager;
import com.salah.app.utils.PrayerTimesCalculator;
import com.salah.app.models.Location;
import com.salah.app.models.PrayerTime;
import com.salah.app.models.UserSettings;

import java.util.List;

public class PrayerAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "PrayerAlarmReceiver";

    // ✅ نافذة قبول الأذان: 3 دقائق فقط
    private static final long TOLERANCE_MS = 3 * 60 * 1000L;

    @Override
    public void onReceive(Context context, Intent intent) {
        String prayerId = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER_ID);
        String prayerAr = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER_NAME_AR);
        Log.i(TAG, "PrayerAlarm fired: " + prayerId);

        UserSettings settings = PreferencesManager.load(context);
        Location loc = PreferencesManager.loadLocation(context);

        // ✅ التحقق أن وقت الصلاة لم يمر أكثر من 3 دقائق
        if (!isWithinTolerance(loc, settings, prayerId)) {
            Log.w(TAG, "Prayer alarm fired too late or wrong time for: " + prayerId + " - skipping");
            // إعادة جدولة كل الصلوات
            if (loc != null && settings.adhanEnabled) {
                AlarmScheduler.scheduleAllPrayers(context, loc, settings);
            }
            return;
        }

        // تحديد صوت الأذان
        String adhanFile;
        if ("fajr".equalsIgnoreCase(prayerId)) {
            adhanFile = settings.selectedFajrAdhanFile;
            if (adhanFile == null || adhanFile.isEmpty()) adhanFile = "adhan_madinah";
        } else {
            adhanFile = settings.selectedAdhanFile;
            if (adhanFile == null || adhanFile.isEmpty()) adhanFile = "adhan_madinah";
        }
        String muezzin = adhanFile.startsWith("adhan_") ? adhanFile.substring(6) : adhanFile;

        // ✅ تشغيل AdhanService فقط (الصوت من هنا)
        Intent svc = new Intent(context, AdhanService.class);
        svc.putExtra(AlarmScheduler.EXTRA_PRAYER_ID, prayerId);
        svc.putExtra(AlarmScheduler.EXTRA_PRAYER_NAME_AR, prayerAr);
        svc.putExtra("adhan_file", adhanFile);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc);
        } else {
            context.startService(svc);
        }

        // ✅ تشغيل SyncedAdhanActivity للكلمات فقط (بدون صوت)
        try {
            Intent sync = new Intent(context, SyncedAdhanActivity.class);
            sync.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            sync.putExtra(SyncedAdhanActivity.EXTRA_PRAYER, prayerId);
            sync.putExtra(SyncedAdhanActivity.EXTRA_MUEZZIN, muezzin);
            context.startActivity(sync);
        } catch (Throwable e) {
            Log.e(TAG, "Could not start SyncedAdhanActivity: " + e.getMessage());
        }

        // ✅ إعادة جدولة كل الصلوات
        try {
            if (loc != null && settings.adhanEnabled) {
                AlarmScheduler.scheduleAllPrayers(context, loc, settings);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to reschedule prayers", t);
        }
    }

    /**
     * ✅ التحقق أن وقت الصلاة لم يمر أكثر من TOLERANCE_MS
     */
    private boolean isWithinTolerance(Location loc, UserSettings settings, String prayerId) {
        if (loc == null || prayerId == null) return false;
        try {
            long now = System.currentTimeMillis();
            List<PrayerTime> todayPrayers = PrayerTimesCalculator.getTodayTimes(loc, settings);
            for (PrayerTime p : todayPrayers) {
                if (p.prayer.id.equals(prayerId)) {
                    long diff = now - p.time.getTime();
                    // الأذان مقبول إذا كان في نطاق 0 إلى 3 دقائق
                    boolean ok = diff >= -5000 && diff <= TOLERANCE_MS;
                    Log.i(TAG, "Prayer " + prayerId + " time diff: " + diff + "ms, ok=" + ok);
                    return ok;
                }
            }
            // تحقق من صلوات الغد (للفجر)
            List<PrayerTime> tomorrowPrayers = PrayerTimesCalculator.getTomorrowTimes(loc, settings);
            for (PrayerTime p : tomorrowPrayers) {
                if (p.prayer.id.equals(prayerId)) {
                    long diff = now - p.time.getTime();
                    boolean ok = diff >= -5000 && diff <= TOLERANCE_MS;
                    Log.i(TAG, "Tomorrow prayer " + prayerId + " time diff: " + diff + "ms, ok=" + ok);
                    return ok;
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "isWithinTolerance error", t);
        }
        return false;
    }
}
