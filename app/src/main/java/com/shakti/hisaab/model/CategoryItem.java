package com.shakti.hisaab.model;

public class CategoryItem {
    public String name;
    public String subtitle;
    public String icon;
    public int iconColorRes;
    public int iconBackgroundRes;

    public CategoryItem(String name, String subtitle, String icon, int iconColorRes, int iconBackgroundRes) {
        this.name = name;
        this.subtitle = subtitle;
        this.icon = icon;
        this.iconColorRes = iconColorRes;
        this.iconBackgroundRes = iconBackgroundRes;
    }
}
