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

public class FridayKahfReceiver extends BroadcastReceiver {

    private static final String MSG = "ثبت عن ابن عمر أنه كان يقرأ سورة الكهف كل جمعة، ويُرجى لقارئها الثواب العظيم";

    @Override
    public void onReceive(Context context, Intent intent) {
        int notifId = intent.getIntExtra("notif_id", 600);
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, notifId, openIntent,
                PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(context, NotificationHelper.CH_KAHF)
                .setSmallIcon(R.drawable.ic_mosque)
                .setContentTitle("سورة الكهف - يوم الجمعة")
                .setContentText(MSG)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(MSG))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(notifId, n);
    }
}
