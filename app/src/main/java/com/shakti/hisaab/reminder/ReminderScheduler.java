package com.shakti.hisaab.reminder;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.util.Calendar;

public class ReminderScheduler {
    public static final String KEY_MILK_HOUR = "milk_hour";
    public static final String KEY_MILK_MINUTE = "milk_minute";
    public static final String KEY_MILK_ENABLED = "milk_enabled";
    public static final String KEY_RENT_HOUR = "rent_hour";
    public static final String KEY_RENT_MINUTE = "rent_minute";
    public static final String KEY_RENT_ENABLED = "rent_enabled";
    public static final String KEY_RENT_DAY = "rent_day";
    public static final String KEY_CUSTOM_HOUR = "custom_hour";
    public static final String KEY_CUSTOM_MINUTE = "custom_minute";
    public static final String KEY_CUSTOM_ENABLED = "custom_enabled";
    public static final String KEY_CUSTOM_TITLE = "custom_title";
    public static final String KEY_CUSTOM_MESSAGE = "custom_message";
    public static final String KEY_MASTER_ENABLED = "reminders_master_enabled";

    public static void ensureDefaults(Context context) {
    }

    public static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "reminders_channel", 
                    "Daily Reminders", 
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static boolean isMasterEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_MASTER_ENABLED, true);
    }

    public static void setMasterEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply();
    }

    public static void scheduleAll(Context context) {
        cancelAll(context);
        if (!isMasterEnabled(context)) return;

        SharedPreferences prefs = getPrefs(context);
        
        // Milk Reminder
        if (prefs.getBoolean(KEY_MILK_ENABLED, true)) {
            scheduleDaily(context, 
                    prefs.getInt(KEY_MILK_HOUR, 7), 
                    prefs.getInt(KEY_MILK_MINUTE, 0), 
                    "Milk Reminder", 
                    "Update today's milk entry!", 
                    101);
        }

        // Rent Reminder
        if (prefs.getBoolean(KEY_RENT_ENABLED, true)) {
            scheduleMonthly(context, 
                    prefs.getInt(KEY_RENT_DAY, 1),
                    prefs.getInt(KEY_RENT_HOUR, 9), 
                    prefs.getInt(KEY_RENT_MINUTE, 0), 
                    "Rent & Bills", 
                    "Check and pay your monthly bills.", 
                    102);
        }

        // Custom Reminder
        if (prefs.getBoolean(KEY_CUSTOM_ENABLED, false)) {
            scheduleDaily(context, 
                    prefs.getInt(KEY_CUSTOM_HOUR, 20), 
                    prefs.getInt(KEY_CUSTOM_MINUTE, 0), 
                    prefs.getString(KEY_CUSTOM_TITLE, "Custom Reminder"), 
                    prefs.getString(KEY_CUSTOM_MESSAGE, "Check today's Hisaab tasks."), 
                    103);
        }
    }

    public static void cancelAll(Context context) {
        cancel(context, 101);
        cancel(context, 102);
        cancel(context, 103);
    }

    private static void scheduleDaily(Context context, int hour, int minute, String title, String message, int id) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_TITLE, title);
        intent.putExtra(ReminderReceiver.EXTRA_MESSAGE, message);
        intent.putExtra("extra_id", id);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }
    }

    private static void scheduleMonthly(Context context, int day, int hour, int minute, String title, String message, int id) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_TITLE, title);
        intent.putExtra(ReminderReceiver.EXTRA_MESSAGE, message);
        intent.putExtra("extra_id", id);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.MONTH, 1);
        }

        if (alarmManager != null) {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }
    }

    private static void cancel(Context context, int id) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE);
    }
}
