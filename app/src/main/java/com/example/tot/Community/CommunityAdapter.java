package com.example.tot.Community;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tot.MyPage.UserProfileActivity;
import com.example.tot.R;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommunityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_POST = 1;
    private static final int VIEW_TYPE_MORE_USERS = 2;

    private List<Object> items; // ✅ 사용자 + 게시글 혼합 리스트
    private OnPostClickListener postClickListener;
    private OnMoreUsersClickListener moreUsersClickListener;
    private boolean showUsers; // ✅ 사용자 검색 결과 표시 여부

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
    }

    /**
     * ✅ 사용자 검색 결과와 게시글을 함께 표시
     * @param posts 게시글 목록
     * @param users 사용자 검색 결과 (최대 3명 또는 전체)
     * @param showUsers 사용자 표시 여부
     * @param showMoreButton 더보기 버튼 표시 여부 (4명 이상일 때만)
     */
    public void updateDataWithUsers(List<CommunityPostDTO> posts, List<CommunityFragment.UserSearchResult> users, boolean showUsers, boolean showMoreButton) {
        this.items.clear();
        this.showUsers = showUsers;

        if (showUsers && !users.isEmpty()) {
            // ✅ 사용자 검색 결과 추가
            this.items.addAll(users);

            // ✅ "더보기" 버튼 추가 (4명 이상일 때만)
            if (showMoreButton) {
                this.items.add("MORE_USERS");
            }
        }

        // 게시글 추가
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
            ((UserViewHolder) holder).bind((CommunityFragment.UserSearchResult) item);
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

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.img_profile);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvNickname = itemView.findViewById(R.id.tv_nickname);
            btnFollow = itemView.findViewById(R.id.btn_follow);

            // ✅ 불필요한 요소 숨기기
            itemView.findViewById(R.id.tv_follow_back).setVisibility(View.GONE);
            itemView.findViewById(R.id.btn_edit_nickname).setVisibility(View.GONE);
            itemView.findViewById(R.id.layout_nickname_edit).setVisibility(View.GONE);
            itemView.findViewById(R.id.btn_menu).setVisibility(View.GONE);
        }

        public void bind(CommunityFragment.UserSearchResult user) {
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

            // 팔로우 버튼 스타일링
            btnFollow.setText("프로필 보기");
            btnFollow.setBackgroundResource(R.drawable.button_style1);
            btnFollow.setTextColor(0xFFFFFFFF);

            // 클릭 이벤트
            View.OnClickListener profileClickListener = v -> {
                Intent intent = new Intent(itemView.getContext(), UserProfileActivity.class);
                intent.putExtra("userId", user.getUserId());
                itemView.getContext().startActivity(intent);
            };

            imgProfile.setOnClickListener(profileClickListener);
            tvUserName.setOnClickListener(profileClickListener);
            btnFollow.setOnClickListener(profileClickListener);
        }
    }

    /**
     * ✅ "더보기" ViewHolder
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