package com.example.tot.Community;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.tot.Follow.FollowButtonHelper;
import com.example.tot.MyPage.UserProfileActivity;
import com.example.tot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommunityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "CommunityAdapter";
    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_POST = 1;
    private static final int VIEW_TYPE_MORE_USERS = 2;

    private List<Object> items;
    private OnPostClickListener postClickListener;
    private OnMoreUsersClickListener moreUsersClickListener;
    private boolean showUsers;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public interface OnPostClickListener {
        void onPostClick(CommunityPostDTO post, int position);
    }

    public interface OnMoreUsersClickListener {
        void onMoreUsersClick();
    }

    public CommunityAdapter(List<CommunityPostDTO> posts, OnPostClickListener postClickListener, OnMoreUsersClickListener moreUsersClickListener) {
        this.items = new ArrayList<>(posts);
        this.postClickListener = postClickListener;
        this.moreUsersClickListener = moreUsersClickListener;
        this.showUsers = false;
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public void updateDataWithUsers(List<CommunityPostDTO> posts, List<CommunityFragment.UserSearchResult> users, boolean showUsers, boolean showMoreButton) {
        this.items.clear();
        this.showUsers = showUsers;

        if (showUsers && !users.isEmpty()) {
            this.items.addAll(users);

            if (showMoreButton) {
                this.items.add("MORE_USERS");
            }
        }

        if (posts != null) {
            this.items.addAll(posts);
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof CommunityFragment.UserSearchResult) {
            return VIEW_TYPE_USER;
        } else if (item instanceof String && item.equals("MORE_USERS")) {
            return VIEW_TYPE_MORE_USERS;
        } else {
            return VIEW_TYPE_POST;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_follow_user, parent, false);
            return new UserViewHolder(view);
        } else if (viewType == VIEW_TYPE_MORE_USERS) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_more_users, parent, false);
            return new MoreUsersViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_community, parent, false);
            return new PostViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);

        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind((CommunityFragment.UserSearchResult) item, position);
        } else if (holder instanceof MoreUsersViewHolder) {
            ((MoreUsersViewHolder) holder).bind();
        } else if (holder instanceof PostViewHolder) {
            ((PostViewHolder) holder).bind((CommunityPostDTO) item, position);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateData(List<CommunityPostDTO> newPosts) {
        updateDataWithUsers(newPosts, new ArrayList<>(), false, false);
    }

    public void addData(List<CommunityPostDTO> newPosts) {
        if (newPosts != null && !newPosts.isEmpty()) {
            int startPosition = this.items.size();
            this.items.addAll(newPosts);
            notifyItemRangeInserted(startPosition, newPosts.size());
        }
    }

    /**
     * ✅ 프로필 이미지 로드 헬퍼 메서드
     * - 캐시 활성화로 깜빡임 방지
     * - 메모리 캐시 사용
     */
    private void loadProfileImage(ImageView imageView, String profileUrl) {
        if (profileUrl != null && !profileUrl.isEmpty()) {
            Glide.with(imageView.getContext())
                    .load(profileUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // ✅ 디스크 캐시 활성화
                    .placeholder(R.drawable.ic_profile_default)
                    .error(R.drawable.ic_profile_default)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_profile_default);
        }
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgProfile;
        TextView tvUserName;
        TextView tvNickname;
        TextView btnFollow;

        boolean isFollowing = false;
        boolean isFollower = false;
        String currentUserId;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.img_profile);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvNickname = itemView.findViewById(R.id.tv_nickname);
            btnFollow = itemView.findViewById(R.id.btn_follow);

            itemView.findViewById(R.id.tv_follow_back).setVisibility(View.GONE);
            itemView.findViewById(R.id.btn_edit_nickname).setVisibility(View.GONE);
            itemView.findViewById(R.id.layout_nickname_edit).setVisibility(View.GONE);
            itemView.findViewById(R.id.btn_menu).setVisibility(View.GONE);
        }

        public void bind(CommunityFragment.UserSearchResult user, int position) {
            currentUserId = user.getUserId();

            tvUserName.setText(user.getNickname() != null ? user.getNickname() : "사용자");

            String statusMsg = user.getStatusMessage();
            if (statusMsg != null && !statusMsg.isEmpty()) {
                tvNickname.setText(statusMsg);
                tvNickname.setVisibility(View.VISIBLE);
            } else {
                tvNickname.setVisibility(View.GONE);
            }

            // ✅ 캐시 활성화된 이미지 로드
            loadProfileImage(imgProfile, user.getProfileImageUrl());

            FollowButtonHelper.checkFollowStatus(user.getUserId(), (following, follower) -> {
                isFollowing = following;
                isFollower = follower;
                FollowButtonHelper.updateFollowButton(btnFollow, isFollowing, isFollower);
            });

            View.OnClickListener profileClickListener = v -> {
                Intent intent = new Intent(itemView.getContext(), UserProfileActivity.class);
                intent.putExtra("userId", user.getUserId());
                itemView.getContext().startActivity(intent);
            };

            imgProfile.setOnClickListener(profileClickListener);
            tvUserName.setOnClickListener(profileClickListener);

            btnFollow.setOnClickListener(v -> {
                FollowButtonHelper.handleFollowButtonClick(
                        itemView.getContext(),
                        user.getUserId(),
                        isFollowing,
                        isFollower,
                        new FollowButtonHelper.FollowActionCallback() {
                            @Override
                            public void onSuccess(boolean nowFollowing) {
                                isFollowing = nowFollowing;
                                FollowButtonHelper.updateFollowButton(btnFollow, isFollowing, isFollower);
                            }

                            @Override
                            public void onFailure(String message) {
                                Toast.makeText(itemView.getContext(), message, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            });
        }
    }

    class MoreUsersViewHolder extends RecyclerView.ViewHolder {
        TextView tvMoreUsers;

        public MoreUsersViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMoreUsers = itemView.findViewById(R.id.tv_more_users);
        }

        public void bind() {
            itemView.setOnClickListener(v -> {
                if (moreUsersClickListener != null) {
                    moreUsersClickListener.onMoreUsersClick();
                }
            });
        }
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgProfile;
        TextView txtUserName;
        TextView txtPostTitle;
        ImageView imgPostPhoto;
        ImageView imgHeart;
        TextView txtHeartCount;
        ImageView imgComment;
        TextView txtCommentCount;
        TextView btnFollow;

        boolean isFollowing = false;
        boolean isFollower = false;
        String currentPostAuthorId;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.img_profile);
            txtUserName = itemView.findViewById(R.id.txt_user_name);
            txtPostTitle = itemView.findViewById(R.id.txt_post_title);
            imgPostPhoto = itemView.findViewById(R.id.img_post_photo);
            imgHeart = itemView.findViewById(R.id.img_heart);
            txtHeartCount = itemView.findViewById(R.id.txt_heart_count);
            imgComment = itemView.findViewById(R.id.img_comment);
            txtCommentCount = itemView.findViewById(R.id.txt_comment_count);
            btnFollow = itemView.findViewById(R.id.btn_follow);
        }

        public void bind(CommunityPostDTO post, int position) {
            if (post == null) return;

            currentPostAuthorId = post.getUserId();
            String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

            // ✅ 캐시 활성화된 이미지 로드
            loadProfileImage(imgProfile, post.getProfileImageUrl());

            txtUserName.setText(post.getUserName() != null ? post.getUserName() : "사용자");
            txtPostTitle.setText(post.getTitle() != null ? post.getTitle() : "");

            if (post.getPostImage() != 0) {
                imgPostPhoto.setImageResource(post.getPostImage());
                imgPostPhoto.setVisibility(View.VISIBLE);
            } else {
                imgPostPhoto.setVisibility(View.GONE);
            }

            updateHeartIcon(post.isLiked());
            txtHeartCount.setText(formatCount(post.getHeartCount()));
            txtCommentCount.setText(post.getCommentCount() + "개");

            if (currentUid != null && currentUid.equals(currentPostAuthorId)) {
                btnFollow.setVisibility(View.GONE);
            } else {
                btnFollow.setVisibility(View.VISIBLE);

                FollowButtonHelper.checkFollowStatus(currentPostAuthorId, (following, follower) -> {
                    isFollowing = following;
                    isFollower = follower;
                    FollowButtonHelper.updateFollowButton(btnFollow, isFollowing, isFollower);
                });

                btnFollow.setOnClickListener(v -> {
                    FollowButtonHelper.handleFollowButtonClick(
                            itemView.getContext(),
                            currentPostAuthorId,
                            isFollowing,
                            isFollower,
                            new FollowButtonHelper.FollowActionCallback() {
                                @Override
                                public void onSuccess(boolean nowFollowing) {
                                    isFollowing = nowFollowing;
                                    FollowButtonHelper.updateFollowButton(btnFollow, isFollowing, isFollower);
                                }

                                @Override
                                public void onFailure(String message) {
                                    Toast.makeText(itemView.getContext(), message, Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                });
            }

            imgProfile.setOnClickListener(v -> {
                String userId = post.getUserId();
                if (userId == null || userId.isEmpty()) {
                    Toast.makeText(itemView.getContext(),
                            "프로필 정보가 없는 사용자입니다",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(itemView.getContext(), UserProfileActivity.class);
                intent.putExtra("userId", userId);
                itemView.getContext().startActivity(intent);
            });

            txtUserName.setOnClickListener(v -> {
                String userId = post.getUserId();
                if (userId == null || userId.isEmpty()) {
                    Toast.makeText(itemView.getContext(),
                            "프로필 정보가 없는 사용자입니다",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(itemView.getContext(), UserProfileActivity.class);
                intent.putExtra("userId", userId);
                itemView.getContext().startActivity(intent);
            });

            imgHeart.setOnClickListener(v -> {
                if (currentUid == null) {
                    Toast.makeText(itemView.getContext(), "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                String postId = post.getPostId();
                if (postId == null || postId.isEmpty()) {
                    Toast.makeText(itemView.getContext(), "게시글 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                imgHeart.setEnabled(false);

                if (post.isLiked()) {
                    db.collection("public")
                            .document("community")
                            .collection("posts")
                            .document(postId)
                            .collection("likes")
                            .document(currentUid)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                db.collection("public")
                                        .document("community")
                                        .collection("posts")
                                        .document(postId)
                                        .update("heartCount", FieldValue.increment(-1))
                                        .addOnSuccessListener(aVoid2 -> {
                                            post.setLiked(false);
                                            post.setHeartCount(post.getHeartCount() - 1);
                                            updateHeartIcon(false);
                                            txtHeartCount.setText(formatCount(post.getHeartCount()));
                                            imgHeart.setEnabled(true);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "❌ 좋아요 카운트 감소 실패", e);
                                            Toast.makeText(itemView.getContext(), "좋아요 취소 실패", Toast.LENGTH_SHORT).show();
                                            imgHeart.setEnabled(true);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ 좋아요 삭제 실패", e);
                                Toast.makeText(itemView.getContext(), "좋아요 취소 실패", Toast.LENGTH_SHORT).show();
                                imgHeart.setEnabled(true);
                            });
                } else {
                    Map<String, Object> likeData = new HashMap<>();
                    likeData.put("userId", currentUid);
                    likeData.put("timestamp", System.currentTimeMillis());

                    db.collection("public")
                            .document("community")
                            .collection("posts")
                            .document(postId)
                            .collection("likes")
                            .document(currentUid)
                            .set(likeData)
                            .addOnSuccessListener(aVoid -> {
                                db.collection("public")
                                        .document("community")
                                        .collection("posts")
                                        .document(postId)
                                        .update("heartCount", FieldValue.increment(1))
                                        .addOnSuccessListener(aVoid2 -> {
                                            post.setLiked(true);
                                            post.setHeartCount(post.getHeartCount() + 1);
                                            updateHeartIcon(true);
                                            txtHeartCount.setText(formatCount(post.getHeartCount()));
                                            imgHeart.setEnabled(true);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "❌ 좋아요 카운트 증가 실패", e);
                                            Toast.makeText(itemView.getContext(), "좋아요 실패", Toast.LENGTH_SHORT).show();
                                            imgHeart.setEnabled(true);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ 좋아요 추가 실패", e);
                                Toast.makeText(itemView.getContext(), "좋아요 실패", Toast.LENGTH_SHORT).show();
                                imgHeart.setEnabled(true);
                            });
                }
            });

            itemView.setOnClickListener(v -> {
                if (postClickListener != null) {
                    postClickListener.onPostClick(post, getAdapterPosition());
                }
            });
        }

        private void updateHeartIcon(boolean isLiked) {
            if (isLiked) {
                imgHeart.setImageResource(R.drawable.ic_heart_c);
            } else {
                imgHeart.setImageResource(R.drawable.ic_heart);
            }
        }

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