package com.example.tot.MemoryRecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;

import java.util.List;

public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.ViewHolder> {

    private List<MemoryData> items;

    public MemoryAdapter(List<MemoryData> items) {
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, room, startTime, endTime;
        ImageView locationIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_title);
            date = itemView.findViewById(R.id.text_date);
            room = itemView.findViewById(R.id.text_room);
            startTime = itemView.findViewById(R.id.text_start_time);
            endTime = itemView.findViewById(R.id.text_end_time);
            locationIcon = itemView.findViewById(R.id.image_location_point);
        }
    }

    @NonNull
    @Override
    public MemoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_memory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MemoryData item = items.get(position);
        holder.title.setText(item.getTitle());
        holder.date.setText(item.getDate());
        holder.room.setText(item.getRoom());
        holder.startTime.setText(item.getStartTime());
        holder.endTime.setText(item.getEndTime());

        if (item.getLocationIconResId() != 0) {
            holder.locationIcon.setImageResource(item.getLocationIconResId());
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
}
