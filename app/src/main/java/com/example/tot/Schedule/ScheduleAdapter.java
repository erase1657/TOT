package com.example.tot.Schedule;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
        void onScheduleDeleteClick(ScheduleDTO schedule, int position);
        void onScheduleChangeBackgroundClick(ScheduleDTO schedule, int position);
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
        Date startDate = schedule.getStartDate().toDate();
        Date endDate = schedule.getEndDate().toDate();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.KOREA);

        String start = sdf.format(startDate);
        String end = sdf.format(endDate);
        String date = start + " ~ " + end;

        long diffInMillis = endDate.getTime() - startDate.getTime();
        long nights = TimeUnit.MILLISECONDS.toDays(diffInMillis);
        long days = nights + 1;

        holder.tvDate.setText(date);
        holder.tvDateRange.setText(String.format("%d박%d일", nights, days));

        // ▼▼▼ 이미지 로딩 코드 수정 ▼▼▼
        String imageUrl = schedule.getBackground();
        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .into(holder.imgBackground);
        } else {
            // 이미지 URL이 없을 경우 기본 이미지 설정
            holder.imgBackground.setImageResource(R.drawable.sample1);
        }
        // ▲▲▲ 이미지 로딩 코드 수정 ▲▲▲

        // 클릭 이벤트 설정
        if (clickListener != null) {
            holder.itemView.setOnClickListener(v -> {
                clickListener.onScheduleClick(schedule, position);
            });
        }

        holder.btnMenu.setOnClickListener(v -> {
            showPopupMenu(v.getContext(), v, schedule, position);
        });
    }

    private void showPopupMenu(Context context, View view, ScheduleDTO schedule, int position) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.schedule_item_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_delete) {
                if (clickListener != null) {
                    clickListener.onScheduleDeleteClick(schedule, position);
                }
                return true;
            } else if (itemId == R.id.action_change_background) {
                if (clickListener != null) {
                    clickListener.onScheduleChangeBackgroundClick(schedule, position);
                }
                return true;
            }
            return false;
        });
        popup.show();
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    public void updateData(List<ScheduleDTO> newScheduleList) {
        this.scheduleList.clear();
        if (newScheduleList != null) {
            this.scheduleList.addAll(newScheduleList);
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBackground;
        TextView tvLocation;
        TextView tvDateRange;
        TextView tvDate;
        ImageButton btnMenu;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBackground = itemView.findViewById(R.id.img_schedule_background);
            tvLocation = itemView.findViewById(R.id.tv_schedule_location);
            tvDateRange = itemView.findViewById(R.id.tv_date_range);
            tvDate = itemView.findViewById(R.id.tv_date);
            btnMenu = itemView.findViewById(R.id.btn_schedule_menu);
        }
    }
}
