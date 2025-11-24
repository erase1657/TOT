package com.example.tot.MyPage;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
        private Context context;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            tvLocationNames = itemView.findViewById(R.id.tv_location_names);
            imgBackground = itemView.findViewById(R.id.img_schedule_background);
        }

        public void bind(ScheduleDTO schedule, int position) {
            if (schedule == null) return;

            // 지역명 설정
            if (schedule.getLocationName() != null && !schedule.getLocationName().isEmpty()) {
                tvLocationNames.setText(schedule.getLocationName());
            } else {
                tvLocationNames.setText("지역");
            }

            // 배경 이미지 설정
            if (schedule.getBackgroundImageUri() != null) {
                Glide.with(context)
                        .load(Uri.parse(schedule.getBackgroundImageUri()))
                        .into(imgBackground);
            } else if (schedule.getThumbnailRef() != null) {
                Glide.with(context)
                        .load(schedule.getThumbnailRef())
                        .into(imgBackground);
            } else {
                imgBackground.setImageResource(R.drawable.sample3);
            }

            // 클릭 이벤트
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onScheduleClick(schedule, position);
                }
            });
        }
    }
}
