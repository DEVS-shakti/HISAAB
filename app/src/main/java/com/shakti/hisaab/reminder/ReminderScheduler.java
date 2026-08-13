package com.shakti.hisaab.reminder;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.shakti.hisaab.MainActivity;
import com.shakti.hisaab.R;

import java.util.Calendar;

public final class ReminderScheduler {

    public static final String PREFS_NAME = "hisaab_reminders";
    public static final String CHANNEL_ID = "hisaab_reminders_channel";
    public static final String EXTRA_TYPE = "reminder_type";

    public static final String TYPE_MILK = "milk";
    public static final String TYPE_RENT = "rent";
    public static final String TYPE_CUSTOM = "custom";

    public static final String KEY_MASTER_ENABLED = "master_enabled";
    public static final String KEY_MILK_ENABLED = "milk_enabled";
    public static final String KEY_MILK_HOUR = "milk_hour";
    public static final String KEY_MILK_MINUTE = "milk_minute";
    public static final String KEY_RENT_ENABLED = "rent_enabled";
    public static final String KEY_RENT_DAY = "rent_day";
    public static final String KEY_RENT_HOUR = "rent_hour";
    public static final String KEY_RENT_MINUTE = "rent_minute";
    public static final String KEY_CUSTOM_ENABLED = "custom_enabled";
    public static final String KEY_CUSTOM_TITLE = "custom_title";
    public static final String KEY_CUSTOM_MESSAGE = "custom_message";
    public static final String KEY_CUSTOM_HOUR = "custom_hour";
    public static final String KEY_CUSTOM_MINUTE = "custom_minute";

    private static final int REQUEST_CODE_MILK = 1101;
    private static final int REQUEST_CODE_RENT = 1102;
    private static final int REQUEST_CODE_CUSTOM = 1103;
    private static final int NOTIFICATION_ID_MILK = 2101;
    private static final int NOTIFICATION_ID_RENT = 2102;
    private static final int NOTIFICATION_ID_CUSTOM = 2103;

    private ReminderScheduler() {
    }

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void ensureDefaults(Context context) {
        SharedPreferences prefs = getPrefs(context);
        if (!prefs.contains(KEY_MASTER_ENABLED)) {
            prefs.edit()
                    .putBoolean(KEY_MASTER_ENABLED, true)
                    .putBoolean(KEY_MILK_ENABLED, true)
                    .putInt(KEY_MILK_HOUR, 7)
                    .putInt(KEY_MILK_MINUTE, 0)
                    .putBoolean(KEY_RENT_ENABLED, true)
                    .putInt(KEY_RENT_DAY, 1)
                    .putInt(KEY_RENT_HOUR, 9)
                    .putInt(KEY_RENT_MINUTE, 0)
                    .putBoolean(KEY_CUSTOM_ENABLED, false)
                    .putString(KEY_CUSTOM_TITLE, "Custom Reminder")
                    .putString(KEY_CUSTOM_MESSAGE, "Check today's Hisaab tasks.")
                    .putInt(KEY_CUSTOM_HOUR, 20)
                    .putInt(KEY_CUSTOM_MINUTE, 0)
                    .apply();
        }
    }

    public static boolean isMasterEnabled(Context context) {
        ensureDefaults(context);
        return getPrefs(context).getBoolean(KEY_MASTER_ENABLED, true);
    }

