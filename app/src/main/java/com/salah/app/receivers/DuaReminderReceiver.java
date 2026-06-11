package com.salah.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.salah.app.utils.NotificationHelper;

public class DuaReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // عرض إشعار الدعاء
        NotificationHelper.showDuaNotification(context);
        
        // إعادة الجدولة للساعتين القادمتين
        com.salah.app.utils.DuaScheduler.scheduleNext(context);
    }
}
