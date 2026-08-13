package com.shakti.hisaab.backup;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.shakti.hisaab.database.AppDatabase;
import com.shakti.hisaab.database.dao.ExpenseDao;
import com.shakti.hisaab.database.dao.MilkEntryDao;
import com.shakti.hisaab.database.entities.Expense;
import com.shakti.hisaab.database.entities.MilkEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class AppBackupManager {

    private static final String PREFS_UI = "hisaab_ui";
    private static final String PREFS_MILK = "hisaab_settings";
    private static final String PREFS_REMINDERS = "hisaab_reminders";

    private AppBackupManager() {
    }

    public static void exportBackup(Context context, Uri uri) throws Exception {
        AppDatabase database = AppDatabase.getInstance(context);
        MilkEntryDao milkEntryDao = database.milkEntryDao();
        ExpenseDao expenseDao = database.expenseDao();

        JSONObject root = new JSONObject();
        root.put("schemaVersion", 1);
        root.put("exportedAt", System.currentTimeMillis());
        root.put("preferences", buildPreferencesPayload(context));
        root.put("milkEntries", buildMilkEntriesPayload(milkEntryDao.getAllEntries()));
        root.put("expenses", buildExpensesPayload(expenseDao.getAllExpensesSync()));

        try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) {
                throw new IllegalStateException("Unable to open backup destination.");
            }
            outputStream.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
    }

    public static void restoreBackup(Context context, Uri uri) throws Exception {
        String json = readText(context, uri);
        JSONObject root = new JSONObject(json);
        JSONObject preferences = root.optJSONObject("preferences");
        JSONArray milkEntries = root.optJSONArray("milkEntries");
        JSONArray expenses = root.optJSONArray("expenses");

        AppDatabase database = AppDatabase.getInstance(context);
        MilkEntryDao milkEntryDao = database.milkEntryDao();
        ExpenseDao expenseDao = database.expenseDao();

        database.runInTransaction(() -> {
            milkEntryDao.deleteAll();
            expenseDao.deleteAll();

            if (milkEntries != null) {
                for (int i = 0; i < milkEntries.length(); i++) {
                    JSONObject item = milkEntries.optJSONObject(i);
                    if (item == null) {
                        continue;
                    }
                    MilkEntry entry = new MilkEntry(
                            item.optString("date", ""),
                            item.optBoolean("taken", false),
                            item.optBoolean("paid", false),
                            item.optDouble("quantity", 0),
                            item.optDouble("pricePerLiter", 0),
                            item.optDouble("totalCost", 0)
                    );
                    entry.id = item.optInt("id", 0);
                    milkEntryDao.insert(entry);
                }
            }

            if (expenses != null) {
                for (int i = 0; i < expenses.length(); i++) {
                    JSONObject item = expenses.optJSONObject(i);
                    if (item == null) {
                        continue;
                    }
                    Expense expense = new Expense(
                            item.optString("category", "Other"),
                            item.optDouble("amount", 0),
                            item.optLong("dateMillis", 0),
                            item.optString("note", ""),
                            item.optBoolean("isPaid", false)
                    );
                    expense.id = item.optInt("id", 0);
                    expenseDao.insert(expense);
                }
            }
        });

        if (preferences != null) {
            restorePreferences(context, preferences);
        }
    }

    private static JSONObject buildPreferencesPayload(Context context) throws JSONException {
        JSONObject preferences = new JSONObject();
        preferences.put(PREFS_UI, sharedPreferencesToJson(context.getSharedPreferences(PREFS_UI, Context.MODE_PRIVATE)));
        preferences.put(PREFS_MILK, sharedPreferencesToJson(context.getSharedPreferences(PREFS_MILK, Context.MODE_PRIVATE)));
        preferences.put(PREFS_REMINDERS, sharedPreferencesToJson(context.getSharedPreferences(PREFS_REMINDERS, Context.MODE_PRIVATE)));
        return preferences;
    }

    private static JSONArray buildMilkEntriesPayload(List<MilkEntry> entries) throws JSONException {
        JSONArray array = new JSONArray();
        for (MilkEntry entry : entries) {
            JSONObject item = new JSONObject();
            item.put("id", entry.id);
            item.put("date", entry.date);
            item.put("taken", entry.taken);
            item.put("paid", entry.paid);
            item.put("quantity", entry.quantity);
            item.put("pricePerLiter", entry.pricePerLiter);
            item.put("totalCost", entry.totalCost);
            array.put(item);
        }
        return array;
    }

    private static JSONArray buildExpensesPayload(List<Expense> expenses) throws JSONException {
        JSONArray array = new JSONArray();
        for (Expense expense : expenses) {
            JSONObject item = new JSONObject();
            item.put("id", expense.id);
            item.put("category", expense.category);
            item.put("amount", expense.amount);
            item.put("dateMillis", expense.dateMillis);
            item.put("note", expense.note == null ? "" : expense.note);
            item.put("isPaid", expense.isPaid);
            array.put(item);
        }
        return array;
    }

    private static JSONObject sharedPreferencesToJson(SharedPreferences preferences) throws JSONException {
        JSONObject object = new JSONObject();
        Map<String, ?> all = preferences.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Boolean || value instanceof Integer || value instanceof Long
                    || value instanceof Float || value instanceof String) {
                object.put(entry.getKey(), value);
            }
        }
        return object;
    }

    private static void restorePreferences(Context context, JSONObject preferences) {
        restorePreferenceFile(context, PREFS_UI, preferences.optJSONObject(PREFS_UI));
        restorePreferenceFile(context, PREFS_MILK, preferences.optJSONObject(PREFS_MILK));
        restorePreferenceFile(context, PREFS_REMINDERS, preferences.optJSONObject(PREFS_REMINDERS));
    }

    private static void restorePreferenceFile(Context context, String name, JSONObject payload) {
        SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit();
        editor.clear();
        if (payload != null) {
            Iterator<String> keys = payload.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = payload.opt(key);
                if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Number) {
                    Number num = (Number) value;
                    if (PREFS_MILK.equals(name)) {
                        // Double values are stored as long bits in hisaab_settings
                        editor.putLong(key, num.longValue());
                    } else if (PREFS_REMINDERS.equals(name)) {
                        // All numeric settings in hisaab_reminders are integers (hours, minutes, days)
                        editor.putInt(key, num.intValue());
                    } else {
                        // Generic fallback for other potential preferences
                        double d = num.doubleValue();
                        if (d == Math.floor(d) && !Double.isInfinite(d)) {
                            if (d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE) {
                                editor.putInt(key, num.intValue());
                            } else {
                                editor.putLong(key, num.longValue());
                            }
                        } else {
                            editor.putFloat(key, num.floatValue());
                        }
                    }
                } else if (value instanceof String) {
                    editor.putString(key, (String) value);
                }
            }
        }
        editor.commit();
    }

    private static String readText(Context context, Uri uri) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new IllegalStateException("Unable to open backup file.");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            }
        }
        return builder.toString();
    }
}
