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

public class PrayerAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "PrayerAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String prayerId = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER_ID);
        String prayerAr = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER_NAME_AR);
        Log.i(TAG, "PrayerAlarm fired: " + prayerId);

        String muezzin = PreferencesManager.load(context).selectedAdhanFile;
        if (muezzin == null || muezzin.isEmpty()) muezzin = "adhan_madinah";
        if (muezzin.startsWith("adhan_")) muezzin = muezzin.substring(6);

        // تشغيل AdhanService للصوت والإشعار
        Intent svc = new Intent(context, AdhanService.class);
        svc.putExtra(AlarmScheduler.EXTRA_PRAYER_ID, prayerId);
        svc.putExtra(AlarmScheduler.EXTRA_PRAYER_NAME_AR, prayerAr);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc);
        } else {
            context.startService(svc);
        }

        // تشغيل SyncedAdhanActivity للصورة والكلمات
        try {
            Intent sync = new Intent(context, SyncedAdhanActivity.class);
            sync.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            sync.putExtra(SyncedAdhanActivity.EXTRA_PRAYER, prayerId);
            sync.putExtra(SyncedAdhanActivity.EXTRA_MUEZZIN, muezzin);
            context.startActivity(sync);
        } catch (Throwable e) {
            Log.e(TAG, "Could not start SyncedAdhanActivity: " + e.getMessage());
        }

        // جدولة الصلاة التالية
        try {
            UserSettings settings = PreferencesManager.load(context);
            Location loc = PreferencesManager.loadLocation(context);
            if (loc != null && settings.adhanEnabled) {
                PrayerTime next = PrayerTimesCalculator.nextPrayer(loc, settings);
                if (next != null) AlarmScheduler.schedulePrayer(context, next);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to schedule next prayer", t);
        }
    }
}
