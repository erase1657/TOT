package com.example.tot.Schedule;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScheduleItemAdapter extends RecyclerView.Adapter<ScheduleItemAdapter.ViewHolder> {

    private List<SchedulePlanItem> scheduleList = new ArrayList<>();
    private OnItemClickListener listener;

    // 아이템 클릭 인터페이스
    public interface OnItemClickListener {
        void onItemClick(SchedulePlanItem item);
    }

    public ScheduleItemAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<SchedulePlanItem> list) {
        this.scheduleList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scheduleplan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SchedulePlanItem item = scheduleList.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }
    public List<SchedulePlanItem> getCurrentList() {
        return scheduleList;
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv_Title, tv_StartTime, tv_EndTime, tv_Place;
        LinearLayout layout_Place, layout_Alarm;
        Button btn_Modify;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_Title = itemView.findViewById(R.id.tv_title);
            tv_StartTime = itemView.findViewById(R.id.tv_starttime);
            tv_EndTime = itemView.findViewById(R.id.tv_endtime);
            tv_Place = itemView.findViewById(R.id.tv_place);
            layout_Place = itemView.findViewById(R.id.layout_place);
            layout_Alarm = itemView.findViewById(R.id.layout_alarm);
            btn_Modify = itemView.findViewById(R.id.btn_modify);
        }

        void bind(SchedulePlanItem item, OnItemClickListener listener) {
            tv_Title.setText(item.getTitle());
            tv_Place.setText(item.getPlaceName());

            Timestamp start = item.getStartTime();
            Timestamp end = item.getEndTime();

            if (start != null && end != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String startText = sdf.format(start.toDate());
                String endText = sdf.format(end.toDate());

                tv_StartTime.setText(startText);
                tv_EndTime.setText(endText);
            }
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }

    }
}