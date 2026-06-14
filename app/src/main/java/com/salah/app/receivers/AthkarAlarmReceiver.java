package com.salah.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.os.VibrationEffect;
import android.util.Log;
import androidx.core.app.NotificationManagerCompat;

import com.salah.app.services.AthkarSoundService;
import com.salah.app.utils.AlarmScheduler;
import com.salah.app.utils.NotificationHelper;
import com.salah.app.utils.PreferencesManager;
import com.salah.app.models.UserSettings;
import com.salah.app.R;

import java.util.Calendar;

public class AthkarAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AthkarAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra(AlarmScheduler.EXTRA_ATHKAR_TYPE);
        if (type == null) type = "morning";
        Log.i(TAG, "AthkarAlarm fired: " + type);

        // ✅ التحقق من الوقت قبل التشغيل
        if (!isCorrectTimeWindow(type)) {
            Log.w(TAG, "AthkarAlarm outside time window for: " + type + " - skipping");
            rescheduleNext(context, type);
            return;
        }

        // ✅ تشغيل الصوت عبر AthkarSoundService (حتى يعمل زر الإيقاف)
        int soundRes = getSoundRes(type);
        if (soundRes != 0) {
            Intent svc = new Intent(context, AthkarSoundService.class);
            svc.putExtra("sound_res", soundRes);
            svc.putExtra("athkar_type", type);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svc);
            } else {
                context.startService(svc);
            }
        }

        // إظهار الإشعار
        try {
            android.app.Notification n = NotificationHelper.buildAthkarNotification(context, type);
            int notifId = "morning".equals(type) ? 201 :
                         "sleep".equals(type)   ? 203 : 202;
            NotificationManagerCompat.from(context).notify(notifId, n);

            UserSettings s = PreferencesManager.load(context);
            if (s.vibrateOnAlarm) vibrate(context);
        } catch (SecurityException se) {
            Log.e(TAG, "Notification permission missing", se);
        }

        // إعادة الجدولة لليوم التالي
        rescheduleNext(context, type);
    }

    private int getSoundRes(String type) {
        switch (type) {
            case "morning": return R.raw.adhkar_morning;
            case "evening": return R.raw.adhkar_evening;
            case "sleep":   return R.raw.adhkar_sleep;
            default:        return 0;
        }
    }

    // ✅ التحقق أن الوقت في النافذة الصحيحة (±30 دقيقة)
    private boolean isCorrectTimeWindow(String type) {
        Calendar now = Calendar.getInstance();
        int total = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        switch (type) {
            case "morning": return total >= 330 && total <= 390;  // 5:30-6:30 ص
            case "evening": return total >= 1050 && total <= 1110; // 5:30-6:30 م
            case "sleep":   return total >= 1290 && total <= 1350; // 9:30-10:30 م
            default:        return true;
        }
    }

    private void rescheduleNext(Context context, String type) {
        UserSettings s = PreferencesManager.load(context);
        switch (type) {
            case "morning":
                if (s.morningAthkarEnabled)
                    AlarmScheduler.scheduleAthkar(context, "morning", 5, 30);
                break;
            case "evening":
                if (s.eveningAthkarEnabled)
                    AlarmScheduler.scheduleAthkar(context, "evening", 17, 30);
                break;
            case "sleep":
                AlarmScheduler.scheduleAthkar(context, "sleep", 22, 0);
                break;
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
