package com.salah.app.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.Build;

import com.salah.app.R;

public class AdhanPlayer {
    private static MediaPlayer mediaPlayer;
    private static PowerManager.WakeLock wakeLock;
    
    // قائمة الأذان المتوفرة
    public static final int[] ADHAN_RESOURCES = {
        R.raw.adhan_madinah,    // 0
        R.raw.adhan_makkah,     // 1
        R.raw.adhan_kuwait,     // 2
        R.raw.adhan_afasy,      // 3
        R.raw.adhan_brunei,     // 4
        R.raw.adhan_quds,       // 5
        R.raw.adhan_haram_makki,// 6
        R.raw.adhan_other       // 7
    };
    
    public static final String[] ADHAN_NAMES = {
        "المدينة المنورة",
        "مكة المكرمة",
        "الكويت",
        "مشاري العفاسي",
        "بروناي",
        "القدس",
        "الحرم المكي",
        "أذان آخر"
    };
    
    public static void play(Context context, int voiceIndex) {
        stop(); // إيقاف أي أذان سابق
        
        // الحصول على WakeLock لمنع النوم
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "SalahApp:AdhanWakeLock");
        wakeLock.acquire(5*60*1000); // 5 دقائق
        
        // اهتزاز
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(2000);
            }
        }
        
        // تشغيل الأذان
        int resourceId = (voiceIndex >= 0 && voiceIndex < ADHAN_RESOURCES.length) 
            ? ADHAN_RESOURCES[voiceIndex] 
            : ADHAN_RESOURCES[0];
            
        mediaPlayer = MediaPlayer.create(context, resourceId);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(false);
            mediaPlayer.setOnCompletionListener(mp -> stop());
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                stop();
                return true;
            });
            mediaPlayer.start();
        }
    }
    
    public static void stop() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                // تجاهل الأخطاء
            }
            mediaPlayer = null;
        }
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }
    
    public static boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }
}
