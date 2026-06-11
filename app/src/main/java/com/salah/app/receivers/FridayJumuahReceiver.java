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

public class FridayJumuahReceiver extends BroadcastReceiver {

    private static final String HADITH = "الرائح إلى الجمعة في الساعة الأولى كالمقرِّب بدنة، وفي الساعة الثانية كالمقرِّب بقرة، وفي الساعة الثالثة كالمقرِّب كبشًا، وفي الرابعة كالمهدي دجاجة، والخامسة كالمهدي بيضة ﷺ";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 701, openIntent,
                PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(context, NotificationHelper.CH_JUMUAH)
                .setSmallIcon(R.drawable.ic_mosque)
                .setContentTitle("فضل التبكير إلى الجمعة")
                .setContentText("الرائح في الساعة الأولى كالمقرِّب بدنة...")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(HADITH))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(500, n);
    }
}
