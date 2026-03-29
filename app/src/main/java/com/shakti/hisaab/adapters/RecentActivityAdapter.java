package com.shakti.hisaab.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.shakti.hisaab.AppPreferences;
import com.shakti.hisaab.CategoryDisplayHelper;
import com.shakti.hisaab.R;
import com.shakti.hisaab.database.entities.Expense;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.RecentViewHolder> {

    public interface OnRecentClickListener {
        void onRecentClick(Expense expense);
    }

    private final List<Expense> items = new ArrayList<>();
    private final OnRecentClickListener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("dd MMM  h:mm a", Locale.getDefault());

    public RecentActivityAdapter(OnRecentClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Expense> expenses) {
        items.clear();
        if (expenses != null) {
            items.addAll(expenses);
        }
        notifyDataSetChanged();
    }

    public void refresh() {
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_activity, parent, false);
        return new RecentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentViewHolder holder, int position) {
        Expense expense = items.get(position);
        holder.tvTitle.setText(expense.note == null || expense.note.trim().isEmpty()
                ? expense.category
                : expense.note.trim());
        holder.tvTime.setText(timeFormat.format(new Date(expense.dateMillis)));
        holder.tvAmount.setText(AppPreferences.formatAmount(holder.itemView.getContext(), expense.amount));
        holder.tvIcon.setText(CategoryDisplayHelper.getIcon(expense.category));

        int backgroundRes = CategoryDisplayHelper.getBackgroundRes(expense.category);
        int textColorRes = CategoryDisplayHelper.getColorRes(expense.category);
        holder.tvIcon.setBackgroundTintList(
                ContextCompat.getColorStateList(holder.itemView.getContext(), backgroundRes)
        );
        holder.tvIcon.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), textColorRes));

        holder.itemView.setOnClickListener(v -> listener.onRecentClick(expense));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    static class RecentViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon;
        TextView tvTitle;
        TextView tvTime;
        TextView tvAmount;

        RecentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvRecentIcon);
            tvTitle = itemView.findViewById(R.id.tvRecentTitle);
            tvTime = itemView.findViewById(R.id.tvRecentTime);
            tvAmount = itemView.findViewById(R.id.tvRecentAmount);
        }
    }
}
