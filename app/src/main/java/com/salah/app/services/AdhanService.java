package com.salah.app.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import com.salah.app.R;
import com.salah.app.utils.AlarmScheduler;
import com.salah.app.utils.NotificationHelper;
import com.salah.app.utils.PreferencesManager;
import com.salah.app.utils.WakeLockManager;
import com.salah.app.models.UserSettings;

public class AdhanService extends Service {
    private static final String TAG = "AdhanService";
    public static final String ACTION_STOP = "com.salah.app.STOP_ADHAN";

    private MediaPlayer player;
    private String prayerId;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopPlaybackAndSelf();
            return START_NOT_STICKY;
        }

        WakeLockManager.acquire(this.getApplicationContext(), 10 * 60 * 1000L);

        String prayerAr = intent != null
            ? intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER_NAME_AR) : null;
        if (prayerAr == null) prayerAr = "الصلاة";
        prayerId = intent != null ? intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER_ID) : null;

        // ✅ استقبال adhan_file من الـ intent
        String adhanFile = intent != null ? intent.getStringExtra("adhan_file") : null;

        // ✅ وضع المعاينة - بدون إشعار "حان وقت الصلاة"
        boolean isPreview = intent != null && intent.getBooleanExtra("is_preview", false);

        // زر إغلاق الأذان
        Intent stopI = new Intent(this, AdhanService.class).setAction(ACTION_STOP);
        PendingIntent stopPI = PendingIntent.getService(
            this, 0, stopI,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        android.app.Notification notif = isPreview
            ? NotificationHelper.buildSilentForegroundNotification(this)
            : NotificationHelper.buildPrayerNotification(this, prayerAr, prayerId, stopPI);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NotificationHelper.NID_PRAYER, notif,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NotificationHelper.NID_PRAYER, notif);
        }

        startPlayback(adhanFile);
        return START_NOT_STICKY;
    }

    private void startPlayback(String adhanFile) {
        try {
            boolean isFajr = "fajr".equalsIgnoreCase(prayerId);
            int resId = 0;

            if (isFajr) {
                // ✅ أذان الفجر: استخدم adhanFile المرسل (المعاينة) أولاً، ثم المحفوظ
                String fajrFile = (adhanFile != null && !adhanFile.isEmpty())
                        ? adhanFile : PreferencesManager.load(this).selectedFajrAdhanFile;
                if (fajrFile != null && !fajrFile.isEmpty()) {
                    resId = getResources().getIdentifier(fajrFile, "raw", getPackageName());
                }
                if (resId == 0) resId = R.raw.adhan_madinah;
                Log.i(TAG, "Fajr adhan using: " + fajrFile);
            } else {
                // ✅ باقي الصلوات: يستخدم adhanFile المرسل من الـ receiver
                if (adhanFile != null && !adhanFile.isEmpty()) {
                    resId = getResources().getIdentifier(adhanFile, "raw", getPackageName());
                }
                if (resId == 0) {
                    UserSettings s = PreferencesManager.load(this);
                    if (s.selectedAdhanFile != null && !s.selectedAdhanFile.isEmpty()) {
                        resId = getResources().getIdentifier(s.selectedAdhanFile, "raw", getPackageName());
                    }
                }
                if (resId == 0) resId = R.raw.adhan_madinah;
            }

            player = MediaPlayer.create(this, resId);
            if (player == null) {
                Log.e(TAG, "MediaPlayer.create returned null");
                stopPlaybackAndSelf();
                return;
            }
            player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
            player.setOnCompletionListener(mp -> {
                try {
                    Intent post = new Intent(this, com.salah.app.activities.PostAdhanActivity.class);
                    post.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(post);
                } catch (Throwable ignored) {}
                stopPlaybackAndSelf();
            });
            player.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error " + what + "/" + extra);
                stopPlaybackAndSelf();
                return true;
            });
            player.start();
        } catch (Throwable t) {
            Log.e(TAG, "Adhan playback failed", t);
            stopPlaybackAndSelf();
        }
    }

    private void stopPlaybackAndSelf() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
                player = null;
            }
        } catch (Throwable ignored) {}
        try { stopForeground(STOP_FOREGROUND_REMOVE); } catch (Throwable ignored) {}
        try {
            NotificationManagerCompat.from(this).cancel(NotificationHelper.NID_PRAYER);
        } catch (SecurityException ignored) {}
        WakeLockManager.release();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
                player = null;
            }
        } catch (Throwable ignored) {}
        WakeLockManager.release();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
