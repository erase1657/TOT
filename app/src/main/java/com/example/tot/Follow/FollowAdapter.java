package com.example.tot.Follow;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FollowAdapter extends RecyclerView.Adapter<FollowAdapter.ViewHolder> {

    private List<FollowUserDTO> users;
    private FollowListener listener;
    private boolean isMyProfile;        // 내 프로필 여부
    private boolean isFollowerMode;     // 팔로워 모드 여부

    public interface FollowListener {
        void onProfileClick(FollowUserDTO user);
        void onFollowClick(FollowUserDTO user, int position);
        void onRemoveFollower(FollowUserDTO user, int position);
        void onNicknameChanged(FollowUserDTO user, String newNickname, int position);
    }

    public FollowAdapter(List<FollowUserDTO> users, FollowListener listener,
                         boolean isMyProfile, boolean isFollowerMode) {
        this.users = users != null ? users : new ArrayList<>();
        this.listener = listener;
        this.isMyProfile = isMyProfile;
        this.isFollowerMode = isFollowerMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_follow_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < users.size()) {
            FollowUserDTO user = users.get(position);
            holder.bind(user, position);
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void updateData(List<FollowUserDTO> newUsers) {
        this.users.clear();
        if (newUsers != null) {
            this.users.addAll(newUsers);
        }
        notifyDataSetChanged();
    }

    public void setFollowerMode(boolean isFollowerMode) {
        this.isFollowerMode = isFollowerMode;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgProfile;
        TextView tvUserName;
        TextView tvFollowBack;
        TextView tvNickname;
        ImageView btnEditNickname;
        LinearLayout layoutNicknameEdit;
        EditText edtNickname;
        TextView btnSaveNickname;
        TextView btnFollow;
        ImageView btnMenu;

        boolean isEditingNickname = false;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.img_profile);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvFollowBack = itemView.findViewById(R.id.tv_follow_back);
            tvNickname = itemView.findViewById(R.id.tv_nickname);
            btnEditNickname = itemView.findViewById(R.id.btn_edit_nickname);
            layoutNicknameEdit = itemView.findViewById(R.id.layout_nickname_edit);
            edtNickname = itemView.findViewById(R.id.edt_nickname);
            btnSaveNickname = itemView.findViewById(R.id.btn_save_nickname);
            btnFollow = itemView.findViewById(R.id.btn_follow);
            btnMenu = itemView.findViewById(R.id.btn_menu);
        }

        public void bind(FollowUserDTO user, int position) {
            if (user == null) return;

            // 프로필 이미지
            if (user.getProfileImage() != 0) {
                imgProfile.setImageResource(user.getProfileImage());
            } else {
                imgProfile.setImageResource(R.drawable.ic_profile_default);
            }

            // 사용자 이름
            tvUserName.setText(user.getUserName());

            // Follow back 표시
            if (isFollowerMode && isMyProfile && !user.isFollowing()) {
                tvFollowBack.setVisibility(View.VISIBLE);
            } else {
                tvFollowBack.setVisibility(View.GONE);
            }

            // 별명 표시 + 수정 아이콘
            if (isMyProfile) {
                String nickname = user.getNickname();
                if (nickname != null && !nickname.trim().isEmpty()) {
                    tvNickname.setText(nickname);
                } else {
                    tvNickname.setText("별명 없음");
                }
                tvNickname.setVisibility(View.VISIBLE);
                btnEditNickname.setVisibility(View.VISIBLE);
            } else {
                tvNickname.setVisibility(View.GONE);
                btnEditNickname.setVisibility(View.GONE);
            }

            // 팔로우 버튼 설정
            updateFollowButton(user);

            // 점 세개 메뉴
            if (isFollowerMode && isMyProfile) {
                btnMenu.setVisibility(View.VISIBLE);
            } else {
                btnMenu.setVisibility(View.GONE);
            }

            // 클릭 이벤트
            setupClickListeners(user, position);
        }

        private void updateFollowButton(FollowUserDTO user) {
            // ✅ 팔로우 버튼 라운드 처리
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setCornerRadius(dpToPx(10));

            if (isMyProfile) {
                if (isFollowerMode) {
                    if (user.isFollowing()) {
                        btnFollow.setText("팔로우 중");
                        btnBg.setColor(0xFFE0E0E0);
                        btnFollow.setTextColor(0xFF666666);
                    } else {
                        btnFollow.setText("맞 팔로우");
                        btnBg.setColor(0xFF575DFB);
                        btnFollow.setTextColor(0xFFFFFFFF);
                    }
                } else {
                    // ✅ 팔로잉 모드: 언팔하면 "팔로우"로 변경
                    if (user.isFollowing()) {
                        btnFollow.setText("팔로우 중");
                        btnBg.setColor(0xFFE0E0E0);
                        btnFollow.setTextColor(0xFF666666);
                    } else {
                        btnFollow.setText("팔로우");
                        btnBg.setColor(0xFF575DFB);
                        btnFollow.setTextColor(0xFFFFFFFF);
                    }
                }
            } else {
                // 친구 프로필
                if (user.isFollower() && user.isFollowing()) {
                    btnFollow.setText("팔로우 중");
                    btnBg.setColor(0xFFE0E0E0);
                    btnFollow.setTextColor(0xFF666666);
                } else if (user.isFollower()) {
                    btnFollow.setText("맞 팔로우");
                    btnBg.setColor(0xFF575DFB);
                    btnFollow.setTextColor(0xFFFFFFFF);
                } else {
                    btnFollow.setText("팔로우");
                    btnBg.setColor(0xFF575DFB);
                    btnFollow.setTextColor(0xFFFFFFFF);
                }
            }

            btnFollow.setBackground(btnBg);
        }

        private void setupClickListeners(FollowUserDTO user, int position) {
            // 프로필 클릭
            imgProfile.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProfileClick(user);
                }
            });

            // ✅ 수정 아이콘 클릭
            btnEditNickname.setOnClickListener(v -> {
                if (isMyProfile) {
                    enterNicknameEditMode(user);
                }
            });

            // 별명 저장
            btnSaveNickname.setOnClickListener(v -> {
                saveNickname(user, position);
            });

            // 엔터키로 저장
            edtNickname.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveNickname(user, position);
                    return true;
                }
                return false;
            });

            // 팔로우 버튼
            btnFollow.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFollowClick(user, position);
                    updateFollowButton(user);
                }
            });

            // 메뉴 버튼
            btnMenu.setOnClickListener(v -> {
                showPopupMenu(v, user, position);
            });
        }

        private void enterNicknameEditMode(FollowUserDTO user) {
            isEditingNickname = true;
            tvNickname.setVisibility(View.GONE);
            btnEditNickname.setVisibility(View.GONE);
            layoutNicknameEdit.setVisibility(View.VISIBLE);

            String currentNickname = user.getNickname();
            edtNickname.setText(currentNickname != null ? currentNickname : "");
            edtNickname.requestFocus();
            edtNickname.setSelection(edtNickname.getText().length());

            InputMethodManager imm = (InputMethodManager) itemView.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(edtNickname, InputMethodManager.SHOW_IMPLICIT);
            }
        }

        private void saveNickname(FollowUserDTO user, int position) {
            String newNickname = edtNickname.getText().toString().trim();

            isEditingNickname = false;
            layoutNicknameEdit.setVisibility(View.GONE);

            if (newNickname.isEmpty()) {
                tvNickname.setText("별명 없음");
            } else {
                tvNickname.setText(newNickname);
                user.setNickname(newNickname);
            }

            tvNickname.setVisibility(View.VISIBLE);
            btnEditNickname.setVisibility(View.VISIBLE);

            InputMethodManager imm = (InputMethodManager) itemView.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(edtNickname.getWindowToken(), 0);
            }

            if (listener != null) {
                listener.onNicknameChanged(user, newNickname, position);
            }
        }

        private void showPopupMenu(View view, FollowUserDTO user, int position) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.getMenuInflater().inflate(R.menu.menu_follower_options, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_remove_follower) {
                    if (listener != null) {
                        listener.onRemoveFollower(user, position);
                    }
                    return true;
                }
                return false;
            });

            popup.show();
        }

        private int dpToPx(int dp) {
            float density = itemView.getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }
    }
}