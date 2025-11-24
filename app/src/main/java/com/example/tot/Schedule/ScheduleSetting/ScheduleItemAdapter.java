package com.example.tot.Schedule.ScheduleSetting;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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

public class ScheduleItemAdapter extends ListAdapter<ScheduleItemDTO, ScheduleItemAdapter.ViewHolder> {

    private List<String> docIdList = new ArrayList<>();
    private final OnItemClickListener listener;
    private OnScheduleMenuItemClickListener onScheduleMenuItemClickListener; // Listener for menu item clicks
    private boolean readOnlyMode = false;

    public interface OnItemClickListener {
        void onItemClick(ScheduleItemDTO item, String docId);
    }

    // New interface for menu item clicks
    public interface OnScheduleMenuItemClickListener {
        void onDeleteClick(String docId);
    }

    // Setter for the new listener
    public void setOnScheduleMenuItemClickListener(OnScheduleMenuItemClickListener listener) {
        this.onScheduleMenuItemClickListener = listener;
    }


    private static final DiffUtil.ItemCallback<ScheduleItemDTO> DIFF_CALLBACK = new DiffUtil.ItemCallback<ScheduleItemDTO>() {
        @Override
        public boolean areItemsTheSame(@NonNull ScheduleItemDTO oldItem, @NonNull ScheduleItemDTO newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getStartTime().equals(newItem.getStartTime());
        }

        @SuppressLint("DiffUtilEquals")
        @Override
        public boolean areContentsTheSame(@NonNull ScheduleItemDTO oldItem, @NonNull ScheduleItemDTO newItem) {
            return oldItem.equals(newItem);
        }
    };

    public ScheduleItemAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void submitList(List<ScheduleItemDTO> list, List<String> docIds) {
        this.docIdList = docIds != null ? docIds : new ArrayList<>();
        super.submitList(list != null ? new ArrayList<>(list) : new ArrayList<>());
    }

    public void setReadOnlyMode(boolean readOnlyMode) {
        this.readOnlyMode = readOnlyMode;
        notifyDataSetChanged();
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

    class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
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
            boolean isOn = item.getAlarm();

            if (start != null && end != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                tv_StartTime.setText(sdf.format(start.toDate()));
                tv_StartTime.setTextColor(Color.parseColor("#000000")); // 시작시간 검정색

                tv_EndTime.setText(sdf.format(end.toDate()));
                tv_EndTime.setTextColor(Color.parseColor("#999999")); // 종료시간 회색
            }

            if (isOn) {
                layout_Alarm.setVisibility(View.VISIBLE);
                item_Schedule.setCardBackgroundColor(Color.parseColor("#E5E6FF"));
            } else {
                layout_Alarm.setVisibility(View.GONE);
                item_Schedule.setCardBackgroundColor(Color.parseColor("#F6F6F5"));
            }

            btn_Modify.setVisibility(readOnlyMode ? View.GONE : View.VISIBLE);

            item_Schedule.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item, docId);
            });

            btn_Modify.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), btn_Modify);
                popup.getMenuInflater().inflate(R.menu.menu_schedule, popup.getMenu());
                popup.setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getItemId() == R.id.menu_delete) {
                        if (onScheduleMenuItemClickListener != null) {
                            onScheduleMenuItemClickListener.onDeleteClick(docId);
                        }
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }
}
