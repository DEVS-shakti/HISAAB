package com.shakti.hisaab.adapters;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.shakti.hisaab.R;
import com.shakti.hisaab.model.CalendarDay;

import java.util.List;
import java.util.Locale;

public class MilkCalendarAdapter extends RecyclerView.Adapter<MilkCalendarAdapter.DayViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(CalendarDay day);
    }

    private final List<CalendarDay> daysList;
    private final OnDayClickListener listener;

    public MilkCalendarAdapter(List<CalendarDay> daysList, OnDayClickListener listener) {
        this.daysList = daysList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        CalendarDay day = daysList.get(position);
        Context context = holder.itemView.getContext();

        if (day.state == CalendarDay.State.EMPTY) {
            holder.tvDay.setText("");
            holder.tvStatus.setText("");
            holder.tvQuantity.setText("");
            holder.viewStatusDot.setVisibility(View.GONE);
            holder.itemView.setBackground(null);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setElevation(0f);
            return;
        }

        holder.tvDay.setText(String.valueOf(day.dayOfMonth));
        applyBackground(context, holder, day);

        switch (day.state) {
            case PAID:
                holder.tvStatus.setText("\u2713");
                holder.tvQuantity.setText(formatQuantity(day.quantity));
                holder.tvStatus.setVisibility(View.VISIBLE);
                holder.tvQuantity.setVisibility(View.VISIBLE);
                holder.viewStatusDot.setVisibility(View.VISIBLE);
                holder.viewStatusDot.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, R.color.day_paid)
                );
                setTextColors(holder, context, android.R.color.white);
                break;
            case UNPAID:
                holder.tvStatus.setText("\u2715");
                holder.tvQuantity.setText(formatQuantity(day.quantity));
                holder.tvStatus.setVisibility(View.VISIBLE);
                holder.tvQuantity.setVisibility(View.VISIBLE);
                holder.viewStatusDot.setVisibility(View.VISIBLE);
                holder.viewStatusDot.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, R.color.day_unpaid)
                );
                setTextColors(holder, context, android.R.color.white);
                break;
            case NOT_TAKEN:
                holder.tvStatus.setText("-");
                holder.tvQuantity.setText("");
                holder.tvStatus.setVisibility(View.VISIBLE);
                holder.tvQuantity.setVisibility(View.GONE);
                holder.viewStatusDot.setVisibility(View.VISIBLE);
                holder.viewStatusDot.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, R.color.day_not_taken)
                );
                setTextColors(holder, context, R.color.status_unpaid);
                break;
            case NO_ENTRY:
                holder.tvStatus.setText("");
                holder.tvQuantity.setText("");
                holder.tvStatus.setVisibility(View.GONE);
                holder.tvQuantity.setVisibility(View.GONE);
                holder.viewStatusDot.setVisibility(View.GONE);
                setTextColors(holder, context, R.color.text_secondary);
                break;
            case FUTURE:
                holder.tvStatus.setText("");
                holder.tvQuantity.setText("");
                holder.tvStatus.setVisibility(View.GONE);
                holder.tvQuantity.setVisibility(View.GONE);
                holder.viewStatusDot.setVisibility(View.GONE);
                setTextColors(holder, context, R.color.text_muted);
                break;
            default:
                break;
        }

        if (day.state == CalendarDay.State.FUTURE) {
            holder.itemView.setOnClickListener(null);
        } else {
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDayClick(day);
                }
            });
        }

        float elevation = (day.state == CalendarDay.State.PAID || day.state == CalendarDay.State.UNPAID)
                ? dpToPx(context, 3f)
                : 0f;
        holder.itemView.setElevation(elevation);
    }

    @Override
    public int getItemCount() {
        return daysList.size();
    }

    private void applyBackground(Context context, DayViewHolder holder, CalendarDay day) {
        GradientDrawable drawable;
        boolean dashed = false;
        int strokeColor = 0;

        switch (day.state) {
            case PAID:
                drawable = new GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        new int[]{
                                ContextCompat.getColor(context, R.color.day_paid),
                                ContextCompat.getColor(context, R.color.day_paid_end)
                        }
                );
                break;
            case UNPAID:
                drawable = new GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        new int[]{
                                ContextCompat.getColor(context, R.color.day_unpaid),
                                ContextCompat.getColor(context, R.color.day_unpaid_end)
                        }
                );
                break;
            case NOT_TAKEN:
                drawable = new GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        new int[]{
                                ContextCompat.getColor(context, R.color.day_not_taken),
                                ContextCompat.getColor(context, R.color.day_not_taken_end)
                        }
                );
                break;
            case FUTURE:
                drawable = new GradientDrawable();
                drawable.setColor(ContextCompat.getColor(context, R.color.day_future));
                break;
            case NO_ENTRY:
            default:
                drawable = new GradientDrawable();
                drawable.setColor(ContextCompat.getColor(context, R.color.day_no_entry));
                strokeColor = ContextCompat.getColor(context, R.color.day_no_entry_stroke);
                dashed = true;
                break;
        }

        drawable.setCornerRadius(dpToPx(context, 12));

        if (dashed) {
            drawable.setStroke((int) dpToPx(context, 2), strokeColor, dpToPx(context, 6), dpToPx(context, 4));
        }

        if (day.isToday) {
            int todayStroke = ContextCompat.getColor(context, R.color.day_today_stroke);
            drawable.setStroke((int) dpToPx(context, 3), todayStroke);
        }

        holder.itemView.setBackground(drawable);
    }

    private void setTextColors(DayViewHolder holder, Context context, int colorRes) {
        int color = ContextCompat.getColor(context, colorRes);
        holder.tvDay.setTextColor(color);
        holder.tvStatus.setTextColor(color);
        holder.tvQuantity.setTextColor(color);
    }

    private String formatQuantity(Double quantity) {
        if (quantity == null) {
            return "";
        }
        if (quantity == Math.floor(quantity)) {
            return String.format(Locale.getDefault(), "%.0fL", quantity);
        }
        return String.format(Locale.getDefault(), "%.1fL", quantity);
    }

    private float dpToPx(Context context, float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

    public static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay;
        TextView tvStatus;
        TextView tvQuantity;
        View viewStatusDot;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            viewStatusDot = itemView.findViewById(R.id.viewStatusDot);
        }
    }
}

