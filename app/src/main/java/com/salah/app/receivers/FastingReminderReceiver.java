package com.salah.app.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.salah.app.R;
import com.salah.app.activities.MainActivity;
import com.salah.app.utils.FastingReminderScheduler;
import com.salah.app.utils.NotificationHelper;

import java.util.Random;

public class FastingReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "FastingReminderReceiver";
    public static final String EXTRA_DAY = "fast_day";

    private static final String[] HADITHS = {
        "عن أبي هريرة ﵁ أن رسول الله ﷺ قال: «تُعرَضُ الأعمالُ يومَ الإثنينِ والخميسِ، فأُحِبُّ أن يُعرَضَ عملي وأنا صائمٌ».",
        "عن أبي هريرة ﵁ أن رسول الله ﷺ قال: «تُفْتَحُ أبوابُ الجنةِ يومَ الإثنينِ ويومَ الخميسِ، فيُغْفَرُ لكلِّ عبدٍ لا يُشْرِكُ باللهِ شيئًا، إلا رجلًا كانت بينه وبين أخيه شحناء».",
        "عن أبي قتادة الأنصاري ﵁ أن رسول الله ﷺ سُئل عن صوم يوم الإثنين فقال: «فيهِ وُلِدْتُ، وفيهِ أُنْزِلَ عَلَيَّ»."
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        String day = intent.getStringExtra(EXTRA_DAY);
        if (day == null) day = "monday";
        Log.i(TAG, "FastingReminder fired: " + day);

        boolean isMonday = "monday".equals(day);

        String title = isMonday
            ? "تذكير بصيام الإثنين 🌙"
            : "تذكير بصيام الخميس 🌙";

        // حديث عشوائي من الثلاثة
        String hadith = HADITHS[new Random().nextInt(HADITHS.length)];

        String body = (isMonday ? "غداً الإثنين — " : "غداً الخميس — ")
            + "لا تفوّت الأجر.\n\n" + hadith;

        Intent open = new Intent(context, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPI = PendingIntent.getActivity(context, isMonday ? 301 : 302, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CH_ATHKAR)
            .setSmallIcon(R.drawable.ic_athkar)
            .setContentTitle(title)
            .setContentText(isMonday ? "غداً الإثنين — لا تفوّت الأجر" : "غداً الخميس — لا تفوّت الأجر")
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPI)
            .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(isMonday ? 301 : 302, builder.build());
        } catch (SecurityException ignored) {}

        FastingReminderScheduler.scheduleAll(context);
    }
}
