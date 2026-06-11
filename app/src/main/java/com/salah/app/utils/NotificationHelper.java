package com.salah.app.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.salah.app.R;
import com.salah.app.activities.MainActivity;
import com.salah.app.receivers.StopAthkarReceiver;
import com.salah.app.activities.SyncedAdhanActivity;

public class NotificationHelper {

    // Channel IDs
    public static final String CH_ADHAN    = "adhan_channel";
    public static final String CH_ATHKAR   = "athkar_channel";
    public static final String CH_DUA      = "dua_channel";
    public static final String CH_FOREGROUND = "foreground_channel";
    public static final String CH_SALAWAT   = "salawat_channel";
    public static final String CH_JUMUAH    = "jumuah_channel";
    public static final String CH_KAHF      = "kahf_channel";

    // Notification IDs
    public static final int NID_PRAYER     = 1001;
    public static final int NID_FOREGROUND = 1002;
    public static final int NID_ATHKAR     = 1003;

    public static void createAllChannels(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null) return;

        // قناة الأذان - تخترق الصمت وشاشة القفل
        NotificationChannel adhan = new NotificationChannel(
                CH_ADHAN, "تنبيهات الأذان", NotificationManager.IMPORTANCE_HIGH);
        adhan.setSound(null, null);
        adhan.enableVibration(false);
        adhan.setBypassDnd(true);
        adhan.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(adhan);

        nm.createNotificationChannel(new NotificationChannel(
                CH_ATHKAR, "تذكير الأذكار", NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(new NotificationChannel(
                CH_DUA, "تذكير الدعاء", NotificationManager.IMPORTANCE_DEFAULT));
        NotificationChannel salawat = new NotificationChannel(
                CH_SALAWAT, "الصلاة على النبي ﷺ", NotificationManager.IMPORTANCE_HIGH);
        salawat.setDescription("تذكير بالصلاة على النبي محمد ﷺ");
        salawat.enableVibration(true);
        nm.createNotificationChannel(salawat);
        NotificationChannel jumuah = new NotificationChannel(
                CH_JUMUAH, "فضل الجمعة", NotificationManager.IMPORTANCE_HIGH);
        jumuah.setDescription("تذكير بفضل التبكير إلى صلاة الجمعة");
        jumuah.enableVibration(true);
        nm.createNotificationChannel(jumuah);
        NotificationChannel kahf = new NotificationChannel(
                CH_KAHF, "سورة الكهف", NotificationManager.IMPORTANCE_HIGH);
        kahf.setDescription("تذكير بقراءة سورة الكهف يوم الجمعة");
        kahf.enableVibration(true);
        nm.createNotificationChannel(kahf);
        NotificationChannel fg = new NotificationChannel(
                CH_FOREGROUND, "خدمة الصلاة", NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(fg);
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
                .setContentText("اضغط لإيقاف الأذان")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fsPI, true)
                .setOngoing(true)
                .addAction(0, "إيقاف ✕", stopPI)
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
        int imageRes  = R.drawable.athkar_morning_bg;
        if (type != null) {
            switch (type) {
                case "morning":
                    title = "أذكار الصباح";
                    text  = "لا تنس أذكار الصباح";
                    imageRes = R.drawable.athkar_morning_bg;
                    break;
                case "evening":
                    title = "أذكار المساء";
                    text  = "لا تنس أذكار المساء";
                    imageRes = R.drawable.athkar_evening_bg;
                    break;
                case "sleep":
                    title = "أذكار النوم";
                    text  = "حان وقت أذكار النوم";
                    imageRes = R.drawable.athkar_sleep_bg;
                    break;
            }
        }
        Bitmap bigPicture = BitmapFactory.decodeResource(ctx.getResources(), imageRes);

        Intent intent = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);
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
                        .bigPicture(bigPicture)
                        .bigLargeIcon((Bitmap) null))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .addAction(0, "إيقاف الصوت ✕", stopPi)
                .build();
    }

    public static void showPrayerNotification(Context ctx, String prayerName) {
        Intent intent = new Intent(ctx, SyncedAdhanActivity.class);
        intent.putExtra("prayer_name", prayerName);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationManager nm = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NID_PRAYER, buildPrayerNotification(ctx, prayerName, null, pi));
    }

    public static void showDuaNotification(Context ctx) {
        String[] duas = {
            "اللهم اغفر لي ولوالدي وللمسلمين",
            "اللهم ارزقني الجنة وما قرب إليها من قول أو عمل",
            "اللهم إني أسألك علماً نافعاً ورزقاً طيباً وعملاً متقبلاً",
            "اللهم صل وسلم على نبينا محمد",
            "سبحان الله وبحمده سبحان الله العظيم"
        };
        String dua = duas[(int)(Math.random() * duas.length)];
        Intent intent = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);
        Notification n = new NotificationCompat.Builder(ctx, CH_DUA)
                .setSmallIcon(R.drawable.ic_athkar)
                .setContentTitle("تذكير بالدعاء")
                .setContentText(dua)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(dua))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        NotificationManager nm = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(7000, n);
    }
}
