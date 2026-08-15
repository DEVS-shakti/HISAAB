package com.shakti.hisaab;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.shakti.hisaab.adapters.BudgetHistoryAdapter;
import com.shakti.hisaab.database.entities.Expense;
import com.shakti.hisaab.viewmodel.BudgetViewModel;
import com.shakti.hisaab.viewmodel.ExpenseViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetHistoryActivity extends AppCompatActivity {

    private BudgetViewModel budgetViewModel;
    private ExpenseViewModel expenseViewModel;
    private BudgetHistoryAdapter adapter;
    private final Map<String, List<Expense>> expensesByMonth = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_history);

        budgetViewModel = new ViewModelProvider(this).get(BudgetViewModel.class);
        expenseViewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.rvBudgetHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BudgetHistoryAdapter(expensesByMonth);
        recyclerView.setAdapter(adapter);

        observeData();
    }

    private void observeData() {
        expenseViewModel.getAllExpenses().observe(this, expenses -> {
            expensesByMonth.clear();
            if (expenses != null) {
                for (Expense e : expenses) {
                    String month = getMonthKey(e.dateMillis);
                    if (!expensesByMonth.containsKey(month)) {
                        expensesByMonth.put(month, new ArrayList<>());
                    }
                    expensesByMonth.get(month).add(e);
                }
            }
            // After expenses are mapped, observe budgets
            budgetViewModel.getAllBudgets().observe(this, budgets -> {
                adapter.setBudgets(budgets);
            });
        });
    }

    private String getMonthKey(long dateMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateMillis);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        return String.format(Locale.getDefault(), "%d-%02d", year, month);
    }
}
