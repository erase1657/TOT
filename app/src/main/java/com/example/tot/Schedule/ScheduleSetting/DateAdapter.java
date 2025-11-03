package com.example.tot.Schedule.ScheduleSetting;

import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
public class DateAdapter extends RecyclerView.Adapter<DateAdapter.ViewHolder> {

    private List<String> dateList;
    private String selectedDate;
    private OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClick(String date);
    }

    public DateAdapter(List<String> dateList, OnDateClickListener listener) {
        this.dateList = dateList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendarview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String dateString = dateList.get(position); // yyyy-MM-dd
        LocalDate date = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            date = LocalDate.parse(dateString);
        }

        if (date != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                holder.tvDay.setText(String.valueOf(date.getDayOfMonth()));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                holder.tvWeek.setText(
                        date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN)
                );
            }
        }

        if (dateString.equals(selectedDate)) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#575DFB"));
            holder.tvDay.setTextColor(Color.WHITE);
            holder.tvWeek.setTextColor(Color.WHITE);
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.tvDay.setTextColor(Color.BLACK);
            holder.tvWeek.setTextColor(Color.parseColor("#BCC1CD"));
        }

        holder.itemView.setOnClickListener(v -> {
            selectedDate = dateString;
            if (listener != null) listener.onDateClick(dateString);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    public void setSelectedDate(String date) {
        selectedDate = date;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvWeek;
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_day);
            tvWeek = itemView.findViewById(R.id.tv_day_of_week);
            cardView = itemView.findViewById(R.id.item_calendarview);
        }
    }
}
