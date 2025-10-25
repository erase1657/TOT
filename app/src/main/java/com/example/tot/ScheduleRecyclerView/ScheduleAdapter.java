package com.example.tot.ScheduleRecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;

import java.util.ArrayList;
import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private List<ScheduleData> scheduleList;
    private OnScheduleClickListener clickListener;

    // 클릭 리스너 인터페이스
    public interface OnScheduleClickListener {
        void onScheduleClick(ScheduleData schedule, int position);
    }

    public ScheduleAdapter(List<ScheduleData> scheduleList) {
        this.scheduleList = scheduleList != null ? scheduleList : new ArrayList<>();
    }

    public ScheduleAdapter(List<ScheduleData> scheduleList, OnScheduleClickListener clickListener) {
        this.scheduleList = scheduleList != null ? scheduleList : new ArrayList<>();
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleData schedule = scheduleList.get(position);

        holder.tvLocation.setText(schedule.getLocation());
        holder.tvDateRange.setText(schedule.getDateRange());
        holder.tvYearMonth.setText(schedule.getYearMonth());
        holder.imgBackground.setImageResource(schedule.getBackgroundImage());

        // 클릭 이벤트 설정
        if (clickListener != null) {
            holder.itemView.setOnClickListener(v -> {
                clickListener.onScheduleClick(schedule, position);
            });
        }
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    /**
     * 스케줄 데이터 업데이트 메서드
     */
    public void updateData(List<ScheduleData> newScheduleList) {
        this.scheduleList.clear();
        if (newScheduleList != null) {
            this.scheduleList.addAll(newScheduleList);
        }
        notifyDataSetChanged();
    }

    /**
     * 스케줄 추가 메서드
     */
    public void addSchedule(ScheduleData schedule) {
        this.scheduleList.add(schedule);
        notifyItemInserted(scheduleList.size() - 1);
    }

    /**
     * 스케줄 삭제 메서드
     */
    public void removeSchedule(int position) {
        if (position >= 0 && position < scheduleList.size()) {
            this.scheduleList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBackground;
        TextView tvLocation;
        TextView tvDateRange;
        TextView tvYearMonth;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBackground = itemView.findViewById(R.id.img_schedule_background);
            tvLocation = itemView.findViewById(R.id.tv_schedule_location);
            tvDateRange = itemView.findViewById(R.id.tv_schedule_dates);
            tvYearMonth = itemView.findViewById(R.id.tv_schedule_year_month);
        }
    }
}