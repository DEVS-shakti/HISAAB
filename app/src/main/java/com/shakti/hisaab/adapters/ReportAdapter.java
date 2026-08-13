package com.shakti.hisaab.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shakti.hisaab.AppPreferences;
import com.shakti.hisaab.R;
import com.shakti.hisaab.model.CategoryTotal;

import java.util.ArrayList;
import java.util.List;
public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
    private final List<CategoryTotal> items = new ArrayList<>();

    public void setItems(List<CategoryTotal> totals) {
        items.clear();
        if (totals != null) {
            items.addAll(totals);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_row, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        CategoryTotal item = items.get(position);
        holder.tvCategory.setText(item.category);
        holder.tvTotal.setText(AppPreferences.formatAmount(holder.itemView.getContext(), item.total));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory;
        TextView tvTotal;

        ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvReportCategory);
            tvTotal = itemView.findViewById(R.id.tvReportTotal);
        }
    }
}
