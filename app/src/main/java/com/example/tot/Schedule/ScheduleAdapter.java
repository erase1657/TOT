package com.example.tot.Schedule;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private List<ScheduleDTO> scheduleList;
    private OnScheduleClickListener clickListener;

    // 클릭 리스너 인터페이스
    public interface OnScheduleClickListener {
        void onScheduleClick(ScheduleDTO schedule, int position);
    }

    public ScheduleAdapter(List<ScheduleDTO> scheduleList) {
        this.scheduleList = scheduleList != null ? new ArrayList<>(scheduleList) : new ArrayList<>();
    }

    public ScheduleAdapter(List<ScheduleDTO> scheduleList, OnScheduleClickListener clickListener) {
        this.scheduleList = scheduleList != null ? new ArrayList<>(scheduleList) : new ArrayList<>();
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
        ScheduleDTO schedule = scheduleList.get(position);
        Date startDate = schedule.getStartDate().toDate();
        Date endDate = schedule.getEndDate().toDate();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.KOREA);

        String start = sdf.format(startDate);
        String end = sdf.format(endDate);
        String date = start + " ~ " + end;

        long diffInMillis = endDate.getTime() - startDate.getTime();
        long nights = TimeUnit.MILLISECONDS.toDays(diffInMillis); // 2박
        long days = nights + 1;

        holder.tvDate.setText(String.format("%s~%s", start, end)); // 2020/1/3~2020/1/5
        holder.tvDateRange.setText(String.format("%d박%d일", nights, days)); // 2박3일
        holder.tvDate.setText(date);
        //TODO:홀더에 백그라운드 이미지도 설정하게 만들어야함.

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
    public void updateData(List<ScheduleDTO> newScheduleList) {
        List<ScheduleDTO> copiedList = newScheduleList != null ? new ArrayList<>(newScheduleList) : new ArrayList<>();
        this.scheduleList.clear();
        this.scheduleList.addAll(copiedList);
        notifyDataSetChanged();
    }

    /**
     * 스케줄 추가 메서드
     */
    public void addSchedule(ScheduleDTO schedule) {
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
        TextView tvDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBackground = itemView.findViewById(R.id.img_schedule_background);
            tvLocation = itemView.findViewById(R.id.tv_schedule_location);
            tvDateRange = itemView.findViewById(R.id.tv_date_range);
            tvDate = itemView.findViewById(R.id.tv_date);
        }
    }
}