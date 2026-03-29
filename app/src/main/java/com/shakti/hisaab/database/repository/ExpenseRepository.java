package com.shakti.hisaab.database.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.shakti.hisaab.database.AppDatabase;
import com.shakti.hisaab.database.dao.ExpenseDao;
import com.shakti.hisaab.database.entities.Expense;
import com.shakti.hisaab.model.CategoryTotal;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpenseRepository {
    private final ExpenseDao expenseDao;
    private final ExecutorService executorService;

    public ExpenseRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        expenseDao = db.expenseDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(Expense expense) {
        // Writes run off the main thread
        executorService.execute(() -> expenseDao.insert(expense));
    }

    public void update(Expense expense) {
        executorService.execute(() -> expenseDao.update(expense));
    }

    public void delete(int id) {
        // Writes run off the main thread
        executorService.execute(() -> expenseDao.delete(id));
    }

    public void deleteAll() {
        executorService.execute(expenseDao::deleteAll);
    }

    public void deleteByCategory(String category) {
        executorService.execute(() -> expenseDao.deleteByCategory(category));
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

    public LiveData<List<Expense>> getRecentExpenses(int limit) {
        return expenseDao.getRecentExpenses(limit);
    }

    public LiveData<Double> getSumByCategory(String category) {
        return expenseDao.getSumByCategory(category);
    }

    public LiveData<Integer> getUnpaidCountByCategory(String category) {
        return expenseDao.getUnpaidCountByCategory(category);
    }

    public List<Expense> getUnpaidExpensesSync() {
        return expenseDao.getUnpaidExpenses();
    }

    public LiveData<Double> getTotalSum() {
        return expenseDao.getTotalSum();
    }

    public LiveData<Double> getTotalUnpaidSum() {
        return expenseDao.getTotalUnpaidSum();
    }

    public LiveData<List<CategoryTotal>> getCategoryTotalsForMonth(long startMillis, long endMillis) {
        return expenseDao.getCategoryTotalsForMonth(startMillis, endMillis);
    }
}
