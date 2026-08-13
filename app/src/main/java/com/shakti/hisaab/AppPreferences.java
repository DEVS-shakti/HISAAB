package com.shakti.hisaab;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class AppPreferences {

    public static final String PREFS_UI = "hisaab_ui";
    public static final String PREF_DARK_MODE = "dark_mode";
    public static final String PREF_CURRENCY = "currency_code";
    public static final String PREF_CATEGORIES = "custom_categories";

    private static final String CURRENCY_INR = "INR";
    private static final String CURRENCY_USD = "USD";
    private static final String CURRENCY_EUR = "EUR";
    private static final String CURRENCY_GBP = "GBP";

    private static final List<String> DEFAULT_CATEGORIES = Arrays.asList(
            "Milk",
            "Rent",
            "Groceries",
            "Electricity",
            "Vegetables",
            "Travel"
    );

    private AppPreferences() {
    }

    public static SharedPreferences getUiPreferences(Context context) {
        return context.getSharedPreferences(PREFS_UI, Context.MODE_PRIVATE);
    }

    public static List<String> getCategories(Context context) {
        String json = getUiPreferences(context).getString(PREF_CATEGORIES, "");
        if (TextUtils.isEmpty(json)) {
            return new ArrayList<>(DEFAULT_CATEGORIES);
        }

        List<String> categories = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                String category = normalizeCategoryName(array.optString(i, ""));
                if (!TextUtils.isEmpty(category) && !categories.contains(category)) {
                    categories.add(category);
                }
            }
        } catch (JSONException ignored) {
        }

        if (categories.isEmpty()) {
            categories.addAll(DEFAULT_CATEGORIES);
        }
        return categories;
    }

    public static void saveCategories(Context context, List<String> categories) {
        JSONArray array = new JSONArray();
        for (String category : categories) {
            String normalized = normalizeCategoryName(category);
            if (!TextUtils.isEmpty(normalized)) {
                array.put(normalized);
            }
        }
        getUiPreferences(context).edit().putString(PREF_CATEGORIES, array.toString()).apply();
    }

    public static String normalizeCategoryName(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.substring(0, 1).toUpperCase(Locale.getDefault()) + trimmed.substring(1);
    }

    public static boolean isReservedCategory(String category) {
        return "Milk".equalsIgnoreCase(category);
    }

    public static String getCurrencyCode(Context context) {
        return getUiPreferences(context).getString(PREF_CURRENCY, CURRENCY_INR);
    }

    public static String[] getCurrencyCodes() {
        return new String[]{CURRENCY_INR, CURRENCY_USD, CURRENCY_EUR, CURRENCY_GBP};
    }

    public static String[] getCurrencyLabels() {
        return new String[]{
                "Indian Rupee (Rs)",
                "US Dollar ($)",
                "Euro (EUR)",
                "British Pound (£)"
        };
    }

    public static String getCurrencyLabel(Context context) {
        return getCurrencyLabel(getCurrencyCode(context));
    }

    public static String getCurrencyLabel(String code) {
        if (CURRENCY_USD.equals(code)) {
            return "US Dollar ($)";
        }
        if (CURRENCY_EUR.equals(code)) {
            return "Euro (EUR)";
        }
        if (CURRENCY_GBP.equals(code)) {
            return "British Pound (£)";
        }
        return "Indian Rupee (Rs)";
    }

    public static void setCurrencyCode(Context context, String code) {
        getUiPreferences(context).edit().putString(PREF_CURRENCY, code).apply();
    }

    public static String formatAmount(Context context, double amount) {
        String formatted;
        if (amount == (long) amount) {
            formatted = String.format(Locale.getDefault(), "%.0f", amount);
        } else {
            formatted = String.format(Locale.getDefault(), "%.2f", amount);
            if (formatted.contains(".")) {
                formatted = formatted.replaceAll("0*$", "").replaceAll("\\.$", "");
            }
        }
        return getCurrencySymbol(getCurrencyCode(context)) + " " + formatted;
    }

    private static String getCurrencySymbol(String code) {
        if (CURRENCY_USD.equals(code)) {
            return "$";
        }
        if (CURRENCY_EUR.equals(code)) {
            return "EUR";
        }
        if (CURRENCY_GBP.equals(code)) {
            return "GBP";
        }
        return "Rs";
    }
}
