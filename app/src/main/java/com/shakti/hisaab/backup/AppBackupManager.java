package com.shakti.hisaab.backup;

import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.shakti.hisaab.AppPreferences;
import com.shakti.hisaab.database.AppDatabase;
import com.shakti.hisaab.database.entities.Budget;
import com.shakti.hisaab.database.entities.BudgetRevision;
import com.shakti.hisaab.database.entities.Expense;
import com.shakti.hisaab.database.entities.MilkEntry;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class AppBackupManager {

    public static void exportBackup(Context context, Uri uri) throws Exception {
        AppDatabase db = AppDatabase.getInstance(context);
        BackupModel model = new BackupModel();
        
        model.expenses = db.expenseDao().getAllExpensesSync();
        model.milkEntries = db.milkEntryDao().getAllEntries();
        model.budgets = db.budgetDao().getAllBudgetsSync();
        model.budgetRevisions = db.budgetDao().getAllRevisionsSync();
        
        model.categories = AppPreferences.getCategories(context);
        model.currencyCode = AppPreferences.getCurrencyCode(context);

        try (OutputStreamWriter writer = new OutputStreamWriter(
                context.getContentResolver().openOutputStream(uri), StandardCharsets.UTF_8)) {
            new Gson().toJson(model, writer);
        }
    }

    public static void restoreBackup(Context context, Uri uri) throws Exception {
        BackupModel model;
        try (InputStreamReader reader = new InputStreamReader(
                context.getContentResolver().openInputStream(uri), StandardCharsets.UTF_8)) {
            model = new Gson().fromJson(reader, BackupModel.class);
        }

        if (model == null) throw new Exception("Invalid backup file");

        AppDatabase db = AppDatabase.getInstance(context);
        db.runInTransaction(() -> {
            db.expenseDao().deleteAll();
            db.milkEntryDao().deleteAll();
            db.budgetDao().deleteAll();
            db.budgetDao().deleteAllRevisions();
            
            if (model.expenses != null) {
                for (Expense e : model.expenses) db.expenseDao().insert(e);
            }
            if (model.milkEntries != null) {
                for (MilkEntry m : model.milkEntries) db.milkEntryDao().insert(m);
            }
            if (model.budgets != null) {
                for (Budget b : model.budgets) db.budgetDao().insert(b);
            }
            if (model.budgetRevisions != null) {
                for (BudgetRevision br : model.budgetRevisions) db.budgetDao().insertRevision(br);
            }
        });

        if (model.categories != null) {
            AppPreferences.saveCategories(context, model.categories);
        }
        if (model.currencyCode != null) {
            AppPreferences.setCurrencyCode(context, model.currencyCode);
        }
    }
}
