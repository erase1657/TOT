package com.example.tot.MemoryRecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;

import java.util.*;

public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.ViewHolder> {
    private List<MemoryData> items;

    public MemoryAdapter(List<MemoryData> items) {
        this.items = items;
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profile;
        TextView region, place;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profile = itemView.findViewById(R.id.img_profile);
            region = itemView.findViewById(R.id.tv_region);
            place = itemView.findViewById(R.id.tv_place);
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
        holder.profile.setImageResource(item.getImg_profile());
        holder.region.setText(item.getTv_region());
        holder.place.setText(item.getTv_place());
    }

    @Override
    public int getItemCount() {
        return items.size();

    }
}
