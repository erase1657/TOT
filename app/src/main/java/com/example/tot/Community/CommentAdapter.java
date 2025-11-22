package com.example.tot.Community;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tot.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final Context context;
    private final List<CommentDTO> commentList;
    private final String currentUserId;
    private OnDeleteButtonClickListener onDeleteButtonClickListener;

    public interface OnDeleteButtonClickListener {
        void onDeleteClick(CommentDTO comment);
    }

    public void setOnDeleteButtonClickListener(OnDeleteButtonClickListener listener) {
        this.onDeleteButtonClickListener = listener;
    }

    public CommentAdapter(Context context, List<CommentDTO> commentList) {
        this.context = context;
        this.commentList = commentList;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        CommentDTO comment = commentList.get(position);
        holder.bind(comment);

        if (currentUserId != null && currentUserId.equals(comment.getUid())) {
            holder.deleteTextView.setVisibility(View.VISIBLE);
            holder.deleteTextView.setOnClickListener(v -> {
                if (onDeleteButtonClickListener != null) {
                    onDeleteButtonClickListener.onDeleteClick(comment);
                }
            });
        } else {
            holder.deleteTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImageView;
        TextView nicknameTextView;
        TextView timestampTextView;
        TextView contentTextView;
        TextView deleteTextView;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.iv_profile);
            nicknameTextView = itemView.findViewById(R.id.tv_nickname);
            timestampTextView = itemView.findViewById(R.id.tv_timestamp);
            contentTextView = itemView.findViewById(R.id.tv_comment_content);
            deleteTextView = itemView.findViewById(R.id.tv_delete_comment);
        }

        void bind(CommentDTO comment) {
            nicknameTextView.setText(comment.getNickname());
            contentTextView.setText(comment.getContent());

            // 프로필 이미지 로드
            Glide.with(itemView.getContext())
                    .load(comment.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile_default)
                    .into(profileImageView);

            // 타임스탬프 변환
            if (comment.getTimestamp() != null) {
                timestampTextView.setText(formatTimestamp(comment.getTimestamp()));
            }
        }

        private String formatTimestamp(Timestamp timestamp) {
            long currentTime = System.currentTimeMillis();
            long commentTime = timestamp.toDate().getTime();
            long diff = currentTime - commentTime;

            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            if (minutes < 1) {
                return "방금 전";
            }
            if (minutes < 60) {
                return minutes + "분 전";
            }
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            if (hours < 24) {
                return hours + "시간 전";
            }
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            if (days < 7) {
                return days + "일 전";
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
            return sdf.format(new Date(commentTime));
        }
    }
}