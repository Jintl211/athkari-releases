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
import android.os.SystemClock;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.salah.app.R;
import com.salah.app.activities.MainActivity;
import com.salah.app.models.PrayerTime;
import com.salah.app.models.Location;
import com.salah.app.models.UserSettings;
import com.salah.app.utils.PreferencesManager;
import com.salah.app.utils.PrayerTimesCalculator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PrayerNotificationService extends Service {
    private static final String TAG = "PrayerNotifService";
    private static final String CH_ID = "persistent_v2";
    private static final int NID = 1004;
    private Handler handler;
    private Runnable updateRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            createChannel();
            startForeground(NID, buildNotification());
        } catch (Exception e) {
            Log.e(TAG, "onCreate error", e);
            startForeground(NID, buildSimpleNotification());
        }

        handler = new Handler();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if (nm != null) {
                        Notification n = buildNotification();
                        nm.notify(NID, n);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "update error", e);
                }
                handler.postDelayed(this, 60_000L);
            }
        };
        handler.postDelayed(updateRunnable, 60_000L);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CH_ID, "الوقت المتبقي للصلاة", NotificationManager.IMPORTANCE_LOW);
            ch.setSound(null, null);
            ch.enableVibration(false);
            ch.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildSimpleNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Notification n = new NotificationCompat.Builder(this, CH_ID)
            .setSmallIcon(R.drawable.ic_mosque)
            .setContentTitle("أذكاري")
            .setContentText("جاري حساب وقت الصلاة...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pi)
            .build();
        n.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        return n;
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        SimpleDateFormat df = new SimpleDateFormat("EEEE، d MMMM yyyy", new Locale("ar"));
        String today = df.format(new Date());
        String hijri = getHijriDate();

        String prayerName = "جاري الحساب";
        String prayerTime = "";
        String countdown  = "";
        long whenMs = System.currentTimeMillis();
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
                    // ✅ للعرض في شريط الحالة (Chronometer عكسي)
                    whenMs = nextPrayerMs;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "buildNotification error", e);
        }

        String bigText = "الصلاة القادمة: " + prayerName + " (" + prayerTime + ")\n"
                       + "⏱ " + countdown;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CH_ID)
            .setSmallIcon(R.drawable.ic_mosque)
            .setContentTitle(!hijri.isEmpty() ? today + " | " + hijri : today)
            .setContentText("الصلاة القادمة: " + prayerName + "  " + countdown)
            .setStyle(new NotificationCompat.BigTextStyle()
                .setBigContentTitle(!hijri.isEmpty() ? today + "\n" + hijri : today)
                .bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .setColor(0xFF1A3A5C);

        // ✅ Chronometer - يعرض الوقت المتبقي في شريط الحالة
        if (nextPrayerMs > 0) {
            long timeUntilPrayer = nextPrayerMs - System.currentTimeMillis();
            builder.setWhen(System.currentTimeMillis() + timeUntilPrayer)
                   .setUsesChronometer(true)
                   .setChronometerCountDown(true);
        }

        Notification n = builder.build();
        // ✅ منع المسح نهائياً
        n.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        return n;
    }

    private String getHijriDate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                java.time.chrono.HijrahDate hijrah = java.time.chrono.HijrahDate.now();
                int day   = hijrah.get(java.time.temporal.ChronoField.DAY_OF_MONTH);
                int month = hijrah.get(java.time.temporal.ChronoField.MONTH_OF_YEAR);
                int year  = hijrah.get(java.time.temporal.ChronoField.YEAR);
                String[] months = {"محرم","صفر","ربيع الأول","ربيع الثاني",
                    "جمادى الأولى","جمادى الآخرة","رجب","شعبان",
                    "رمضان","شوال","ذو القعدة","ذو الحجة"};
                return day + " " + months[month-1] + " " + year + " هـ";
            }
        } catch (Exception ignored) {}
        return "";
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (handler != null) handler.removeCallbacks(updateRunnable);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
