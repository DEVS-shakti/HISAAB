package com.shakti.hisaab.database.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.shakti.hisaab.database.AppDatabase;
import com.shakti.hisaab.database.dao.BudgetDao;
import com.shakti.hisaab.database.entities.Budget;
import com.shakti.hisaab.database.entities.BudgetRevision;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BudgetRepository {
    private final BudgetDao budgetDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public BudgetRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        budgetDao = db.budgetDao();
    }

    public LiveData<Budget> getBudgetForMonth(String month) {
        return budgetDao.getBudgetForMonth(month);
    }

    public LiveData<List<Budget>> getAllBudgets() {
        return budgetDao.getAllBudgets();
    }

    public void setBudget(String month, int year, double amount) {
        executor.execute(() -> {
            Budget existing = budgetDao.getBudgetForMonthSync(month);
            long now = System.currentTimeMillis();
            if (existing != null) {
                if (existing.amount != amount) {
                    budgetDao.insertRevision(new BudgetRevision(existing.id, existing.amount, amount, now));
                    existing.amount = amount;
                    existing.updatedAt = now;
                    budgetDao.update(existing);
                }
            } else {
                long id = budgetDao.insert(new Budget(month, year, amount, now, now));
                budgetDao.insertRevision(new BudgetRevision((int) id, 0, amount, now));
            }
        });
    }

    public LiveData<List<BudgetRevision>> getRevisionsForBudget(int budgetId) {
        return budgetDao.getRevisionsForBudget(budgetId);
    }
}
