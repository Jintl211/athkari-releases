package com.salah.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationManagerCompat;

public class StopAthkarReceiver extends BroadcastReceiver {
    public static final String ACTION_STOP = "com.salah.app.STOP_ATHKAR";

    @Override
    public void onReceive(Context context, Intent intent) {
        // إيقاف الصوت
        try {
            com.salah.app.services.AthkarSoundService.stopSound();
            context.stopService(new Intent(context, com.salah.app.services.AthkarSoundService.class));
        } catch (Exception ignored) {}
        // إيقاف الإشعار
        NotificationManagerCompat.from(context).cancel(201);
        NotificationManagerCompat.from(context).cancel(202);
        NotificationManagerCompat.from(context).cancel(203);
    }
}
