package com.shakti.hisaab.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budget_revisions")
public class BudgetRevision {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int budgetId;
    public double oldAmount;
    public double newAmount;
    public long changedAt;

    public BudgetRevision(int budgetId, double oldAmount, double newAmount, long changedAt) {
        this.budgetId = budgetId;
        this.oldAmount = oldAmount;
        this.newAmount = newAmount;
        this.changedAt = changedAt;
    }
}
