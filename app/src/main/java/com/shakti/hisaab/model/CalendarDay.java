package com.shakti.hisaab.model;

public class CalendarDay {
    public enum State {
        EMPTY,
        FUTURE,
        NO_ENTRY,
        PAID,
        UNPAID,
        NOT_TAKEN
    }

    public final String dateKey;
    public final int dayOfMonth;
    public final boolean isToday;
    public final State state;
    public final Double quantity;

    public CalendarDay(String dateKey, int dayOfMonth, boolean isToday, State state, Double quantity) {
        this.dateKey = dateKey;
        this.dayOfMonth = dayOfMonth;
        this.isToday = isToday;
        this.state = state;
        this.quantity = quantity;
    }
}
