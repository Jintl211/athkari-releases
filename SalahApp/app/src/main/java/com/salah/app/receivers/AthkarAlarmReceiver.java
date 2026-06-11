package com.salah.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.os.VibrationEffect;
import android.util.Log;
import android.media.MediaPlayer;
import android.media.AudioAttributes;
import android.net.Uri;
import androidx.core.app.NotificationManagerCompat;

import com.salah.app.utils.AlarmScheduler;
import com.salah.app.utils.NotificationHelper;
import com.salah.app.utils.PreferencesManager;
import com.salah.app.models.UserSettings;
import com.salah.app.R;

public class AthkarAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AthkarAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra(AlarmScheduler.EXTRA_ATHKAR_TYPE);
        if (type == null) type = "morning";
        Log.i(TAG, "AthkarAlarm fired: " + type);

        // تشغيل الصوت
        playAthkarSound(context, type);

        // إظهار الإشعار
        try {
            android.app.Notification n = NotificationHelper.buildAthkarNotification(context, type);
            int notifId = "morning".equals(type) ? 201 : 
                         "sleep".equals(type) ? 203 : 202;
            NotificationManagerCompat.from(context).notify(notifId, n);
            
            UserSettings s = PreferencesManager.load(context);
            if (s.vibrateOnAlarm) vibrate(context);
        } catch (SecurityException se) {
            Log.e(TAG, "Notification permission missing", se);
        }

        // إعادة الجدولة لليوم التالي
        UserSettings s = PreferencesManager.load(context);
        if ("morning".equals(type) && s.morningAthkarEnabled) {
            AlarmScheduler.scheduleAthkar(context, "morning", 6, 0);
        } else if ("evening".equals(type) && s.eveningAthkarEnabled) {
            AlarmScheduler.scheduleAthkar(context, "evening", 18, 0);
        } else if ("sleep".equals(type)) {
            AlarmScheduler.scheduleAthkar(context, "sleep", 21, 0);
        }
    }

    private void playAthkarSound(Context context, String type) {
        try {
            int soundRes = 0;
            
            if ("morning".equals(type) || "evening".equals(type)) {
                soundRes = R.raw.adhkar_morning;
            } else if ("sleep".equals(type)) {
                soundRes = R.raw.adhkar_sleep;
            }
            
            if (soundRes != 0) {
                MediaPlayer player = new MediaPlayer();
                player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
                
                Uri uri = Uri.parse("android.resource://" + context.getPackageName() + "/" + soundRes);
                player.setDataSource(context, uri);
                player.prepare();
                player.setOnCompletionListener(mp -> mp.release());
                player.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error: " + what + "/" + extra);
                    mp.release();
                    return true;
                });
                player.start();
                Log.i(TAG, "Playing athkar sound: " + type);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to play athkar sound", e);
        }
    }

    private void vibrate(Context ctx) {
        try {
            Vibrator v;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                v = vm != null ? vm.getDefaultVibrator() : null;
            } else {
                v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (v == null) return;
            long[] pattern = {0, 200, 100, 200};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                v.vibrate(pattern, -1);
            }
        } catch (Throwable ignored) {}
    }
}
