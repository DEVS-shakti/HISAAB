package com.shakti.hisaab;

public class CategoryDisplayHelper {
    public static String getIcon(String category) {
        if (category == null) return "❓";
        switch (category.toLowerCase()) {
            case "milk": return "🥛";
            case "food": return "🍕";
            case "transport": return "🚗";
            case "shopping": return "🛍️";
            case "entertainment": return "🎭";
            case "bills": return "💳";
            case "groceries": return "🛒";
            case "electricity": return "⚡";
            case "travel": return "🚗";
            case "vegetables": return "🥬";
            case "rent": return "🏠";
            case "health": return "🏥";
            case "education": return "🎓";
            case "salary": return "💰";
            case "gift": return "🎁";
            default: return "📦";
        }
    }

    public static int getColorRes(String category) {
        if (category == null) return R.color.category_default;
        switch (category.toLowerCase()) {
            case "rent": return R.color.category_rent;
            case "electricity": return R.color.category_electricity;
            case "groceries": return R.color.category_groceries;
            case "travel":
            case "transport": return R.color.category_travel;
            case "vegetables": return R.color.category_vegetables;
            case "food": return R.color.category_food;
            case "shopping": return R.color.category_shopping;
            case "bills": return R.color.category_bills;
            case "entertainment": return R.color.category_entertainment;
            case "health": return R.color.category_health;
            case "education": return R.color.category_education;
            case "salary": return R.color.category_salary;
            case "gift": return R.color.category_gift;
            case "milk": return R.color.category_milk;
            default: return R.color.category_default;
        }
    }

    public static int getBackgroundRes(String category) {
        if (category == null) return R.color.category_default_bg;
        switch (category.toLowerCase()) {
            case "rent": return R.color.category_rent_bg;
            case "electricity": return R.color.category_electricity_bg;
            case "groceries": return R.color.category_groceries_bg;
            case "travel":
            case "transport": return R.color.category_travel_bg;
            case "vegetables": return R.color.category_vegetables_bg;
            case "food": return R.color.category_food_bg;
            case "shopping": return R.color.category_shopping_bg;
            case "bills": return R.color.category_bills_bg;
            case "entertainment": return R.color.category_entertainment_bg;
            case "health": return R.color.category_health_bg;
            case "education": return R.color.category_education_bg;
            case "salary": return R.color.category_salary_bg;
            case "gift": return R.color.category_gift_bg;
            case "milk": return R.color.category_milk_bg;
            default: return R.color.category_default_bg;
        }
    }

    public static String getSubtitle(String category) {
        if (category == null) return "Expense category";
        switch (category.toLowerCase()) {
            case "milk": return "Updated today";
            case "rent": return "Track monthly dues";
            case "groceries": return "Track weekly spend";
            case "electricity": return "Bills and meter costs";
            case "vegetables": return "Fresh market spend";
            case "travel": return "Trips and rides";
            case "food": return "Dining and snacks";
            case "shopping": return "Clothing and gear";
            case "health": return "Medical expenses";
            case "education": return "Courses and books";
            case "salary": return "Monthly income";
            case "gift": return "Occasions and joy";
            default: return "Category tracking";
        }
    }
}
