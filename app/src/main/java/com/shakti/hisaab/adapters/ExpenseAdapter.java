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
import com.shakti.hisaab.database.entities.Expense;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    private final List<Expense> items = new ArrayList<>();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final OnExpenseClickListener listener;

    public ExpenseAdapter(OnExpenseClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Expense> expenses) {
        items.clear();
        if (expenses != null) {
            items.addAll(expenses);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = items.get(position);
        holder.tvNote.setText(expense.note == null || expense.note.trim().isEmpty() ? "-" : expense.note.trim());
        holder.tvAmount.setText(AppPreferences.formatAmount(holder.itemView.getContext(), expense.amount));
        holder.tvDate.setText(dateFormat.format(new Date(expense.dateMillis)));

        if (expense.isPaid) {
            holder.tvStatus.setText("Paid");
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_paid));
            holder.tvStatus.setBackgroundTintList(
                    ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.status_paid_bg));
        } else {
            holder.tvStatus.setText("Unpaid");
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_unpaid));
            holder.tvStatus.setBackgroundTintList(
                    ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.status_unpaid_bg));
        }

        holder.itemView.setOnClickListener(v -> listener.onExpenseClick(expense));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvNote;
        TextView tvAmount;
        TextView tvStatus;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvExpenseDate);
            tvNote = itemView.findViewById(R.id.tvExpenseNote);
            tvAmount = itemView.findViewById(R.id.tvExpenseAmount);
            tvStatus = itemView.findViewById(R.id.tvExpenseStatus);
        }
    }
}
