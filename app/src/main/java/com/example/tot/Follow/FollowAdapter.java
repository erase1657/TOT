package com.example.tot.Follow;

import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.example.tot.MyPage.UserProfileActivity;
import com.example.tot.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FollowAdapter extends RecyclerView.Adapter<FollowAdapter.ViewHolder> {

    private List<FollowUserDTO> users;
    private FollowListener listener;
    private boolean isMyProfile;
    private boolean isFollowerMode;

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

            tvUserName.setText(user.getUserName());

            // ✅ Firestore에서 실시간 프로필 이미지 로드
            loadProfileImageFromFirestore(user.getUserId());

            if (isFollowerMode && isMyProfile && !user.isFollowing()) {
                tvFollowBack.setVisibility(View.VISIBLE);
            } else {
                tvFollowBack.setVisibility(View.GONE);
            }

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

            FollowButtonHelper.updateFollowButton(btnFollow, user.isFollowing(), user.isFollower());

            if (isFollowerMode && isMyProfile) {
                btnMenu.setVisibility(View.VISIBLE);
            } else {
                btnMenu.setVisibility(View.GONE);
            }

            setupClickListeners(user, position);
        }

        /**
         * ✅ Firestore에서 실시간 프로필 이미지 URL 로드
         */
        private void loadProfileImageFromFirestore(String userId) {
            if (userId == null || userId.isEmpty()) {
                imgProfile.setImageResource(R.drawable.ic_profile_default);
                return;
            }

            FirebaseFirestore.getInstance()
                    .collection("user")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String profileUrl = doc.getString("profileImageUrl");
                            if (profileUrl != null && !profileUrl.isEmpty()) {
                                Glide.with(itemView.getContext())
                                        .load(profileUrl)
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                                        .skipMemoryCache(true)
                                        .signature(new ObjectKey(System.currentTimeMillis()))
                                        .placeholder(R.drawable.ic_profile_default)
                                        .error(R.drawable.ic_profile_default)
                                        .into(imgProfile);
                            } else {
                                imgProfile.setImageResource(R.drawable.ic_profile_default);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        imgProfile.setImageResource(R.drawable.ic_profile_default);
                    });
        }

        private void setupClickListeners(FollowUserDTO user, int position) {
            imgProfile.setOnClickListener(v -> {
                String userId = user.getUserId();
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

            tvUserName.setOnClickListener(v -> {
                String userId = user.getUserId();
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

            btnEditNickname.setOnClickListener(v -> {
                if (isMyProfile) {
                    enterNicknameEditMode(user);
                }
            });

            btnSaveNickname.setOnClickListener(v -> {
                saveNickname(user, position);
            });

            edtNickname.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveNickname(user, position);
                    return true;
                }
                return false;
            });

            btnFollow.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFollowClick(user, position);
                    FollowButtonHelper.updateFollowButton(btnFollow, user.isFollowing(), user.isFollower());
                }
            });

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
    }
}