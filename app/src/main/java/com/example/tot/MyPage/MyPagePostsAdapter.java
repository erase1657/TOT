package com.example.tot.MyPage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tot.Community.CommunityPostDTO;
import com.example.tot.R;

import java.util.List;

public class MyPagePostsAdapter extends RecyclerView.Adapter<MyPagePostsAdapter.PostViewHolder> {

    private List<CommunityPostDTO> postList;
    private OnPostClickListener listener;

    public interface OnPostClickListener {
        void onPostClick(CommunityPostDTO post, int position);
    }

    public MyPagePostsAdapter(List<CommunityPostDTO> postList, OnPostClickListener listener) {
        this.postList = postList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mypage_schedule, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        CommunityPostDTO post = postList.get(position);
        holder.bind(post, listener);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imgScheduleBackground;
        TextView tvLocationNames;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imgScheduleBackground = itemView.findViewById(R.id.img_schedule_background);
            tvLocationNames = itemView.findViewById(R.id.tv_location_names);
        }

        public void bind(final CommunityPostDTO post, final OnPostClickListener listener) {
            if (post.getThumbnailUrl() != null && !post.getThumbnailUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(post.getThumbnailUrl())
                        .into(imgScheduleBackground);
            } else if (post.getPostImage() != 0) {
                imgScheduleBackground.setImageResource(post.getPostImage());
            } else {
                imgScheduleBackground.setImageResource(R.drawable.sample3); // Placeholder image
            }

            tvLocationNames.setText(post.getTitle());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPostClick(post, getAdapterPosition());
                }
            });
        }
    }
}
