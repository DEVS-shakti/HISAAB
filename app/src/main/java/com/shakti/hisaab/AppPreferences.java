package com.shakti.hisaab;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class AppPreferences {
    public static final String PREF_DARK_MODE = "pref_dark_mode";
    private static final String PREF_CURRENCY_CODE = "pref_currency_code";
    private static final String PREF_CATEGORIES = "pref_categories";

    private static final String[] CURRENCY_LABELS = {"Indian Rupee (₹)", "US Dollar ($)", "Euro (€)", "Pound (£)"};
    private static final String[] CURRENCY_CODES = {"INR", "USD", "EUR", "GBP"};
    private static final List<String> DEFAULT_CATEGORIES = Arrays.asList("Food", "Transport", "Shopping", "Entertainment", "Bills", "Milk", "Rent", "Others");

    public static SharedPreferences getUiPreferences(Context context) {
        return context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE);
    }

    public static String formatAmount(Context context, double amount) {
        String code = getCurrencyCode(context);
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
        try {
            format.setCurrency(Currency.getInstance(code));
        } catch (Exception e) {
            // fallback if currency code is invalid
        }
        return format.format(amount);
    }

    public static String getCurrencyLabel(Context context) {
        String code = getCurrencyCode(context);
        for (int i = 0; i < CURRENCY_CODES.length; i++) {
            if (CURRENCY_CODES[i].equals(code)) {
                return CURRENCY_LABELS[i];
            }
        }
        return CURRENCY_LABELS[0];
    }

    public static String[] getCurrencyLabels() {
        return CURRENCY_LABELS;
    }

    public static String[] getCurrencyCodes() {
        return CURRENCY_CODES;
    }

    public static String getCurrencyCode(Context context) {
        return getUiPreferences(context).getString(PREF_CURRENCY_CODE, "INR");
    }

    public static void setCurrencyCode(Context context, String code) {
        getUiPreferences(context).edit().putString(PREF_CURRENCY_CODE, code).apply();
    }

    public static List<String> getCategories(Context context) {
        String cats = getUiPreferences(context).getString(PREF_CATEGORIES, null);
        if (cats == null || cats.trim().isEmpty()) {
            return new ArrayList<>(DEFAULT_CATEGORIES);
        }
        return new ArrayList<>(Arrays.asList(cats.split(",")));
    }

    public static void saveCategories(Context context, List<String> categories) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < categories.size(); i++) {
            sb.append(categories.get(i));
            if (i < categories.size() - 1) {
                sb.append(",");
            }
        }
        getUiPreferences(context).edit().putString(PREF_CATEGORIES, sb.toString()).apply();
    }

    public static boolean isReservedCategory(String category) {
        return "Milk".equalsIgnoreCase(category);
    }

    public static String normalizeCategoryName(String name) {
        if (name == null) return "";
        return name.trim();
    }
}
