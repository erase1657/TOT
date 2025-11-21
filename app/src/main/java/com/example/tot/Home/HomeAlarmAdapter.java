package com.example.tot.Home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;
import com.google.firebase.Timestamp;

import java.util.List;

public class HomeAlarmAdapter extends RecyclerView.Adapter<HomeAlarmAdapter.ViewHolder> {

    private List<HomeAlarmDTO> items;

    public HomeAlarmAdapter(List<HomeAlarmDTO> items) {
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, location, startTime, endTime;
        ImageView locationIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_title);
            date = itemView.findViewById(R.id.text_date);
            location = itemView.findViewById(R.id.text_location);
            startTime = itemView.findViewById(R.id.text_start_time);
            endTime = itemView.findViewById(R.id.text_end_time);
            locationIcon = itemView.findViewById(R.id.image_location_point);
        }
    }

    @NonNull
    @Override
    public HomeAlarmAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_alarm, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HomeAlarmDTO item = items.get(position);
        holder.title.setText(item.getTitle());
        holder.date.setText(item.getDate());
        holder.location.setText(item.getPlace());
        holder.startTime.setText(formatTime(item.getStartTime()));
        holder.endTime.setText(formatTime(item.getEndTime()));

        if (item.getPlace() == null || item.getPlace().isEmpty()) {
            holder.locationIcon.setVisibility(View.GONE);
            holder.location.setVisibility(View.GONE);
        } else {
            holder.locationIcon.setVisibility(View.VISIBLE);
            holder.location.setVisibility(View.VISIBLE);
            holder.location.setText(item.getPlace());
        }

    }
    private String formatTime(Timestamp ts) {
        if (ts == null) return "";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
        return sdf.format(ts.toDate());
    }
    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
}
