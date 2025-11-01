package com.example.tot.Schedule.ScheduleSetting;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.example.tot.R;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 여행 일정 하단 RecyclerView 어댑터 (DiffUtil 적용 버전)
 * 실시간 데이터 반영 시에도 깜빡임 없이 자연스러운 UI 갱신
 */
public class ScheduleItemAdapter extends ListAdapter<ScheduleItemDTO, ScheduleItemAdapter.ViewHolder> {

    private List<String> docIdList = new ArrayList<>();
    private final OnItemClickListener listener;

    /** 클릭 리스너 인터페이스 */
    public interface OnItemClickListener {
        void onItemClick(ScheduleItemDTO item, String docId);
    }

    /** ✅ DiffUtil — 각 아이템이 다를 때만 갱신 */
    private static final DiffUtil.ItemCallback<ScheduleItemDTO> DIFF_CALLBACK = new DiffUtil.ItemCallback<ScheduleItemDTO>() {
        @Override
        public boolean areItemsTheSame(@NonNull ScheduleItemDTO oldItem, @NonNull ScheduleItemDTO newItem) {
            // 제목 + 시간으로 동일 아이템 판단 (문서 ID는 별도 리스트에서 관리)
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getStartTime().equals(newItem.getStartTime());
        }

        @SuppressLint("DiffUtilEquals")
        @Override
        public boolean areContentsTheSame(@NonNull ScheduleItemDTO oldItem, @NonNull ScheduleItemDTO newItem) {
            // 모든 필드 동일 여부 비교
            return oldItem.equals(newItem);
        }
    };

    public ScheduleItemAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    /** ✅ 일정 리스트와 문서 ID를 함께 전달 */
    public void submitList(List<ScheduleItemDTO> list, List<String> docIds) {
        this.docIdList = docIds != null ? docIds : new ArrayList<>();
        super.submitList(list != null ? new ArrayList<>(list) : new ArrayList<>());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scheduleitem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleItemDTO item = getItem(position);
        String docId = (docIdList != null && position < docIdList.size()) ? docIdList.get(position) : null;
        holder.bind(item, docId, listener);
    }

    static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        TextView tv_Title, tv_StartTime, tv_EndTime, tv_Place;
        LinearLayout layout_Place, layout_Alarm;
        CardView item_Schedule;
        Button btn_Modify;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_Title = itemView.findViewById(R.id.tv_title);
            tv_StartTime = itemView.findViewById(R.id.tv_starttime);
            tv_EndTime = itemView.findViewById(R.id.tv_endtime);
            tv_Place = itemView.findViewById(R.id.tv_place);
            layout_Place = itemView.findViewById(R.id.layout_place);
            layout_Alarm = itemView.findViewById(R.id.layout_alarm);
            item_Schedule = itemView.findViewById(R.id.item_schedule);
            btn_Modify = itemView.findViewById(R.id.btn_modify);
        }

        void bind(ScheduleItemDTO item, String docId, OnItemClickListener listener) {
            tv_Title.setText(item.getTitle());
            tv_Place.setText(item.getPlaceName());

            Timestamp start = item.getStartTime();
            Timestamp end = item.getEndTime();
            boolean isOn = item.getAlarm(); // 알람 여부

            // ✅ 시간 포맷팅
            if (start != null && end != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                tv_StartTime.setText(sdf.format(start.toDate()));
                tv_EndTime.setText(sdf.format(end.toDate()));
            }

            // ✅ 알람 상태에 따른 배경색과 뷰 표시
            if (isOn) {
                layout_Alarm.setVisibility(View.VISIBLE);
                item_Schedule.setCardBackgroundColor(Color.parseColor("#E5E6FF")); // 알람 켜짐
            } else {
                layout_Alarm.setVisibility(View.GONE);
                item_Schedule.setCardBackgroundColor(Color.parseColor("#F6F6F5")); // 알람 꺼짐 기본색
            }

            // ✅ 카드 클릭 → 수정용 바텀시트 호출
            item_Schedule.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item, docId);
            });
        }
    }
}
