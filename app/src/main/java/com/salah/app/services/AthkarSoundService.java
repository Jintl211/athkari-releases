package com.salah.app.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.core.app.NotificationManagerCompat;
import com.salah.app.R;
import com.salah.app.activities.MainActivity;
import com.salah.app.utils.NotificationHelper;

public class AthkarSoundService extends Service {
    private static final String TAG = "AthkarSoundService";
    public static final String ACTION_STOP = "com.salah.app.STOP_ATHKAR_SOUND";
    private static MediaPlayer player;
    private PowerManager.WakeLock wakeLock;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSound();
            stopSelf();
            return START_NOT_STICKY;
        }

        // WakeLock يمنع النوم
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SalahApp:AthkarWakeLock");
        wakeLock.acquire(10 * 60 * 1000L);

        int soundRes = intent != null ? intent.getIntExtra("sound_res", 0) : 0;
        String type  = intent != null ? intent.getStringExtra("athkar_type") : "morning";

        // لازم Foreground عشان يشتغل لما الشاشة مقفلة
        startForeground(203, buildNotification(type));

        if (soundRes != 0) playSound(soundRes);
        return START_NOT_STICKY;
    }

    private void playSound(int resId) {
        stopSound();
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());
            Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + resId);
            player.setDataSource(this, uri);
            player.prepare();
            player.setOnCompletionListener(mp -> {
                mp.release();
                player = null;
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
            });
            player.start();
        } catch (Exception e) {
            Log.e(TAG, "playSound failed", e);
            stopSelf();
        }
    }

    public static void stopSound() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
                player = null;
            }
        } catch (Exception ignored) {}
    }

    private Notification buildNotification(String type) {
        String title = "morning".equals(type) ? "أذكار الصباح" :
                       "evening".equals(type) ? "أذكار المساء" : "أذكار النوم";
        int imageRes = "morning".equals(type) ? R.drawable.athkar_morning_bg :
                       "sleep".equals(type)   ? R.drawable.athkar_sleep_bg :
                       R.drawable.athkar_evening_bg;
        Bitmap bigPicture = BitmapFactory.decodeResource(getResources(), imageRes);

        Intent stopIntent = new Intent(this, AthkarSoundService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 300, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, NotificationHelper.CH_ATHKAR)
            .setSmallIcon(R.drawable.ic_athkar)
            .setContentTitle(title)
            .setContentText("اضغط لإيقاف الصوت")
            .setLargeIcon(bigPicture)
            .setStyle(new NotificationCompat.BigPictureStyle()
                .bigPicture(bigPicture)
                .bigLargeIcon((Bitmap) null))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(openPi)
            .addAction(0, "إيقاف الصوت ✕", stopPi)
            .build();
    }

    @Override
    public void onDestroy() {
        stopSound();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) { return null; }
}
