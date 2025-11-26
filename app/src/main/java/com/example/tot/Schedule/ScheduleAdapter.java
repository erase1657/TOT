package com.example.tot.Schedule;

import android.content.Context;
import android.net.Uri;
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

    private final List<ScheduleDTO> scheduleList;
    private final OnScheduleClickListener clickListener;
    private OnMenuItemClickListener menuClickListener;

    // 클릭 리스너 인터페이스
    public interface OnScheduleClickListener {
        void onScheduleClick(ScheduleDTO schedule, int position);
    }

    // 메뉴 아이템 클릭 리스너 인터페이스
    public interface OnMenuItemClickListener {
        void onChangeBackgroundClick(ScheduleDTO schedule, int position);
        void onDeleteClick(ScheduleDTO schedule, int position);
        void onEditTitleClick(ScheduleDTO schedule, int position); // 제목 수정 추가
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        this.menuClickListener = listener;
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
        holder.bind(schedule, clickListener, menuClickListener);
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

    public void updateScheduleItem(int position, ScheduleDTO schedule) {
        if (position >= 0 && position < scheduleList.size()) {
            scheduleList.set(position, schedule);
            notifyItemChanged(position);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBackground;
        TextView tvTitle; // tvLocation에서 tvTitle로 변경
        TextView tvDateRange;
        TextView tvDate;
        ImageButton btnOptions;
        Context context;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            imgBackground = itemView.findViewById(R.id.img_schedule_background);
            tvTitle = itemView.findViewById(R.id.tv_schedule_title); // ID 변경
            tvDateRange = itemView.findViewById(R.id.tv_date_range);
            tvDate = itemView.findViewById(R.id.tv_date);
            btnOptions = itemView.findViewById(R.id.btn_schedule_options);
        }

        public void bind(ScheduleDTO schedule, OnScheduleClickListener listener, OnMenuItemClickListener menuListener) {
            // Null 체크
            if (schedule == null) return;

            // 지역 정보 설정
            if (schedule.getLocationName() != null && !schedule.getLocationName().isEmpty()) {
                tvTitle.setText(schedule.getLocationName());
            } else {
                tvTitle.setText("제목 없음"); // 기본 텍스트 변경
            }

            // 날짜 정보 설정
            if (schedule.getStartDate() != null && schedule.getEndDate() != null) {
                Date startDate = schedule.getStartDate().toDate();
                Date endDate = schedule.getEndDate().toDate();

                // yyyy.MM.dd 형식으로 날짜 포맷 (점으로 구분)
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
                String startDateStr = sdf.format(startDate);
                String endDateStr = sdf.format(endDate);

                // tvDateRange에 날짜 범위 표시
                tvDateRange.setText(String.format("%s~%s", startDateStr, endDateStr));

                // D-Day 계산 (시간을 제외하고 날짜만 비교)
                java.util.Calendar startCal = java.util.Calendar.getInstance();
                startCal.setTime(startDate);
                startCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                startCal.set(java.util.Calendar.MINUTE, 0);
                startCal.set(java.util.Calendar.SECOND, 0);
                startCal.set(java.util.Calendar.MILLISECOND, 0);

                java.util.Calendar todayCal = java.util.Calendar.getInstance();
                todayCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                todayCal.set(java.util.Calendar.MINUTE, 0);
                todayCal.set(java.util.Calendar.SECOND, 0);
                todayCal.set(java.util.Calendar.MILLISECOND, 0);

                long diff = startCal.getTimeInMillis() - todayCal.getTimeInMillis();
                long days = TimeUnit.MILLISECONDS.toDays(diff);

                // tvDate에 D-Day 표시
                if (days < 0) {
                    tvDate.setText("지난 여행");
                } else if (days == 0) {
                    tvDate.setText("D-DAY");
                } else {
                    tvDate.setText(String.format(Locale.getDefault(), "D-%d", days));
                }
            } else {
                tvDateRange.setText("날짜 정보 없음");
                tvDate.setText("");
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

            // 클릭 이벤트 설정
            if (listener != null) {
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onScheduleClick(schedule, position);
                    }
                });
            }
            btnOptions.setVisibility(schedule.isShared() ? View.GONE : View.VISIBLE);
            // 더보기 버튼 클릭 이벤트
            if (menuListener != null) {
                btnOptions.setOnClickListener(v -> {
                    PopupMenu popup = new PopupMenu(v.getContext(), btnOptions);
                    popup.getMenuInflater().inflate(R.menu.item_schedule_menu, popup.getMenu());

                    popup.setOnMenuItemClickListener(menuItem -> {
                        int position = getAdapterPosition();
                        if (position == RecyclerView.NO_POSITION) {
                            return false;
                        }

                        int itemId = menuItem.getItemId();
                        if (itemId == R.id.action_change_background) {
                            menuListener.onChangeBackgroundClick(schedule, position);
                            return true;
                        } else if (itemId == R.id.action_delete_schedule) {
                            menuListener.onDeleteClick(schedule, position);
                            return true;
                        } else if (itemId == R.id.action_edit_title) {
                            menuListener.onEditTitleClick(schedule, position);
                            return true;
                        }
                        return false;
                    });
                    popup.show();
                });
            }
        }
    }
}
