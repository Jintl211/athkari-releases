package com.salah.app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.salah.app.R;
import com.salah.app.activities.MainActivity;
import com.salah.app.models.PrayerTime;
import com.salah.app.models.Location;
import com.salah.app.models.UserSettings;
import com.salah.app.utils.AlarmScheduler;
import com.salah.app.utils.DuaScheduler;
import com.salah.app.utils.NotificationHelper;
import com.salah.app.utils.PreferencesManager;
import com.salah.app.utils.PrayerTimesCalculator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PrayerMonitorService extends Service {
    private static final String TAG = "PrayerMonitorService";
    // ✅ إشعار الخلفية القديم - مستقل
    private static final int NOTIF_ID_BG = 9999;
    private static final String CHANNEL_BG = "prayer_monitor";
    // ✅ إشعار الوقت المتبقي - مستقل
    private static final int NOTIF_ID_TIMER = 8888;
    private static final String CHANNEL_TIMER = "prayer_timer";

    private PowerManager.WakeLock wakeLock;
    private Handler handler;
    private Runnable updateRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels();

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SalahApp:MonitorWakeLock");
        wakeLock.setReferenceCounted(false);

        // ✅ إشعار الخلفية القديم كـ Foreground
        startForeground(NOTIF_ID_BG, buildBgNotification());

        // ✅ إشعار الوقت المتبقي بعد ثانية
        new Handler().postDelayed(() -> {
            try {
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.notify(NOTIF_ID_TIMER, buildTimerNotification());
            } catch (Exception ignored) {}
        }, 1000);

        handler = new Handler();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if (nm != null) nm.notify(NOTIF_ID_TIMER, buildTimerNotification());
                } catch (Exception e) {
                    Log.e(TAG, "update error", e);
                }
                handler.postDelayed(this, 60_000L);
            }
        };
        handler.postDelayed(updateRunnable, 61_000L);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (wakeLock != null && !wakeLock.isHeld()) wakeLock.acquire();
        AlarmScheduler.rescheduleAll(this);
        DuaScheduler.start(this);
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restart = new Intent(this, PrayerMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restart);
        } else {
            startService(restart);
        }
        super.onTaskRemoved(rootIntent);
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            // قناة الخلفية القديمة
            NotificationChannel bg = new NotificationChannel(
                CHANNEL_BG, "مراقبة أوقات الصلاة", NotificationManager.IMPORTANCE_MIN);
            bg.setSound(null, null);
            bg.enableVibration(false);
            nm.createNotificationChannel(bg);

            // قناة إشعار الوقت المتبقي
            NotificationChannel timer = new NotificationChannel(
                CHANNEL_TIMER, "الوقت المتبقي للصلاة", NotificationManager.IMPORTANCE_LOW);
            timer.setSound(null, null);
            timer.enableVibration(false);
            timer.setShowBadge(false);
            nm.createNotificationChannel(timer);
        }
    }

    // ✅ إشعار الخلفية القديم
    private Notification buildBgNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_BG)
            .setSmallIcon(R.drawable.ic_mosque)
            .setContentTitle("أذكاري")
            .setContentText("يعمل في الخلفية - إشعارات الأذان والأذكار نشطة")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pi)
            .build();
    }

    // ✅ إشعار الوقت المتبقي المستقل
    private Notification buildTimerNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        SimpleDateFormat df = new SimpleDateFormat("EEEE، d MMMM yyyy", new Locale("ar"));
        String today = df.format(new Date());
        String hijri = getHijriDate();

        String prayerName = "جاري الحساب";
        String prayerTime = "";
        String countdown  = "";
        long nextPrayerMs = 0;

        try {
            Location loc = PreferencesManager.loadLocation(this);
            UserSettings s = PreferencesManager.load(this);
            if (loc != null) {
                long now = System.currentTimeMillis();
                PrayerTime next = null;
                List<PrayerTime> todayList = PrayerTimesCalculator.getTodayTimes(loc, s);
                for (PrayerTime p : todayList) {
                    if (p.prayer == PrayerTime.Prayer.SUNRISE) continue;
                    if (p.time.getTime() > now) { next = p; break; }
                }
                if (next == null) {
                    List<PrayerTime> tmrw = PrayerTimesCalculator.getTomorrowTimes(loc, s);
                    for (PrayerTime p : tmrw) {
                        if (p.prayer == PrayerTime.Prayer.FAJR) { next = p; break; }
                    }
                }
                if (next != null) {
                    long diff = next.time.getTime() - now;
                    long hrs  = TimeUnit.MILLISECONDS.toHours(diff);
                    long mins = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
                    long secs = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;
                    prayerName   = next.getArabicName();
                    SimpleDateFormat tf = new SimpleDateFormat("hh:mm a", new Locale("ar"));
                    prayerTime   = tf.format(next.time);
                    nextPrayerMs = next.time.getTime();
                    if (hrs > 0) {
                        countdown = hrs + ":" + String.format("%02d", mins) + ":" + String.format("%02d", secs) + "-";
                    } else {
                        countdown = mins + ":" + String.format("%02d", secs) + "-";
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "buildTimerNotification error", e);
        }

        String header  = !hijri.isEmpty() ? today + " | " + hijri : today;
        String content = "الصلاة القادمة: " + prayerName + " (" + prayerTime + ")";
        String bigText = content + "\n⏱ " + countdown;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_mosque)
            .setContentTitle(header)
            .setContentText(content)
            .setStyle(new NotificationCompat.BigTextStyle()
                .setBigContentTitle(header)
                .bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi);

        if (nextPrayerMs > 0) {
            long diff = nextPrayerMs - System.currentTimeMillis();
            builder.setWhen(System.currentTimeMillis() + diff)
                   .setUsesChronometer(true)
                   .setChronometerCountDown(true);
        }

        Notification n = builder.build();
        n.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        return n;
    }

    private String getHijriDate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                java.time.chrono.HijrahDate h = java.time.chrono.HijrahDate.now();
                int d = h.get(java.time.temporal.ChronoField.DAY_OF_MONTH);
                int m = h.get(java.time.temporal.ChronoField.MONTH_OF_YEAR);
                int y = h.get(java.time.temporal.ChronoField.YEAR);
                String[] months = {"محرم","صفر","ربيع الأول","ربيع الثاني",
                    "جمادى الأولى","جمادى الآخرة","رجب","شعبان",
                    "رمضان","شوال","ذو القعدة","ذو الحجة"};
                return d + " " + months[m-1] + " " + y + " هـ";
            }
        } catch (Exception ignored) {}
        return "";
    }

    @Override
    public void onDestroy() {
        if (handler != null) handler.removeCallbacks(updateRunnable);
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
