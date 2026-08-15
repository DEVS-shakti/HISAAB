package com.shakti.hisaab.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.shakti.hisaab.database.dao.BudgetDao;
import com.shakti.hisaab.database.dao.ExpenseDao;
import com.shakti.hisaab.database.dao.MilkEntryDao;
import com.shakti.hisaab.database.entities.Budget;
import com.shakti.hisaab.database.entities.BudgetRevision;
import com.shakti.hisaab.database.entities.Expense;
import com.shakti.hisaab.database.entities.MilkEntry;

@Database(entities = {MilkEntry.class, Expense.class, Budget.class, BudgetRevision.class}, version = 5)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    public abstract MilkEntryDao milkEntryDao();
    public abstract ExpenseDao expenseDao();
    public abstract BudgetDao budgetDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "hisaab_database"
            ).fallbackToDestructiveMigration().build();
        }

        return instance;
    }
}
