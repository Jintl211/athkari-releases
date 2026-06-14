package com.salah.app;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.salah.app.services.PrayerMonitorService;
import com.salah.app.services.PrayerNotificationService;
import com.salah.app.utils.AlarmScheduler;
import com.salah.app.utils.DuaScheduler;
import com.salah.app.utils.HourlyDuaScheduler;
import com.salah.app.utils.NotificationHelper;
import com.salah.app.utils.PreferencesManager;
import com.salah.app.utils.PrayerApiClient;

public class SalahApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        com.salah.app.utils.CrashLogger.setup(this);

        try { NotificationHelper.createAllChannels(this); }
        catch (Exception e) { Log.e("SalahApp", "createAllChannels failed", e); }

        try { PreferencesManager.applyTheme(this); }
        catch (Exception e) { Log.e("SalahApp", "applyTheme failed", e); }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try { AlarmScheduler.rescheduleAll(this); }
            catch (Exception e) { Log.e("SalahApp", "rescheduleAll failed", e); }

            try { PrayerApiClient.fetchAllCities(this); }
            catch (Exception e) { Log.e("SalahApp", "fetchAllCities failed", e); }

            try { HourlyDuaScheduler.scheduleNext(this); }
            catch (Exception e) { Log.e("SalahApp", "HourlyDuaScheduler failed", e); }
        }, 3000);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // ✅ تشغيل خدمة الإشعار الثابت
            // PrayerMonitorService يتولى الإشعار الثابت

            try { startPrayerMonitorService(); }
            catch (Exception e) { Log.e("SalahApp", "PrayerMonitor failed", e); }
        }, 2000);

        try { DuaScheduler.start(this); }
        catch (Exception e) { Log.e("SalahApp", "DuaScheduler failed", e); }
    }

    // ✅ الإشعار الثابت مع العد التنازلي
    private void startPersistentNotificationService() {
        Intent intent = new Intent(this, PrayerNotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void startPrayerMonitorService() {
        Intent intent = new Intent(this, PrayerMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
}
