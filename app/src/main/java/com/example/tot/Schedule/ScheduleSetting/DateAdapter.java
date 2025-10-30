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

    private List<LocalDate> dateList;
    private LocalDate selectedDate;
    private OnDateClickListener listener;

    // 날짜 클릭 콜백
    public interface OnDateClickListener {
        void onDateClick(LocalDate date);
    }

    public DateAdapter(List<LocalDate> dateList, OnDateClickListener listener) {
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
        LocalDate date = dateList.get(position);

        // 요일과 일 표시
        String dayText = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dayText = String.valueOf(date.getDayOfMonth());
        }
        String weekText = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            weekText = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
        }

        holder.tvDay.setText(dayText);
        holder.tvWeek.setText(weekText);

        // 선택 여부에 따라 색상 변경
        if (date.equals(selectedDate)) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#575DFB")); ;
            holder.tvDay.setTextColor(Color.parseColor("#ffffff"));
            holder.tvWeek.setTextColor(Color.parseColor("#ffffff"));
        } else {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#ffffff"));
            holder.tvDay.setTextColor(Color.parseColor("#000000"));
            holder.tvWeek.setTextColor(Color.parseColor("#BCC1CD"));
        }

        holder.itemView.setOnClickListener(v -> {
            selectedDate = date;
            if (listener != null) listener.onDateClick(date);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    public void setSelectedDate(LocalDate date) {
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
