package com.salah.app.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.salah.app.R;
import com.salah.app.activities.MainActivity;
import com.salah.app.receivers.StopAthkarReceiver;
import com.salah.app.activities.SyncedAdhanActivity;
import com.salah.app.models.PrayerTime;
import com.salah.app.models.Location;
import com.salah.app.models.UserSettings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationHelper {

    public static final String CH_ADHAN      = "adhan_channel";
    public static final String CH_ATHKAR     = "athkar_channel";
    public static final String CH_DUA        = "dua_channel";
    public static final String CH_FOREGROUND = "foreground_channel";
    public static final String CH_SALAWAT    = "salawat_channel";
    public static final String CH_JUMUAH     = "jumuah_channel";
    public static final String CH_KAHF       = "kahf_channel";
    public static final String CH_PERSISTENT = "persistent_channel";

    public static final int NID_PRAYER     = 1001;
    public static final int NID_FOREGROUND = 1002;
    public static final int NID_ATHKAR     = 1003;
    public static final int NID_PERSISTENT = 1004;

    public static void createAllChannels(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel adhan = new NotificationChannel(
                CH_ADHAN, "تنبيهات الأذان", NotificationManager.IMPORTANCE_HIGH);
        adhan.setSound(null, null);
        adhan.enableVibration(false);
        adhan.setBypassDnd(true);
        adhan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(adhan);

        nm.createNotificationChannel(new NotificationChannel(
                CH_ATHKAR, "تذكير الأذكار", NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(new NotificationChannel(
                CH_DUA, "تذكير الدعاء", NotificationManager.IMPORTANCE_DEFAULT));
        nm.createNotificationChannel(new NotificationChannel(
                CH_SALAWAT, "الصلاة على النبي ﷺ", NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(new NotificationChannel(
                CH_JUMUAH, "فضل الجمعة", NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(new NotificationChannel(
                CH_KAHF, "سورة الكهف", NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(new NotificationChannel(
                CH_FOREGROUND, "خدمة الصلاة", NotificationManager.IMPORTANCE_LOW));

        // ✅ قناة الإشعار الثابت - بدون صوت ولا اهتزاز
        NotificationChannel persistent = new NotificationChannel(
                CH_PERSISTENT, "الوقت المتبقي للصلاة", NotificationManager.IMPORTANCE_LOW);
        persistent.setSound(null, null);
        persistent.enableVibration(false);
        persistent.setShowBadge(false);
        nm.createNotificationChannel(persistent);
    }

    // ✅ الإشعار الثابت - يستخدم لوغو athkari
    public static Notification buildPersistentNotification(Context ctx) {
        Intent intent = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        // التاريخ
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE، d MMMM yyyy", new Locale("ar"));
        String today = dateFormat.format(new Date());

        String nextPrayerLine = "جاري حساب وقت الصلاة...";
        String countdownLine  = "";

        try {
            Location loc = PreferencesManager.loadLocation(ctx);
            UserSettings s = PreferencesManager.load(ctx);
            if (loc != null) {
                long now = System.currentTimeMillis();
                PrayerTime next = null;

                List<PrayerTime> todayPrayers = PrayerTimesCalculator.getTodayTimes(loc, s);
                for (PrayerTime p : todayPrayers) {
                    if (p.prayer == PrayerTime.Prayer.SUNRISE) continue;
                    if (p.time.getTime() > now) { next = p; break; }
                }
                if (next == null) {
                    List<PrayerTime> tomorrow = PrayerTimesCalculator.getTomorrowTimes(loc, s);
                    for (PrayerTime p : tomorrow) {
                        if (p.prayer == PrayerTime.Prayer.FAJR) { next = p; break; }
                    }
                }
                if (next != null) {
                    long diff    = next.time.getTime() - now;
                    long hours   = TimeUnit.MILLISECONDS.toHours(diff);
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;

                    SimpleDateFormat tf = new SimpleDateFormat("hh:mm a", new Locale("ar"));
                    nextPrayerLine = "متبقي على صلاة " + next.getArabicName()
                            + "  (" + tf.format(next.time) + ")";
                    if (hours > 0) {
                        countdownLine = hours + ":" + String.format("%02d", minutes)
                                + ":" + String.format("%02d", seconds) + " ساعة";
                    } else {
                        countdownLine = minutes + ":" + String.format("%02d", seconds) + " دقيقة";
                    }
                }
            }
        } catch (Exception ignored) {}

        // ✅ لوغو athkari كصورة كبيرة
        Bitmap logo = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_athkari_logo);

        return new NotificationCompat.Builder(ctx, CH_PERSISTENT)
                .setSmallIcon(R.drawable.ic_mosque)
                .setLargeIcon(logo)
                .setContentTitle(today)
                .setContentText(nextPrayerLine)
                .setSubText(countdownLine)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(today)
                        .bigText(nextPrayerLine + "\n⏱  " + countdownLine))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)       // ✅ ثابت لا يمسح
                .setSilent(true)
                .setShowWhen(false)
                .setOnlyAlertOnce(true)
                .setContentIntent(pi)
                .setColor(0xFF1B3A2F)
                .build();
    }

    // ✅ تحديث الإشعار الثابت (استدعيه كل دقيقة)
    public static void updatePersistentNotification(Context ctx) {
        try {
            NotificationManagerCompat.from(ctx).notify(NID_PERSISTENT,
                    buildPersistentNotification(ctx));
        } catch (SecurityException ignored) {}
    }

    public static Notification buildPrayerNotification(Context ctx, String prayerName,
            String prayerId, PendingIntent stopPI) {
        Intent fsIntent = new Intent(ctx, com.salah.app.activities.AdhanDisplayActivity.class);
        fsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (prayerId != null) fsIntent.putExtra("prayer_id", prayerId);
        PendingIntent fsPI = PendingIntent.getActivity(ctx, 9001, fsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(ctx, CH_ADHAN)
                .setSmallIcon(R.drawable.ic_mosque)
                .setContentTitle("حان وقت صلاة " + prayerName)
                .setContentText("اضغط لإغلاق الأذان")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fsPI, true)
                .setOngoing(true)
                .addAction(0, "إغلاق ✕", stopPI)
                .build();
    }

    // ✅ إشعار صامت للمعاينة - بدون نص "حان وقت الصلاة"
    public static Notification buildSilentForegroundNotification(Context ctx) {
        return new NotificationCompat.Builder(ctx, CH_FOREGROUND)
                .setSmallIcon(R.drawable.ic_mosque)
                .setContentTitle("معاينة الأذان")
                .setContentText("جارٍ تشغيل المعاينة...")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }

    public static Notification buildForegroundNotification(Context ctx, String text) {
        Intent intent = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(ctx, CH_FOREGROUND)
                .setSmallIcon(R.drawable.ic_mosque)
                .setContentTitle("أذكاري")
                .setContentText(text != null ? text : "خدمة أوقات الصلاة تعمل")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pi)
                .build();
    }

    public static Notification buildAthkarNotification(Context ctx, String type) {
        String title = "أذكاري";
        String text  = "حان وقت الأذكار";
        int imageRes = R.drawable.athkar_morning_bg;
        if (type != null) {
            switch (type) {
                case "morning":
                    title = "أذكار الصباح"; text = "لا تنس أذكار الصباح";
                    imageRes = R.drawable.athkar_morning_bg; break;
                case "evening":
                    title = "أذكار المساء"; text = "لا تنس أذكار المساء";
                    imageRes = R.drawable.athkar_evening_bg; break;
                case "sleep":
                    title = "أذكار النوم";  text = "حان وقت أذكار النوم";
                    imageRes = R.drawable.athkar_sleep_bg;   break;
            }
        }
        Bitmap bigPicture = BitmapFactory.decodeResource(ctx.getResources(), imageRes);
        Intent intent = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Intent stopIntent = new Intent(ctx, StopAthkarReceiver.class);
        stopIntent.setAction(StopAthkarReceiver.ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getBroadcast(ctx, 299, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(ctx, CH_ATHKAR)
                .setSmallIcon(R.drawable.ic_athkar)
                .setContentTitle(title)
                .setContentText(text)
                .setLargeIcon(bigPicture)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bigPicture).bigLargeIcon((Bitmap) null))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setFullScreenIntent(pi, true)
                .addAction(0, "إيقاف الصوت ✕", stopPi)
                .build();
    }

    public static void showDuaNotification(Context ctx) {
        String[] duas = {
            "اللهم اغفر لي ولوالدي وللمسلمين",
            "اللهم ارزقني الجنة وما قرب إليها من قول أو عمل",
            "اللهم إني أسألك علماً نافعاً ورزقاً طيباً وعملاً متقبلاً",
            "اللهم صل وسلم على نبينا محمد",
            "سبحان الله وبحمده سبحان الله العظيم",
            "اللهم إني أعوذ بك من الهم والحزن والعجز والكسل",
            "اللهم أعني على ذكرك وشكرك وحسن عبادتك",
            "ربنا آتنا في الدنيا حسنة وفي الآخرة حسنة وقنا عذاب النار",
            "اللهم اجعل القرآن ربيع قلبي ونور صدري",
            "اللهم إني ظلمت نفسي فاغفر لي فإنه لا يغفر الذنوب إلا أنت",
            "سبحان الله والحمد لله ولا إله إلا الله والله أكبر",
            "لا إله إلا الله وحده لا شريك له له الملك وله الحمد وهو على كل شيء قدير",
            "اللهم إني أسألك الجنة وأعوذ بك من النار",
            "اللهم بارك لي في ديني ودنياي وأهلي ومالي",
            "اللهم اكفني بحلالك عن حرامك وأغنني بفضلك عمن سواك",
            "اللهم إني أسألك العفو والعافية في الدنيا والآخرة",
            "حسبي الله لا إله إلا هو عليه توكلت وهو رب العرش العظيم",
            "اللهم إني أسألك حسن الخاتمة",
            "ربي اغفر لي وتب علي إنك أنت التواب الرحيم",
            "اللهم ارحم والدي كما ربياني صغيراً"
        };
        String dua = duas[(int)(Math.random() * duas.length)];
        Intent intent = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Notification n = new NotificationCompat.Builder(ctx, CH_DUA)
                .setSmallIcon(R.drawable.ic_mosque)
                .setContentTitle("تذكير بالدعاء")
                .setContentText(dua)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(dua))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setFullScreenIntent(pi, true)
                .build();
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(7000, n);
    }

    public static void showPrayerNotification(Context ctx, String prayerName) {
        Intent intent = new Intent(ctx, SyncedAdhanActivity.class);
        intent.putExtra("prayer_name", prayerName);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NID_PRAYER, buildPrayerNotification(ctx, prayerName, null, pi));
    }
}
