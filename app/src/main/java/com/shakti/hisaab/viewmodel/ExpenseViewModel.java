package com.shakti.hisaab.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.shakti.hisaab.database.entities.Expense;
import com.shakti.hisaab.database.repository.ExpenseRepository;
import com.shakti.hisaab.model.CategoryTotal;

import java.util.List;

public class ExpenseViewModel extends AndroidViewModel {
    private final ExpenseRepository repository;

    public ExpenseViewModel(@NonNull Application application) {
        super(application);
        repository = new ExpenseRepository(application);
    }

    public void insert(Expense expense) {
        repository.insert(expense);
    }

    public void update(Expense expense) {
        repository.update(expense);
    }

    public void delete(int id) {
        repository.delete(id);
    }

    public void deleteAll() {
        repository.deleteAll();
    }

    public void deleteByCategory(String category) {
        repository.deleteByCategory(category);
    }

    public void renameCategory(String oldCategory, String newCategory) {
        repository.renameCategory(oldCategory, newCategory);
    }

    public LiveData<List<Expense>> getExpensesByCategory(String category) {
        return repository.getExpensesByCategory(category);
    }

    public LiveData<List<Expense>> getExpensesBetween(long startMillis, long endMillis) {
        return repository.getExpensesBetween(startMillis, endMillis);
    }

    public LiveData<List<Expense>> getRecentExpenses(int limit) {
        return repository.getRecentExpenses(limit);
    }

    public LiveData<Double> getSumByCategory(String category) {
        return repository.getSumByCategory(category);
    }

    public LiveData<Integer> getUnpaidCountByCategory(String category) {
        return repository.getUnpaidCountByCategory(category);
    }

    public LiveData<Double> getTotalSum() {
        return repository.getTotalSum();
    }

    public LiveData<Double> getTotalUnpaidSum() {
        return repository.getTotalUnpaidSum();
    }

    public LiveData<List<CategoryTotal>> getCategoryTotalsForMonth(long startMillis, long endMillis) {
        return repository.getCategoryTotalsForMonth(startMillis, endMillis);
    }
}
