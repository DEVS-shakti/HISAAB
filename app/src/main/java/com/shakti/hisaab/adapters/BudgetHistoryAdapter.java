package com.shakti.hisaab.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.shakti.hisaab.AppPreferences;
import com.shakti.hisaab.R;
import com.shakti.hisaab.database.entities.Budget;
import com.shakti.hisaab.database.entities.Expense;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BudgetHistoryAdapter extends RecyclerView.Adapter<BudgetHistoryAdapter.ViewHolder> {

    private final List<Budget> budgets = new ArrayList<>();
    private final Map<String, List<Expense>> expensesByMonth;

    public BudgetHistoryAdapter(Map<String, List<Expense>> expensesByMonth) {
        this.expensesByMonth = expensesByMonth;
    }

    public void setBudgets(List<Budget> budgetList) {
        budgets.clear();
        if (budgetList != null) {
            budgets.addAll(budgetList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Budget budget = budgets.get(position);
        
        YearMonth ym = YearMonth.parse(budget.month);
        String monthName = ym.getMonth().name().substring(0, 1) + ym.getMonth().name().substring(1).toLowerCase();
        holder.tvMonth.setText(monthName + " " + ym.getYear());
        
        holder.tvBudget.setText(AppPreferences.formatAmount(holder.itemView.getContext(), budget.amount));
        
        double spent = 0;
        List<Expense> expenses = expensesByMonth.get(budget.month);
        if (expenses != null) {
            for (Expense e : expenses) {
                spent += e.amount;
            }
        }
        holder.tvSpent.setText(AppPreferences.formatAmount(holder.itemView.getContext(), spent));
        
        if (spent > budget.amount) {
            holder.tvStatus.setText("OVER BUDGET");
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_unpaid));
        } else {
            holder.tvStatus.setText("UNDER BUDGET");
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_paid));
        }
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonth, tvBudget, tvSpent, tvStatus;
        ViewHolder(View itemView) {
            super(itemView);
            tvMonth = itemView.findViewById(R.id.tvHistoryMonth);
            tvBudget = itemView.findViewById(R.id.tvHistoryBudget);
            tvSpent = itemView.findViewById(R.id.tvHistorySpent);
            tvStatus = itemView.findViewById(R.id.tvHistoryStatus);
        }
    }
}
