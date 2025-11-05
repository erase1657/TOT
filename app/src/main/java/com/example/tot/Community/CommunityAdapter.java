package com.example.tot.Community;

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

import de.hdodenhof.circleimageview.CircleImageView;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {

    private List<CommunityPostDTO> posts;
    private OnPostClickListener listener;

    public interface OnPostClickListener {
        void onPostClick(CommunityPostDTO post, int position);
    }

    public CommunityAdapter(List<CommunityPostDTO> posts, OnPostClickListener listener) {
        this.posts = posts != null ? posts : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityPostDTO post = posts.get(position);
        holder.bind(post, position);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    /**
     * 데이터 업데이트
     */
    public void updateData(List<CommunityPostDTO> newPosts) {
        this.posts.clear();
        if (newPosts != null) {
            this.posts.addAll(newPosts);
        }
        notifyDataSetChanged();
    }

    /**
     * 데이터 추가 (페이지네이션)
     */
    public void addData(List<CommunityPostDTO> newPosts) {
        if (newPosts != null && !newPosts.isEmpty()) {
            int startPosition = this.posts.size();
            this.posts.addAll(newPosts);
            notifyItemRangeInserted(startPosition, newPosts.size());
        }
    }

    /**
     * 데이터 초기화
     */
    public void clearData() {
        this.posts.clear();
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgProfile;
        TextView txtUserName;
        TextView txtPostTitle;
        ImageView imgPostPhoto;
        ImageView imgHeart;
        TextView txtHeartCount;
        ImageView imgComment;
        TextView txtCommentCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.img_profile);
            txtUserName = itemView.findViewById(R.id.txt_user_name);
            txtPostTitle = itemView.findViewById(R.id.txt_post_title);
            imgPostPhoto = itemView.findViewById(R.id.img_post_photo);
            imgHeart = itemView.findViewById(R.id.img_heart);
            txtHeartCount = itemView.findViewById(R.id.txt_heart_count);
            imgComment = itemView.findViewById(R.id.img_comment);
            txtCommentCount = itemView.findViewById(R.id.txt_comment_count);
        }

        public void bind(CommunityPostDTO post, int position) {
            // 프로필 이미지
            imgProfile.setImageResource(post.getUserProfileImage());

            // 사용자 이름
            txtUserName.setText(post.getUserName());

            // 게시글 제목
            txtPostTitle.setText(post.getTitle());

            // 게시글 이미지
            imgPostPhoto.setImageResource(post.getPostImage());

            // 좋아요 아이콘 및 수
            updateHeartIcon(post.isLiked());
            txtHeartCount.setText(formatCount(post.getHeartCount()));

            // 댓글 수
            txtCommentCount.setText(post.getCommentCount() + "개");

            // 하트 클릭 이벤트
            imgHeart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    post.toggleLike();
                    updateHeartIcon(post.isLiked());
                    txtHeartCount.setText(formatCount(post.getHeartCount()));
                }
            });

            // 게시글 클릭 이벤트
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onPostClick(post, position);
                    }
                }
            });
        }

        /**
         * 하트 아이콘 업데이트
         */
        private void updateHeartIcon(boolean isLiked) {
            if (isLiked) {
                imgHeart.setImageResource(R.drawable.ic_heart_c);
            } else {
                imgHeart.setImageResource(R.drawable.ic_heart);
            }
        }

        /**
         * 숫자를 한국어 형식으로 변환 (10.9만, 1.2천 등)
         */
        private String formatCount(int count) {
            if (count >= 10000) {
                double formatted = count / 10000.0;
                if (formatted == (int) formatted) {
                    return String.format("%d만", (int) formatted);
                } else {
                    return String.format("%.1f만", formatted);
                }
            } else if (count >= 1000) {
                double formatted = count / 1000.0;
                if (formatted == (int) formatted) {
                    return String.format("%d천", (int) formatted);
                } else {
                    return String.format("%.1f천", formatted);
                }
            } else {
                return String.valueOf(count);
            }
        }
    }
}