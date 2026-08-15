package com.shakti.hisaab.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.shakti.hisaab.R;

public class HeroPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_EXPENSE = 0;
    private static final int TYPE_BUDGET = 1;

    public interface OnHeroActionListener {
        void onSeeReports();
        void onManageBudget();
    }

    private final OnHeroActionListener listener;
    
    // Expense data
    private String expenseLabel = "";
    private String expenseAmount = "";
    private String unpaidDues = "";

    // Budget data
    private String budgetLabel = "";
    private String budgetMainAmount = "";
    private String budgetStatusLabel = "LEFT";
    private String budgetSpent = "";
    private String budgetTotal = "";
    private boolean isOverBudget = false;

    public HeroPagerAdapter(OnHeroActionListener listener) {
        this.listener = listener;
    }

    public void setExpenseData(String label, String amount, String unpaid) {
        this.expenseLabel = label;
        this.expenseAmount = amount;
        this.unpaidDues = unpaid;
        notifyItemChanged(0);
    }

    public void setBudgetData(String label, String mainAmount, String statusLabel, String spent, String total, boolean overBudget) {
        this.budgetLabel = label;
        this.budgetMainAmount = mainAmount;
        this.budgetStatusLabel = statusLabel;
        this.budgetSpent = spent;
        this.budgetTotal = total;
        this.isOverBudget = overBudget;
        notifyItemChanged(1);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_EXPENSE : TYPE_BUDGET;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_EXPENSE) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hero_expense, parent, false);
            return new ExpenseViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hero_budget, parent, false);
            return new BudgetViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_EXPENSE) {
            ExpenseViewHolder vh = (ExpenseViewHolder) holder;
            vh.tvLabel.setText(expenseLabel);
            vh.tvAmount.setText(expenseAmount);
            vh.tvUnpaid.setText(unpaidDues);
            vh.btnReports.setOnClickListener(v -> listener.onSeeReports());
        } else {
            BudgetViewHolder vh = (BudgetViewHolder) holder;
            vh.tvLabel.setText(budgetLabel);
            vh.tvAmount.setText(budgetMainAmount);
            vh.tvStatusLabel.setText(budgetStatusLabel);
            vh.tvSpent.setText(budgetSpent);
            vh.tvTotal.setText(budgetTotal);
            vh.btnManage.setOnClickListener(v -> listener.onManageBudget());
            
            if (isOverBudget) {
                vh.dot.setBackgroundResource(R.drawable.dot_circle); // Existing indicator
                // Typography changes are handled via budgetStatusLabel text "OVER BUDGET"
            } else {
                vh.dot.setBackgroundResource(R.drawable.dot_circle);
            }
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvLabel, tvAmount, tvUnpaid, btnReports;
        ExpenseViewHolder(View itemView) {
            super(itemView);
            tvLabel = itemView.findViewById(R.id.tvHeroLabel);
            tvAmount = itemView.findViewById(R.id.tvHeroAmount);
            tvUnpaid = itemView.findViewById(R.id.tvUnpaidDues);
            btnReports = itemView.findViewById(R.id.tvSeeReports);
        }
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView tvLabel, tvAmount, tvStatusLabel, tvSpent, tvTotal, btnManage;
        View dot;
        BudgetViewHolder(View itemView) {
            super(itemView);
            tvLabel = itemView.findViewById(R.id.tvBudgetLabel);
            tvAmount = itemView.findViewById(R.id.tvBudgetAmount);
            tvStatusLabel = itemView.findViewById(R.id.tvBudgetStatusLabel);
            tvSpent = itemView.findViewById(R.id.tvBudgetSpent);
            tvTotal = itemView.findViewById(R.id.tvBudgetTotal);
            btnManage = itemView.findViewById(R.id.tvManageBudget);
            dot = itemView.findViewById(R.id.viewBudgetStatusDot);
        }
    }
}
