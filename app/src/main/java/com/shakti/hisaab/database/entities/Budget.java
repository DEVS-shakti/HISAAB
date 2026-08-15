package com.shakti.hisaab.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgets")
public class Budget {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String month; // Format: YYYY-MM
    public int year;
    public double amount;
    public long createdAt;
    public long updatedAt;

    public Budget(String month, int year, double amount, long createdAt, long updatedAt) {
        this.month = month;
        this.year = year;
        this.amount = amount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
