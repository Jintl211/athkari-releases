package com.salah.app.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.salah.app.R;
import com.salah.app.activities.MainActivity;
import com.salah.app.utils.NotificationHelper;

public class PrePrayerReceiver extends BroadcastReceiver {
    public static final String EXTRA_PRAYER_AR = "prayer_ar";
    public static final String EXTRA_MINUTES   = "minutes_before";

    @Override
    public void onReceive(Context context, Intent intent) {
        String prayerAr = intent.getStringExtra(EXTRA_PRAYER_AR);
        int minutes = intent.getIntExtra(EXTRA_MINUTES, 10);
        if (prayerAr == null) prayerAr = "الصلاة";

        // تشغيل صوت التنبيه
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
            Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.pre_prayer_alert);
            mp.setDataSource(context, soundUri);
            mp.prepare();
            mp.start();
            mp.setOnCompletionListener(MediaPlayer::release);
        } catch (Throwable ignored) {}

        // إظهار الإشعار
        Intent open = new Intent(context, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPI = PendingIntent.getActivity(context, 601, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String text = "اقتربت صلاة " + prayerAr + " — باقٍ على الأذان " + minutes + " دقائق";
        NotificationCompat.Builder b = new NotificationCompat.Builder(context, NotificationHelper.CH_ATHKAR)
            .setSmallIcon(R.drawable.ic_mosque)
            .setContentTitle("⏰ تنبيه قبل الأذان")
            .setContentText(text)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(text + "\nاستعد للصلاة وتوضأ إن استطعت."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPI)
            .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(context).notify(601 + prayerAr.hashCode() % 100, b.build());
        } catch (SecurityException ignored) {}
    }
}
