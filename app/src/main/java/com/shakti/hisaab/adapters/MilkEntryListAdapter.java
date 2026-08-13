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
import com.shakti.hisaab.database.entities.MilkEntry;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MilkEntryListAdapter extends RecyclerView.Adapter<MilkEntryListAdapter.EntryViewHolder> {

    public interface OnEntryClickListener {
        void onEntryClick(MilkEntry entry);
    }

    private final List<MilkEntry> entries;
    private final OnEntryClickListener listener;
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault());

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
        holder.tvDate.setText(formatDate(entry.date));

        if (entry.taken) {
            holder.tvDetails.setText(formatQuantity(entry.quantity) + "  " + (entry.paid ? "Paid" : "Unpaid"));
            holder.tvAmount.setText(AppPreferences.formatAmount(holder.itemView.getContext(), entry.totalCost));
            holder.viewStatus.setBackgroundTintList(ContextCompat.getColorStateList(
                    holder.itemView.getContext(),
                    entry.paid ? R.color.day_paid : R.color.day_unpaid
            ));
        } else {
            holder.tvDetails.setText("Not Taken");
            holder.tvAmount.setText("-");
            holder.viewStatus.setBackgroundTintList(ContextCompat.getColorStateList(
                    holder.itemView.getContext(),
                    R.color.day_not_taken
            ));
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

    private String formatDate(String date) {
        try {
            return LocalDate.parse(date).format(dateFormatter);
        } catch (Exception e) {
            return date;
        }
    }

    public static class EntryViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvStatus;
        TextView tvDetails;
        TextView tvAmount;
        View viewStatus;

        public EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvEntryDate);
            tvStatus = itemView.findViewById(R.id.tvEntryStatus);
            tvDetails = itemView.findViewById(R.id.tvEntryDetails);
            tvAmount = itemView.findViewById(R.id.tvEntryAmount);
            viewStatus = itemView.findViewById(R.id.viewEntryStatus);
        }
    }
}
