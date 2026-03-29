package com.shakti.hisaab.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses")
public class Expense {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String category;

    public double amount;

    public long dateMillis;

    public String note;

    public boolean isPaid;

    public Expense(@NonNull String category, double amount, long dateMillis, String note, boolean isPaid) {
        this.category = category;
        this.amount = amount;
        this.dateMillis = dateMillis;
        this.note = note;
        this.isPaid = isPaid;
    }
}
