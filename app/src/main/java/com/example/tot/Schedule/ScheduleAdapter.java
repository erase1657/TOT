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

    private final List<ScheduleDTO> scheduleList;
    private final OnScheduleClickListener clickListener;

    // 클릭 리스너 인터페이스
    public interface OnScheduleClickListener {
        void onScheduleClick(ScheduleDTO schedule, int position);
    }

    public ScheduleAdapter(List<ScheduleDTO> scheduleList) {
        this.scheduleList = scheduleList != null ? scheduleList : new ArrayList<>();
        this.clickListener = null;
    }

    public ScheduleAdapter(List<ScheduleDTO> scheduleList, OnScheduleClickListener clickListener) {
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
        ScheduleDTO schedule = scheduleList.get(position);
        holder.bind(schedule, clickListener);
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    /**
     * 스케줄 데이터 업데이트 메서드
     * notifyDataSetChanged()를 사용하여 전체 리스트 갱신
     */
    public void updateData(List<ScheduleDTO> newScheduleList) {
        if (newScheduleList == null) {
            scheduleList.clear();
        } else {
            scheduleList.clear();
            scheduleList.addAll(newScheduleList);
        }
        notifyDataSetChanged();
    }

    /**
     * 스케줄 추가 메서드
     * 특정 위치에 아이템을 추가하고 해당 위치만 업데이트
     */
    public void addSchedule(ScheduleDTO schedule) {
        if (schedule != null) {
            scheduleList.add(schedule);
            notifyItemInserted(scheduleList.size() - 1);
        }
    }

    /**
     * 스케줄 삭제 메서드
     * 특정 위치의 아이템을 삭제하고 해당 위치만 업데이트
     */
    public void removeSchedule(int position) {
        if (position >= 0 && position < scheduleList.size()) {
            scheduleList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, scheduleList.size());
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
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

        public void bind(ScheduleDTO schedule, OnScheduleClickListener listener) {
            // Null 체크
            if (schedule == null) return;

            // 지역 정보 설정
            if (schedule.getLocationName() != null && !schedule.getLocationName().isEmpty()) {
                tvLocation.setText(schedule.getLocationName());
            } else {
                tvLocation.setText("지역");
            }

            // 날짜 정보 설정
            if (schedule.getStartDate() != null && schedule.getEndDate() != null) {
                Date startDate = schedule.getStartDate().toDate();
                Date endDate = schedule.getEndDate().toDate();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.KOREA);
                String start = sdf.format(startDate);
                String end = sdf.format(endDate);

                // 박/일 계산
                long diffInMillis = endDate.getTime() - startDate.getTime();
                long nights = TimeUnit.MILLISECONDS.toDays(diffInMillis);
                long days = nights + 1;

                tvDate.setText(String.format("%s~%s", start, end));
                tvDateRange.setText(String.format("%d박%d일", nights, days));
            } else {
                tvDate.setText("날짜 미정");
                tvDateRange.setText("");
            }

            // TODO: 홀더에 백그라운드 이미지도 설정하게 만들어야함.
            // if (schedule.getThumbnailRef() != null) {
            //     // 썸네일 로드 로직
            // }

            // 클릭 이벤트 설정
            if (listener != null) {
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onScheduleClick(schedule, position);
                    }
                });
            }
        }
    }
}