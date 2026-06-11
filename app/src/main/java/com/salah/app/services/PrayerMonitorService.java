package com.salah.app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.salah.app.R;
import com.salah.app.activities.MainActivity;
import com.salah.app.utils.AlarmScheduler;
import com.salah.app.utils.DuaScheduler;

public class PrayerMonitorService extends Service {
    private static final int NOTIF_ID = 9999;
    private static final String CHANNEL_ID = "prayer_monitor";
    
    private PowerManager.WakeLock wakeLock;
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        // WakeLock للحفاظ على التشغيل
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SalahApp:MonitorWakeLock");
        wakeLock.setReferenceCounted(false);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // الحصول على WakeLock
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
        
        // إعادة جدولة كل الإشعارات
        AlarmScheduler.rescheduleAll(this);
        DuaScheduler.start(this);
        
        // بدء الإشعار الدائم
        startForeground(NOTIF_ID, buildNotification());
        
        return START_STICKY; // إعادة التشغيل تلقائياً
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        // إعادة تشغيل الخدمة إذا توقفت
        startService(new Intent(this, PrayerMonitorService.class));
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "مراقبة أوقات الصلاة",
                    NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription("يعمل في الخلفية لضمان عدم انقطاع الأذان");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
    
    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("أذكاري")
                .setContentText("يعمل في الخلفية - إشعارات الأذان والأذكار نشطة")
                .setSmallIcon(R.drawable.ic_mosque)
                .setContentIntent(pi)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }
}
