package com.shakti.hisaab.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shakti.hisaab.R;
import com.shakti.hisaab.database.entities.MilkEntry;

import java.util.List;

public class MilkEntryListAdapter extends RecyclerView.Adapter<MilkEntryListAdapter.EntryViewHolder> {

    public interface OnEntryClickListener {
        void onEntryClick(MilkEntry entry);
    }

    private final List<MilkEntry> entries;
    private final OnEntryClickListener listener;

    public MilkEntryListAdapter(List<MilkEntry> entries, OnEntryClickListener listener) {
        this.entries = entries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entry_row, parent, false);
        return new EntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        MilkEntry entry = entries.get(position);
        holder.tvDate.setText(entry.date);

        if (entry.taken) {
            holder.tvStatus.setText(entry.paid ? "Paid" : "Unpaid");
            holder.tvDetails.setText(formatQuantity(entry.quantity) + " at " + formatCurrency(entry.totalCost));
        } else {
            holder.tvStatus.setText("Not taken");
            holder.tvDetails.setText("-");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEntryClick(entry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    private String formatQuantity(double quantity) {
        if (quantity == Math.floor(quantity)) {
            return String.format("%.0fL", quantity);
        }
        return String.format("%.1fL", quantity);
    }

    private String formatCurrency(double amount) {
        return "Rs " + String.format("%.0f", amount);
    }

    public static class EntryViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvStatus;
        TextView tvDetails;

        public EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvEntryDate);
            tvStatus = itemView.findViewById(R.id.tvEntryStatus);
            tvDetails = itemView.findViewById(R.id.tvEntryDetails);
        }
    }
}
