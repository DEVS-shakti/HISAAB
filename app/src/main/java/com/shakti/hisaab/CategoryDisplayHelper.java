package com.shakti.hisaab;

public final class CategoryDisplayHelper {

    private CategoryDisplayHelper() {
    }

    public static String getSubtitle(String category) {
        if ("Milk".equalsIgnoreCase(category)) {
            return "Updated today";
        }
        if ("Rent".equalsIgnoreCase(category)) {
            return "Track monthly dues";
        }
        if ("Groceries".equalsIgnoreCase(category)) {
            return "Track weekly spend";
        }
        if ("Electricity".equalsIgnoreCase(category)) {
            return "Bills and meter costs";
        }
        if ("Vegetables".equalsIgnoreCase(category)) {
            return "Fresh market spend";
        }
        if ("Travel".equalsIgnoreCase(category)) {
            return "Trips and rides";
        }
        return "Tap to manage entries";
    }

    public static String getIcon(String category) {
        if ("Milk".equalsIgnoreCase(category)) {
            return "\uD83E\uDD5B";
        }
        if ("Rent".equalsIgnoreCase(category)) {
            return "\uD83C\uDFE0";
        }
        if ("Groceries".equalsIgnoreCase(category)) {
            return "\uD83D\uDED2";
        }
        if ("Electricity".equalsIgnoreCase(category)) {
            return "\u26A1";
        }
        if ("Vegetables".equalsIgnoreCase(category)) {
            return "\uD83E\uDD6C";
        }
        if ("Travel".equalsIgnoreCase(category)) {
            return "\uD83D\uDE97";
        }
        return category == null || category.trim().isEmpty()
                ? "\u20B9"
                : category.trim().substring(0, 1).toUpperCase();
    }

    public static int getColorRes(String category) {
        if ("Rent".equalsIgnoreCase(category)) {
            return R.color.category_rent;
        }
        if ("Groceries".equalsIgnoreCase(category)) {
            return R.color.category_groceries;
        }
        if ("Electricity".equalsIgnoreCase(category)) {
            return R.color.category_electricity;
        }
        if ("Vegetables".equalsIgnoreCase(category)) {
            return R.color.category_vegetables;
        }
        if ("Travel".equalsIgnoreCase(category)) {
            return R.color.category_travel;
        }
        return R.color.category_default;
    }

    public static int getBackgroundRes(String category) {
        if ("Rent".equalsIgnoreCase(category)) {
            return R.color.category_rent_bg;
        }
        if ("Groceries".equalsIgnoreCase(category)) {
            return R.color.category_groceries_bg;
        }
        if ("Electricity".equalsIgnoreCase(category)) {
            return R.color.category_electricity_bg;
        }
        if ("Vegetables".equalsIgnoreCase(category)) {
            return R.color.category_vegetables_bg;
        }
        if ("Travel".equalsIgnoreCase(category)) {
            return R.color.category_travel_bg;
        }
        return R.color.category_default_bg;
    }
}
