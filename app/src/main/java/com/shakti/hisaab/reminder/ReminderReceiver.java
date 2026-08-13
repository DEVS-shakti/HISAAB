package com.shakti.hisaab.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent != null ? intent.getStringExtra(ReminderScheduler.EXTRA_TYPE) : null;
        if (type == null || type.trim().isEmpty()) {
            type = ReminderScheduler.TYPE_MILK;
        }

        ReminderScheduler.showNotification(context, type);
        ReminderScheduler.scheduleReminder(context, type);
    }
}
