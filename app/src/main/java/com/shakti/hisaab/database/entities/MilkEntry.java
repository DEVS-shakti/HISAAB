package com.shakti.hisaab.database.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName ="milk_entries", indices = {@Index(value = "date", unique = true)})
public class MilkEntry {
    @PrimaryKey(autoGenerate = true)
     public int id;

    public String date;
    public boolean taken;
    public boolean paid;
    public double quantity;
    public double pricePerLiter;
    public double totalCost;

    public MilkEntry(String date, boolean taken, boolean paid, double quantity, double pricePerLiter, double totalCost) {
        this.date = date;
        this.taken = taken;
        this.paid = paid;
        this.quantity = quantity;
        this.pricePerLiter = pricePerLiter;
        this.totalCost = totalCost;
    }
}
