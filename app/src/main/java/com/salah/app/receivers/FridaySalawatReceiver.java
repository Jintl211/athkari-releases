package com.salah.app.receivers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import com.salah.app.R;
import com.salah.app.activities.MainActivity;
import com.salah.app.utils.NotificationHelper;

public class FridaySalawatReceiver extends BroadcastReceiver {

    private static final String[] AHADITH = {
        "مَنْ صَلَّى عَلَيَّ صَلَاةً صَلَّى اللهُ عَلَيْهِ بِهَا عَشْرًا ﷺ",
        "أَكْثِرُوا الصَّلَاةَ عَلَيَّ يَوْمَ الْجُمُعَةِ وَلَيْلَةَ الْجُمُعَةِ ﷺ",
        "إِنَّ أَوْلَى النَّاسِ بِي يَوْمَ الْقِيَامَةِ أَكْثَرُهُمْ عَلَيَّ صَلَاةً ﷺ",
        "مَنْ صَلَّى عَلَيَّ حِينَ يُصْبِحُ عَشْرًا وَحِينَ يُمْسِي عَشْرًا أَدْرَكَتْهُ شَفَاعَتِي ﷺ",
        "الْبَخِيلُ مَنْ ذُكِرْتُ عِنْدَهُ فَلَمْ يُصَلِّ عَلَيَّ ﷺ",
        "مَنْ صَلَّى عَلَيَّ وَاحِدَةً صَلَّى اللهُ عَلَيْهِ عَشْرًا وَحَطَّ عَنْهُ عَشْرَ خَطِيئَاتٍ ﷺ",
        "يَوْمُ الْجُمُعَةِ خَيْرُ يَوْمٍ طَلَعَتْ عَلَيْهِ الشَّمْسُ فَأَكْثِرُوا عَلَيَّ مِنَ الصَّلَاةِ ﷺ"
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        int index = intent.getIntExtra("hadith_index", 0);
        String hadith = AHADITH[index % AHADITH.length];

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, openIntent,
                PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(context, NotificationHelper.CH_SALAWAT)
                .setSmallIcon(R.drawable.ic_athkar)
                .setContentTitle("الصلاة على النبي ﷺ")
                .setContentText(hadith)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(hadith))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(400 + index, n);

        com.salah.app.utils.FridaySalawatScheduler.reschedule(context, index);
    }
}
