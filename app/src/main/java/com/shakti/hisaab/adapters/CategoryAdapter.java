package com.shakti.hisaab.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.shakti.hisaab.R;
import com.shakti.hisaab.model.CategoryItem;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryItem item);
    }

    private final List<CategoryItem> items;
    private final OnCategoryClickListener listener;

    public CategoryAdapter(List<CategoryItem> items, OnCategoryClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_card, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryItem item = items.get(position);
        Context context = holder.itemView.getContext();
        holder.tvTitle.setText(item.name);
        holder.tvSubtitle.setText(item.subtitle);
        holder.tvEmoji.setText(item.icon);
        int color = ContextCompat.getColor(context, item.iconColorRes);
        holder.tvEmoji.setTextColor(color);
        holder.tvEmoji.setBackgroundTintList(ContextCompat.getColorStateList(context, item.iconBackgroundRes));

        holder.itemView.setOnClickListener(v -> listener.onCategoryClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvEmoji;
        TextView tvTitle;
        TextView tvSubtitle;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardCategory);
            tvEmoji = itemView.findViewById(R.id.tvCategoryEmoji);
            tvTitle = itemView.findViewById(R.id.tvCategoryTitle);
            tvSubtitle = itemView.findViewById(R.id.tvCategorySubtitle);
        }
    }
}