    public static void setMasterEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply();
    }

    public static void scheduleAll(Context context) {
        ensureDefaults(context);
        ensureNotificationChannel(context);

        if (!isMasterEnabled(context)) {
            cancelAll(context);
            return;
        }

        SharedPreferences prefs = getPrefs(context);
        if (prefs.getBoolean(KEY_MILK_ENABLED, true)) {
            scheduleReminder(context, TYPE_MILK);
        } else {
            cancelReminder(context, TYPE_MILK);
        }

        if (prefs.getBoolean(KEY_RENT_ENABLED, true)) {
            scheduleReminder(context, TYPE_RENT);
        } else {
            cancelReminder(context, TYPE_RENT);
        }

        if (prefs.getBoolean(KEY_CUSTOM_ENABLED, false)) {
            scheduleReminder(context, TYPE_CUSTOM);
        } else {
            cancelReminder(context, TYPE_CUSTOM);
        }
    }

    public static void cancelAll(Context context) {
        cancelReminder(context, TYPE_MILK);
        cancelReminder(context, TYPE_RENT);
        cancelReminder(context, TYPE_CUSTOM);
    }

    public static void scheduleReminder(Context context, String type) {
        PendingIntent pendingIntent = buildPendingIntent(context, type, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null || pendingIntent == null) {
            return;
        }

        long triggerAtMillis = getNextTriggerAt(context, type, System.currentTimeMillis());

        // Exact alarm check for Android 12+ (API 31)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } catch (SecurityException e) {
            // Fallback for SecurityException on newer Android versions
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public static void cancelReminder(Context context, String type) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = buildPendingIntent(context, type, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    public static void showNotification(Context context, String type) {
        ensureNotificationChannel(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                getNotificationId(type),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_hisaab)
                .setContentTitle(getNotificationTitle(context, type))
                .setContentText(getNotificationMessage(context, type))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setColor(ContextCompat.getColor(context, R.color.primary));

        NotificationManagerCompat.from(context).notify(getNotificationId(type), builder.build());
    }

    public static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Hisaab Reminders",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Daily and monthly expense reminders for Hisaab.");
        manager.createNotificationChannel(channel);
    }

    private static PendingIntent buildPendingIntent(Context context, String type, int flags) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(EXTRA_TYPE, type);
        return PendingIntent.getBroadcast(context, getRequestCode(type), intent, flags);
    }

    private static long getNextTriggerAt(Context context, String type, long nowMillis) {
        SharedPreferences prefs = getPrefs(context);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nowMillis);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (TYPE_MILK.equals(type)) {
            calendar.set(Calendar.HOUR_OF_DAY, prefs.getInt(KEY_MILK_HOUR, 7));
            calendar.set(Calendar.MINUTE, prefs.getInt(KEY_MILK_MINUTE, 0));
            if (calendar.getTimeInMillis() <= nowMillis) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            return calendar.getTimeInMillis();
        }

        if (TYPE_RENT.equals(type)) {
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, prefs.getInt(KEY_RENT_HOUR, 9));
            calendar.set(Calendar.MINUTE, prefs.getInt(KEY_RENT_MINUTE, 0));

            int requestedDay = clampRentDay(prefs.getInt(KEY_RENT_DAY, 1));
            int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            calendar.set(Calendar.DAY_OF_MONTH, Math.min(requestedDay, maxDay));
            if (calendar.getTimeInMillis() <= nowMillis) {
                calendar.add(Calendar.MONTH, 1);
                maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                calendar.set(Calendar.DAY_OF_MONTH, Math.min(requestedDay, maxDay));
            }
            return calendar.getTimeInMillis();
        }

        calendar.set(Calendar.HOUR_OF_DAY, prefs.getInt(KEY_CUSTOM_HOUR, 20));
        calendar.set(Calendar.MINUTE, prefs.getInt(KEY_CUSTOM_MINUTE, 0));
        if (calendar.getTimeInMillis() <= nowMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return calendar.getTimeInMillis();
    }

    private static int clampRentDay(int day) {
        if (day < 1) {
            return 1;
        }
        return Math.min(day, 28);
    }

    private static int getRequestCode(String type) {
        if (TYPE_RENT.equals(type)) {
            return REQUEST_CODE_RENT;
        }
        if (TYPE_CUSTOM.equals(type)) {
            return REQUEST_CODE_CUSTOM;
        }
        return REQUEST_CODE_MILK;
    }

    private static int getNotificationId(String type) {
        if (TYPE_RENT.equals(type)) {
            return NOTIFICATION_ID_RENT;
        }
        if (TYPE_CUSTOM.equals(type)) {
            return NOTIFICATION_ID_CUSTOM;
        }
        return NOTIFICATION_ID_MILK;
    }

    private static String getNotificationTitle(Context context, String type) {
        SharedPreferences prefs = getPrefs(context);
        if (TYPE_RENT.equals(type)) {
            return "Hisaab Rent Reminder";
        }
        if (TYPE_CUSTOM.equals(type)) {
            return prefs.getString(KEY_CUSTOM_TITLE, "Hisaab Reminder");
        }
        return "Hisaab Milk Reminder";
    }

    private static String getNotificationMessage(Context context, String type) {
        SharedPreferences prefs = getPrefs(context);
        if (TYPE_RENT.equals(type)) {
            int day = clampRentDay(prefs.getInt(KEY_RENT_DAY, 1));
            return "Rent is due on day " + day + ". Review or mark it paid.";
        }
        if (TYPE_CUSTOM.equals(type)) {
            return prefs.getString(KEY_CUSTOM_MESSAGE, "Check today's Hisaab tasks.");
        }
        return "Add today's milk entry so your calendar stays current.";
    }
}
