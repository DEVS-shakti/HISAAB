package com.shakti.hisaab.backup;

import com.shakti.hisaab.database.entities.Budget;
import com.shakti.hisaab.database.entities.BudgetRevision;
import com.shakti.hisaab.database.entities.Expense;
import com.shakti.hisaab.database.entities.MilkEntry;

import java.util.List;

public class BackupModel {
    public int version = 3;
    public List<Expense> expenses;
    public List<MilkEntry> milkEntries;
    public List<Budget> budgets;
    public List<BudgetRevision> budgetRevisions;
    public List<String> categories;
    public String currencyCode;
}
