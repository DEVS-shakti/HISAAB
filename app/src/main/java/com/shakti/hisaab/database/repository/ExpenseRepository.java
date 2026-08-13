package com.shakti.hisaab.database.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.shakti.hisaab.database.AppDatabase;
import com.shakti.hisaab.database.dao.ExpenseDao;
import com.shakti.hisaab.database.dao.MilkEntryDao;
import com.shakti.hisaab.database.entities.Expense;
import com.shakti.hisaab.database.entities.MilkEntry;
import com.shakti.hisaab.model.CategoryTotal;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpenseRepository {
    private final ExpenseDao expenseDao;
    private final MilkEntryDao milkEntryDao;
    private final ExecutorService executorService;

    public ExpenseRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        expenseDao = db.expenseDao();
        milkEntryDao = db.milkEntryDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(Expense expense) {
        executorService.execute(() -> expenseDao.insert(expense));
    }

    public void update(Expense expense) {
        executorService.execute(() -> expenseDao.update(expense));
    }

    public void delete(int id) {
        executorService.execute(() -> expenseDao.delete(id));
    }

    public void deleteAll() {
        executorService.execute(() -> {
            expenseDao.deleteAll();
            milkEntryDao.deleteAll();
        });
    }

    public void deleteByCategory(String category) {
        if ("Milk".equalsIgnoreCase(category)) {
            executorService.execute(milkEntryDao::deleteAll);
        } else {
            executorService.execute(() -> expenseDao.deleteByCategory(category));
        }
    }

    public void renameCategory(String oldCategory, String newCategory) {
        executorService.execute(() -> expenseDao.renameCategory(oldCategory, newCategory));
    }

    public LiveData<List<Expense>> getExpensesByCategory(String category) {
        return expenseDao.getExpensesByCategory(category);
    }

    public LiveData<List<Expense>> getExpensesBetween(long startMillis, long endMillis) {
        return expenseDao.getExpensesBetween(startMillis, endMillis);
    }

    public LiveData<List<MilkEntry>> getMilkEntriesForMonth(String monthPrefix) {
        return milkEntryDao.getEntriesForMonth(monthPrefix);
    }

    public LiveData<List<Expense>> getRecentExpenses(int limit) {
        return expenseDao.getRecentExpenses(limit);
    }

    public LiveData<Double> getSumByCategory(String category) {
        return expenseDao.getSumByCategory(category);
    }

    public LiveData<Integer> getUnpaidCountByCategory(String category) {
        return expenseDao.getUnpaidCountByCategory(category);
    }

    public LiveData<Double> getTotalSum() {
        return expenseDao.getTotalSum();
    }

    public LiveData<Double> getTotalUnpaidSum() {
        return expenseDao.getTotalUnpaidSum();
    }

    public LiveData<Double> getMilkUnpaidSum() {
        return milkEntryDao.getTotalUnpaidMilkSum();
    }

    public LiveData<Double> getCombinedUnpaidSum() {
        MediatorLiveData<Double> combined = new MediatorLiveData<>();
        LiveData<Double> expenseUnpaid = expenseDao.getTotalUnpaidSum();
        LiveData<Double> milkUnpaid = milkEntryDao.getTotalUnpaidMilkSum();

        combined.addSource(expenseUnpaid, val -> combine(combined, expenseUnpaid, milkUnpaid));
        combined.addSource(milkUnpaid, val -> combine(combined, expenseUnpaid, milkUnpaid));
        return combined;
    }

    private void combine(MediatorLiveData<Double> target, LiveData<Double> source1, LiveData<Double> source2) {
        double val1 = source1.getValue() != null ? source1.getValue() : 0;
        double val2 = source2.getValue() != null ? source2.getValue() : 0;
        target.setValue(val1 + val2);
    }

    public LiveData<List<CategoryTotal>> getCategoryTotalsForMonth(long startMillis, long endMillis) {
        return expenseDao.getCategoryTotalsForMonth(startMillis, endMillis);
    }
}
