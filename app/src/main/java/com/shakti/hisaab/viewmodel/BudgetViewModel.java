package com.shakti.hisaab.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.shakti.hisaab.database.entities.Budget;
import com.shakti.hisaab.database.entities.BudgetRevision;
import com.shakti.hisaab.database.repository.BudgetRepository;
import java.util.List;

public class BudgetViewModel extends AndroidViewModel {
    private final BudgetRepository repository;

    public BudgetViewModel(@NonNull Application application) {
        super(application);
        repository = new BudgetRepository(application);
    }

    public LiveData<Budget> getBudgetForMonth(String month) {
        return repository.getBudgetForMonth(month);
    }

    public LiveData<List<Budget>> getAllBudgets() {
        return repository.getAllBudgets();
    }

    public void setBudget(String month, int year, double amount) {
        repository.setBudget(month, year, amount);
    }

    public LiveData<List<BudgetRevision>> getRevisionsForBudget(int budgetId) {
        return repository.getRevisionsForBudget(budgetId);
    }
}
