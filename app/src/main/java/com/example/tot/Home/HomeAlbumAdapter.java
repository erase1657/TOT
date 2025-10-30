package com.example.tot.Home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;

import java.util.ArrayList;
import java.util.List;

public class HomeAlbumAdapter extends RecyclerView.Adapter<HomeAlbumAdapter.ViewHolder> {
    private List<HomeAlbumData> items;

    public HomeAlbumAdapter(List<HomeAlbumData> items){
        this.items = items != null ? items : new ArrayList<>();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView userProfile;
        ImageView albumProfile;
        TextView userName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userProfile = itemView.findViewById(R.id.img_userprofile);
            albumProfile = itemView.findViewById(R.id.img_albumprofile);
            userName = itemView.findViewById(R.id.tv_username);
        }
    }

    @NonNull
    @Override
    public HomeAlbumAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        return new HomeAlbumAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HomeAlbumAdapter.ViewHolder holder, int position) {
        HomeAlbumData item = items.get(position);
        holder.userProfile.setImageResource(item.getUserProfile());
        holder.albumProfile.setImageResource(item.getAlbumProfile());
        holder.userName.setText(item.getUserName());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * 앨범 데이터 업데이트 메서드 (지역 필터링 시 사용)
     * @param newItems 새로운 앨범 리스트
     */
    public void updateData(List<HomeAlbumData> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }
}