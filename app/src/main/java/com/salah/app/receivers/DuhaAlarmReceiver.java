package com.salah.app.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.salah.app.R;
import com.salah.app.activities.MainActivity;
import com.salah.app.utils.DuhaReminderScheduler;

public class DuhaAlarmReceiver extends BroadcastReceiver {

    private static final String CH_DUHA = "ch_duha";
    private static final String HADITH_1 =
        "عَنْ أَبي هُريرةَ ، قَالَ: أوصَاني خَليلي ﷺ بِثَلاثٍ: صِيامِ ثَلاثَةِ أَيَّامٍ مِن كُلِّ شَهْرٍ، وركْعَتي الضُّحَى، وأَنْ أُوتِرَ قَبل أَنْ أَرْقُد. متفقٌ عَلَيْهِ.\n\nوَالإيتَارُ قَبْلَ النَّوْمِ إنَّمَا يُسْتَحَبُّ لِمَنْ لاَ يَثِقُ بِالاسْتِيقَاظِ آخِرَ اللَّيل، فإنْ وَثِقَ فَآخِرُ اللَّيل أفْضَلُ.";
    private static final String HADITH_2 =
        "عَنْ عائشةَ رضيَ اللَّه عَنْها، قالتْ: كانَ رسولُ اللَّهِ ﷺ يصلِّي الضُّحَى أَرْبعًا، ويزَيدُ مَا شاءَ اللَّه. رواه مسلم.";

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra("duha_type");
        if (type == null) type = "duha_1";

        createChannel(context);

        boolean isFirst = "duha_1".equals(type);
        String hadith = isFirst ? HADITH_1 : HADITH_2;
        int notifId = isFirst ? 6001 : 6002;

        Intent open = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, notifId, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification n = new NotificationCompat.Builder(context, CH_DUHA)
                .setSmallIcon(R.drawable.ic_mosque)
                .setContentTitle("🌅 تذكير بصلاة الضحى")
                .setContentText(isFirst ? "حديث أبي هريرة ﵁" : "حديث عائشة ﵂")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(hadith))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();

        try {
            NotificationManagerCompat.from(context).notify(notifId, n);
        } catch (SecurityException ignored) {}

        // إعادة الجدولة لليوم التالي
        DuhaReminderScheduler.scheduleAll(context);
    }

    private void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CH_DUHA, "تذكير صلاة الضحى", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("تذكير يومي بصلاة الضحى مع حديث نبوي");
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }
}
