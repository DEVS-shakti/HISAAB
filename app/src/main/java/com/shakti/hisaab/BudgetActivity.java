package com.shakti.hisaab;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.shakti.hisaab.database.entities.Budget;
import com.shakti.hisaab.database.entities.Expense;
import com.shakti.hisaab.viewmodel.BudgetViewModel;
import com.shakti.hisaab.viewmodel.ExpenseViewModel;

import java.time.YearMonth;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BudgetActivity extends AppCompatActivity {

    private BudgetViewModel budgetViewModel;
    private ExpenseViewModel expenseViewModel;
    private YearMonth selectedMonth = YearMonth.now();

    private TextView tvMonthHeader, tvSummaryMainAmount, tvSummaryStatusLabel;
    private TextView tvSummaryBudget, tvSummarySpent, tvSummaryUsedPercent;
    private ProgressBar progressBudget;
    private TextView tvDaysRemaining, tvAvgDailySpend, tvSafeDailySpend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        budgetViewModel = new ViewModelProvider(this).get(BudgetViewModel.class);
        expenseViewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        bindViews();
        setupToolbar();
        observeData();
    }

    private void bindViews() {
        tvMonthHeader = findViewById(R.id.tvMonthHeader);
        tvSummaryMainAmount = findViewById(R.id.tvSummaryMainAmount);
        tvSummaryStatusLabel = findViewById(R.id.tvSummaryStatusLabel);
        tvSummaryBudget = findViewById(R.id.tvSummaryBudget);
        tvSummarySpent = findViewById(R.id.tvSummarySpent);
        tvSummaryUsedPercent = findViewById(R.id.tvSummaryUsedPercent);
        progressBudget = findViewById(R.id.progressBudget);
        tvDaysRemaining = findViewById(R.id.tvDaysRemaining);
        tvAvgDailySpend = findViewById(R.id.tvAvgDailySpend);
        tvSafeDailySpend = findViewById(R.id.tvSafeDailySpend);

        findViewById(R.id.btnBudgetInsights).setOnClickListener(v -> 
            startActivity(new Intent(this, BudgetInsightsActivity.class)));
        findViewById(R.id.btnBudgetHistory).setOnClickListener(v -> 
            startActivity(new Intent(this, BudgetHistoryActivity.class)));
    }

    private void setupToolbar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnEditBudget).setOnClickListener(v -> openEditBudgetDialog());
    }

    private void observeData() {
        String monthName = selectedMonth.getMonth().name().substring(0, 1) + 
                selectedMonth.getMonth().name().substring(1).toLowerCase();
        tvMonthHeader.setText(monthName + " " + selectedMonth.getYear());

        budgetViewModel.getBudgetForMonth(selectedMonth.toString()).observe(this, budget -> {
            long start = getMonthStart(selectedMonth);
            long end = getMonthEnd(selectedMonth);
            expenseViewModel.getExpensesBetween(start, end).observe(this, expenses -> {
                updateUi(budget, expenses);
            });
        });
    }

    private void updateUi(Budget budget, List<Expense> expenses) {
        double spent = 0;
        if (expenses != null) {
            for (Expense e : expenses) {
                spent += e.amount;
            }
        }

        double totalBudget = (budget != null) ? budget.amount : 0;
        double remaining = totalBudget - spent;
        
        tvSummaryBudget.setText(AppPreferences.formatAmount(this, totalBudget));
        tvSummarySpent.setText(AppPreferences.formatAmount(this, spent));
        
        if (totalBudget > 0) {
            int percent = (int) ((spent / totalBudget) * 100);
            tvSummaryUsedPercent.setText(percent + "%");
            progressBudget.setProgress(Math.min(percent, 100));
        } else {
            tvSummaryUsedPercent.setText("0%");
            progressBudget.setProgress(0);
        }

        if (remaining < 0) {
            tvSummaryMainAmount.setText(AppPreferences.formatAmount(this, Math.abs(remaining)));
            tvSummaryStatusLabel.setText("Over Budget");
        } else {
            tvSummaryMainAmount.setText(AppPreferences.formatAmount(this, remaining));
            tvSummaryStatusLabel.setText("Left");
        }

        // Daily Insights
        Calendar cal = Calendar.getInstance();
        int today = cal.get(Calendar.DAY_OF_MONTH);
        int daysInMonth = selectedMonth.lengthOfMonth();
        int remainingDays = daysInMonth - today + 1;
        if (remainingDays < 1) remainingDays = 1;

        tvDaysRemaining.setText(String.valueOf(remainingDays));
        
        double avgSpent = (today > 0) ? spent / today : spent;
        tvAvgDailySpend.setText(AppPreferences.formatAmount(this, avgSpent));
        
        double safeDaily = (remaining > 0) ? remaining / remainingDays : 0;
        tvSafeDailySpend.setText(AppPreferences.formatAmount(this, safeDaily));
    }

    private void openEditBudgetDialog() {
        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter amount");
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Monthly Budget")
                .setMessage("Set budget for " + selectedMonth.toString())
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String val = input.getText().toString().trim();
                    if (!val.isEmpty()) {
                        try {
                            double amount = Double.parseDouble(val);
                            budgetViewModel.setBudget(selectedMonth.toString(), selectedMonth.getYear(), amount);
                            Toast.makeText(this, "Budget updated", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private long getMonthStart(YearMonth month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, month.getYear());
        calendar.set(Calendar.MONTH, month.getMonthValue() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getMonthEnd(YearMonth month) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getMonthStart(month));
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        return calendar.getTimeInMillis();
    }
}
