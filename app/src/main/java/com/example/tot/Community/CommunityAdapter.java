package com.example.tot.Community;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tot.Follow.FollowActionHelper;
import com.example.tot.MyPage.UserProfileActivity;
import com.example.tot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommunityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

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
     * ✅ 사용자 검색 결과 ViewHolder
     */
    class UserViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgProfile;
        TextView tvUserName;
        TextView tvNickname;
        TextView btnFollow;

        boolean isFollowing = false;
        String currentUserId;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.img_profile);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvNickname = itemView.findViewById(R.id.tv_nickname);
            btnFollow = itemView.findViewById(R.id.btn_follow);

            // 불필요한 요소 숨기기
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

            // 프로필 이미지
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.ic_profile_default)
                        .error(R.drawable.ic_profile_default)
                        .into(imgProfile);
            } else {
                imgProfile.setImageResource(R.drawable.ic_profile_default);
            }

            // ✅ 팔로우 상태 로드
            loadFollowStatus(user.getUserId());

            // 클릭 이벤트
            View.OnClickListener profileClickListener = v -> {
                Intent intent = new Intent(itemView.getContext(), UserProfileActivity.class);
                intent.putExtra("userId", user.getUserId());
                itemView.getContext().startActivity(intent);
            };

            imgProfile.setOnClickListener(profileClickListener);
            tvUserName.setOnClickListener(profileClickListener);

            // ✅ 팔로우 버튼 클릭
            btnFollow.setOnClickListener(v -> {
                if (isFollowing) {
                    performUnfollow(user.getUserId(), position);
                } else {
                    performFollow(user.getUserId(), position);
                }
            });
        }

        /**
         * ✅ 팔로우 상태 로드
         */
        private void loadFollowStatus(String targetUserId) {
            if (mAuth.getCurrentUser() == null) {
                updateFollowButton(false);
                return;
            }

            String myUid = mAuth.getCurrentUser().getUid();

            db.collection("user")
                    .document(myUid)
                    .collection("following")
                    .document(targetUserId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        isFollowing = doc.exists();
                        updateFollowButton(isFollowing);
                    })
                    .addOnFailureListener(e -> {
                        updateFollowButton(false);
                    });
        }

        /**
         * ✅ 팔로우 버튼 UI 업데이트 (FollowAdapter와 동일한 스타일)
         */
        private void updateFollowButton(boolean following) {
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setCornerRadius(dpToPx(10));

            if (following) {
                btnFollow.setText("팔로우 중");
                btnBg.setColor(Color.parseColor("#E0E0E0"));
                btnFollow.setTextColor(Color.parseColor("#666666"));
            } else {
                btnFollow.setText("팔로우");
                btnBg.setColor(Color.parseColor("#575DFB"));
                btnFollow.setTextColor(Color.parseColor("#FFFFFF"));
            }

            btnFollow.setBackground(btnBg);
        }

        /**
         * ✅ 팔로우 실행
         */
        private void performFollow(String targetUserId, int position) {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(itemView.getContext(), "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
                return;
            }

            String myUid = mAuth.getCurrentUser().getUid();

            Map<String, Object> followData = new HashMap<>();
            followData.put("followedAt", System.currentTimeMillis());

            // 1. 내 following에 추가
            db.collection("user")
                    .document(myUid)
                    .collection("following")
                    .document(targetUserId)
                    .set(followData)
                    .addOnSuccessListener(aVoid -> {
                        // 2. 상대방 follower에 추가
                        db.collection("user")
                                .document(targetUserId)
                                .collection("follower")
                                .document(myUid)
                                .set(followData)
                                .addOnSuccessListener(aVoid2 -> {
                                    isFollowing = true;
                                    updateFollowButton(true);

                                    Toast.makeText(itemView.getContext(), "팔로우했습니다", Toast.LENGTH_SHORT).show();

                                    // 3. 팔로우 알림 전송
                                    FollowActionHelper.sendFollowNotification(targetUserId, myUid);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(itemView.getContext(), "팔로우 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    });
        }

        /**
         * ✅ 언팔로우 실행
         */
        private void performUnfollow(String targetUserId, int position) {
            if (mAuth.getCurrentUser() == null) return;

            String myUid = mAuth.getCurrentUser().getUid();

            // 1. 내 following에서 삭제
            db.collection("user")
                    .document(myUid)
                    .collection("following")
                    .document(targetUserId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // 2. 상대방 follower에서 삭제
                        db.collection("user")
                                .document(targetUserId)
                                .collection("follower")
                                .document(myUid)
                                .delete()
                                .addOnSuccessListener(aVoid2 -> {
                                    isFollowing = false;
                                    updateFollowButton(false);

                                    Toast.makeText(itemView.getContext(), "언팔로우했습니다", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(itemView.getContext(), "언팔로우 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    });
        }

        private int dpToPx(int dp) {
            float density = itemView.getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }
    }

    /**
     * "더보기" ViewHolder
     */
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

    /**
     * 게시글 ViewHolder (기존 유지)
     */
    class PostViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgProfile;
        TextView txtUserName;
        TextView txtPostTitle;
        ImageView imgPostPhoto;
        ImageView imgHeart;
        TextView txtHeartCount;
        ImageView imgComment;
        TextView txtCommentCount;

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
        }

        public void bind(CommunityPostDTO post, int position) {
            if (post == null) return;

            if (post.getUserProfileImage() != 0) {
                imgProfile.setImageResource(post.getUserProfileImage());
            } else {
                imgProfile.setImageResource(R.drawable.ic_profile_default);
            }

            txtUserName.setText(post.getUserName() != null ? post.getUserName() : "익명");
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

            imgProfile.setOnClickListener(v -> {
                String userId = post.getUserId();
                if (userId == null || userId.isEmpty()) {
                    Toast.makeText(itemView.getContext(),
                            "프로필 정보가 없는 사용자입니다 (더미 데이터)",
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
                            "프로필 정보가 없는 사용자입니다 (더미 데이터)",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(itemView.getContext(), UserProfileActivity.class);
                intent.putExtra("userId", userId);
                itemView.getContext().startActivity(intent);
            });

            imgHeart.setOnClickListener(v -> {
                post.toggleLike();
                updateHeartIcon(post.isLiked());
                txtHeartCount.setText(formatCount(post.getHeartCount()));
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