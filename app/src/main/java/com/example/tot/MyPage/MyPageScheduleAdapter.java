package com.example.tot.MyPage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;
import com.example.tot.Schedule.ScheduleDTO;

import java.util.List;

public class MyPageScheduleAdapter extends RecyclerView.Adapter<MyPageScheduleAdapter.ViewHolder> {

    private List<ScheduleDTO> scheduleList;
    private OnScheduleClickListener listener;

    public interface OnScheduleClickListener {
        void onScheduleClick(ScheduleDTO schedule, int position);
    }

    public MyPageScheduleAdapter(List<ScheduleDTO> scheduleList, OnScheduleClickListener listener) {
        this.scheduleList = scheduleList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mypage_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleDTO schedule = scheduleList.get(position);
        holder.bind(schedule, position);
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    /**
     * 데이터 업데이트
     */
    public void updateData(List<ScheduleDTO> newScheduleList) {
        this.scheduleList = newScheduleList;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvLocationNames;
        private ImageView imgBackground;
        private TextView tvDates;
        private TextView tvYearMonth;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocationNames = itemView.findViewById(R.id.tv_location_names);
            imgBackground = itemView.findViewById(R.id.img_schedule_background);
            tvDates = itemView.findViewById(R.id.tv_schedule_dates);
            tvYearMonth = itemView.findViewById(R.id.tv_schedule_year_month);
        }

        public void bind(ScheduleDTO schedule, int position) {
           /* // 지역명 설정
            tvLocationNames.setText(schedule.getLocation());

            // 배경 이미지 설정
            imgBackground.setImageResource(schedule.getBackgroundImage());

            // 날짜 설정
            tvDates.setText(schedule.getDateRange());
            tvYearMonth.setText(schedule.getYearMonth());
*/
            // 클릭 이벤트
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onScheduleClick(schedule, position);
                }
            });
        }
    }
}