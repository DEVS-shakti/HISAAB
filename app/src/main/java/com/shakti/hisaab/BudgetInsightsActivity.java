package com.shakti.hisaab;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.shakti.hisaab.database.entities.Budget;
import com.shakti.hisaab.database.entities.Expense;
import com.shakti.hisaab.viewmodel.BudgetViewModel;
import com.shakti.hisaab.viewmodel.ExpenseViewModel;

import java.time.YearMonth;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetInsightsActivity extends AppCompatActivity {

    private BudgetViewModel budgetViewModel;
    private ExpenseViewModel expenseViewModel;
    private LinearLayout layoutInsightsContainer;
    private YearMonth currentMonth = YearMonth.now();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_insights);

        budgetViewModel = new ViewModelProvider(this).get(BudgetViewModel.class);
        expenseViewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        layoutInsightsContainer = findViewById(R.id.layoutInsightsContainer);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        observeData();
    }

    private void observeData() {
        budgetViewModel.getBudgetForMonth(currentMonth.toString()).observe(this, budget -> {
            long start = getMonthStart(currentMonth);
            long end = getMonthEnd(currentMonth);
            expenseViewModel.getExpensesBetween(start, end).observe(this, expenses -> {
                YearMonth prevMonth = currentMonth.minusMonths(1);
                budgetViewModel.getBudgetForMonth(prevMonth.toString()).observe(this, prevBudget -> {
                    long pStart = getMonthStart(prevMonth);
                    long pEnd = getMonthEnd(prevMonth);
                    expenseViewModel.getExpensesBetween(pStart, pEnd).observe(this, prevExpenses -> {
                        renderInsights(budget, expenses, prevBudget, prevExpenses);
                    });
                });
            });
        });
    }

    private void renderInsights(Budget budget, List<Expense> expenses, Budget prevBudget, List<Expense> prevExpenses) {
        layoutInsightsContainer.removeAllViews();
        if (budget == null || budget.amount <= 0) {
            addSectionHeader("No budget set for " + currentMonth.toString());
            return;
        }

        double spent = 0;
        Map<String, Double> categoryMap = new HashMap<>();
        for (Expense e : expenses) {
            spent += e.amount;
            categoryMap.put(e.category, categoryMap.getOrDefault(e.category, 0.0) + e.amount);
        }

        double remaining = budget.amount - spent;
        int usedPercent = (int) ((spent / budget.amount) * 100);

        addSectionHeader("Summary");
        addInsightRow("Total Budget", AppPreferences.formatAmount(this, budget.amount));
        addInsightRow("Total Spent", AppPreferences.formatAmount(this, spent));
        addInsightRow(remaining < 0 ? "Over Budget" : "Remaining", AppPreferences.formatAmount(this, Math.abs(remaining)));
        addInsightRow("Budget Used", usedPercent + "%");

        addSectionHeader("Daily Tracking");
        Calendar cal = Calendar.getInstance();
        int today = cal.get(Calendar.DAY_OF_MONTH);
        int daysInMonth = currentMonth.lengthOfMonth();
        int remainingDays = daysInMonth - today + 1;
        if (remainingDays < 1) remainingDays = 1;

        double avgDaily = spent / today;
        double safeDaily = (remaining > 0) ? remaining / remainingDays : 0;
        double projected = avgDaily * daysInMonth;

        addInsightRow("Average Daily Spend", AppPreferences.formatAmount(this, avgDaily));
        addInsightRow("Safe Daily Spend", AppPreferences.formatAmount(this, safeDaily));
        addInsightRow("Projected Month End", AppPreferences.formatAmount(this, projected));
        
        String projectionMessage;
        if (projected > budget.amount) {
            projectionMessage = "On track to exceed by " + AppPreferences.formatAmount(this, projected - budget.amount);
        } else {
            projectionMessage = "On track to save " + AppPreferences.formatAmount(this, budget.amount - projected);
        }
        addInsightRow("Projection Status", projectionMessage);

        addSectionHeader("Top Category");
        String topCat = "None";
        double maxCat = 0;
        for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
            if (entry.getValue() > maxCat) {
                maxCat = entry.getValue();
                topCat = entry.getKey();
            }
        }
        addInsightRow("Highest Category", topCat + " (" + AppPreferences.formatAmount(this, maxCat) + ")");

        if (prevExpenses != null && !prevExpenses.isEmpty()) {
            addSectionHeader("Month Comparison");
            double pSpent = 0;
            for (Expense e : prevExpenses) pSpent += e.amount;
            
            double diff = spent - pSpent;
            double diffPercent = (pSpent > 0) ? (diff / pSpent) * 100 : 0;
            
            String compMessage = String.format(Locale.getDefault(), "%.1f%% %s than last month", 
                    Math.abs(diffPercent), diff > 0 ? "higher" : "lower");
            addInsightRow("Spending Trend", compMessage);
        }
    }

    private void addSectionHeader(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextSize(14);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(ContextCompat.getColor(this, R.color.primary));
        int padTop = (int) (24 * getResources().getDisplayMetrics().density);
        int padBottom = (int) (12 * getResources().getDisplayMetrics().density);
        tv.setPadding(0, padTop, 0, padBottom);
        layoutInsightsContainer.addView(tv);
    }

    private void addInsightRow(String label, String value) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_budget_insight, layoutInsightsContainer, false);
        ((TextView) row.findViewById(R.id.tvInsightLabel)).setText(label);
        ((TextView) row.findViewById(R.id.tvInsightValue)).setText(value);
        layoutInsightsContainer.addView(row);
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
