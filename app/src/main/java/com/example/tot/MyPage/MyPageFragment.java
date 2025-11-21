package com.example.tot.MyPage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tot.Authentication.LoginActivity;
import com.example.tot.Authentication.UserDTO;
import com.example.tot.Follow.FollowActionHelper;
import com.example.tot.Follow.FollowActivity;
import com.example.tot.R;
import com.example.tot.Schedule.ScheduleDTO;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class MyPageFragment extends Fragment {

    private static final String TAG = "MyPageFragment";
    private static final String ARG_USER_ID = "userId";

    private CircleImageView imgProfile;
    private ImageView imgBackground;
    private ImageView btnLogout;
    private ImageView btnBack;
    private ImageView btnEdit;
    private TextView btnFollowButton;
    private TextView tvName;
    private TextView tvStatusMessage;
    private TextView tvLocation;
    private TextView tvFollowersCount;
    private TextView tvFollowingCount;
    private TextView tvPostsCount;
    private TextView tvTravelTitle;
    private LinearLayout followerSection;
    private LinearLayout followingSection;
    private RecyclerView rvMyTravels;
    private LinearLayout layoutNoTravel;

    private MyPageScheduleAdapter scheduleAdapter;
    private List<ScheduleDTO> scheduleList;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private boolean isEditMode = false;
    private EditText etNameEdit;
    private EditText etStatusEdit;
    private EditText etLocationEdit;

    private String originalName;
    private String originalStatus;
    private String originalLocation;

    // 팔로워/팔로잉 수 (Firestore에서 로드)
    private int followerCount = 0;
    private int followingCount = 0;
    private boolean isCountsLoaded = false;

    private ActivityResultLauncher<Intent> followActivityLauncher;

    private String targetUserId;
    private boolean isMyProfile = true;
    private boolean isFollowing = false;
    private boolean isFollower = false;

    public MyPageFragment() {
        super(R.layout.fragment_mypage);
    }

    public static MyPageFragment newInstance(String userId) {
        MyPageFragment fragment = new MyPageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            targetUserId = getArguments().getString(ARG_USER_ID);
        }

        followActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        followerCount = data.getIntExtra("followerCount", followerCount);
                        followingCount = data.getIntExtra("followingCount", followingCount);
                        updateFollowCounts();
                    }
                }
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews(view);
        determineProfileMode();

        // 팔로우 카운트를 먼저 로드한 후 프로필 로드
        loadFollowCounts(() -> {
            loadProfileData();
        });

        loadTravelHistory();
        setupClickListeners();
    }

    private void initViews(View view) {
        imgProfile = view.findViewById(R.id.img_profile);
        imgBackground = view.findViewById(R.id.img_background);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnBack = view.findViewById(R.id.btn_back);
        btnEdit = view.findViewById(R.id.btn_edit);
        btnFollowButton = view.findViewById(R.id.btn_follow_dynamic);
        tvName = view.findViewById(R.id.tv_name);
        tvStatusMessage = view.findViewById(R.id.tv_status_message);
        tvLocation = view.findViewById(R.id.tv_location);
        tvFollowersCount = view.findViewById(R.id.tv_followers_count);
        tvFollowingCount = view.findViewById(R.id.tv_following_count);
        tvPostsCount = view.findViewById(R.id.tv_posts_count);
        tvTravelTitle = view.findViewById(R.id.tv_travel_title);
        rvMyTravels = view.findViewById(R.id.rv_my_travels);
        layoutNoTravel = view.findViewById(R.id.layout_no_travel);

        followerSection = (LinearLayout) tvFollowersCount.getParent();
        followingSection = (LinearLayout) tvFollowingCount.getParent();
    }

    private void determineProfileMode() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            isMyProfile = false;
            targetUserId = null;
        } else if (targetUserId == null || targetUserId.isEmpty() ||
                targetUserId.equals(currentUser.getUid())) {
            isMyProfile = true;
            targetUserId = currentUser.getUid();
        } else {
            isMyProfile = false;
        }

        updateUIForProfileMode();
    }

    private void updateUIForProfileMode() {
        if (isMyProfile) {
            btnLogout.setVisibility(View.VISIBLE);
            btnBack.setVisibility(View.GONE);
            btnEdit.setVisibility(View.VISIBLE);
            btnFollowButton.setVisibility(View.GONE);
            tvTravelTitle.setText("나의 여행 기록");

            followerSection.setEnabled(true);
            followingSection.setEnabled(true);
        } else {
            btnLogout.setVisibility(View.GONE);
            btnBack.setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.GONE);
            btnFollowButton.setVisibility(View.VISIBLE);
            tvTravelTitle.setText("여행 기록");

            followerSection.setEnabled(false);
            followingSection.setEnabled(false);
        }
    }

    /**
     * ✅ 팔로워/팔로잉 수를 Firestore에서 먼저 로드
     */
    private void loadFollowCounts(Runnable onComplete) {
        if (targetUserId == null || targetUserId.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        // 팔로워 수 로드
        db.collection("user")
                .document(targetUserId)
                .collection("follower")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    followerCount = querySnapshot.size();
                    Log.d(TAG, "✅ 팔로워 수: " + followerCount);
                    checkCountsLoadedAndUpdate(onComplete);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 팔로워 수 로드 실패", e);
                    checkCountsLoadedAndUpdate(onComplete);
                });

        // 팔로잉 수 로드
        db.collection("user")
                .document(targetUserId)
                .collection("following")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    followingCount = querySnapshot.size();
                    Log.d(TAG, "✅ 팔로잉 수: " + followingCount);
                    checkCountsLoadedAndUpdate(onComplete);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 팔로잉 수 로드 실패", e);
                    checkCountsLoadedAndUpdate(onComplete);
                });
    }

    /**
     * ✅ 두 카운트 로딩이 모두 완료되면 UI 업데이트 및 콜백 실행
     */
    private void checkCountsLoadedAndUpdate(Runnable onComplete) {
        if (!isCountsLoaded) {
            isCountsLoaded = true;
            updateFollowCounts();
            if (onComplete != null) onComplete.run();
        }
    }

    private void loadProfileData() {
        if (targetUserId == null || targetUserId.isEmpty()) {
            Toast.makeText(getContext(), "사용자 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("user")
                .document(targetUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserDTO user = documentSnapshot.toObject(UserDTO.class);
                        if (user != null) {
                            displayUserProfile(user);
                            if (!isMyProfile) {
                                loadFollowStatus();
                            }
                        }
                    } else {
                        if (isMyProfile) {
                            setDefaultProfile();
                        } else {
                            Toast.makeText(getContext(), "존재하지 않는 사용자입니다", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 프로필 로드 실패", e);
                    Toast.makeText(getContext(), "프로필을 불러오는 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    if (isMyProfile) {
                        setDefaultProfile();
                    }
                });
    }

    private void displayUserProfile(@NonNull UserDTO user) {
        String nickname = user.getNickname();
        tvName.setText(nickname != null && !nickname.isEmpty() ? nickname : "사용자");

        String comment = user.getComment();
        tvStatusMessage.setText(comment != null && !comment.isEmpty() ? comment : "상태메시지");

        String address = user.getAddress();
        tvLocation.setText(address != null && !address.isEmpty() ? address : "위치 정보 없음");

        String profileImageUrl = user.getProfileImageUrl();
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.ic_profile_default)
                    .error(R.drawable.ic_profile_default)
                    .into(imgProfile);
        } else {
            imgProfile.setImageResource(R.drawable.ic_profile_default);
        }

        originalName = tvName.getText().toString();
        originalStatus = tvStatusMessage.getText().toString();
        originalLocation = tvLocation.getText().toString();

        tvPostsCount.setText("0");
    }

    private void setDefaultProfile() {
        tvName.setText("사용자");
        tvStatusMessage.setText("상태메시지");
        tvLocation.setText("위치 정보 없음");
        imgProfile.setImageResource(R.drawable.ic_profile_default);

        originalName = tvName.getText().toString();
        originalStatus = tvStatusMessage.getText().toString();
        originalLocation = tvLocation.getText().toString();

        updateFollowCounts();
        tvPostsCount.setText("0");
    }

    private void loadFollowStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || targetUserId == null) {
            return;
        }

        String myUid = currentUser.getUid();

        db.collection("user")
                .document(myUid)
                .collection("following")
                .document(targetUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    isFollowing = doc.exists();
                    updateFollowUI();
                });

        db.collection("user")
                .document(targetUserId)
                .collection("following")
                .document(myUid)
                .get()
                .addOnSuccessListener(doc -> {
                    isFollower = doc.exists();
                    updateFollowUI();
                });
    }

    private void updateFollowUI() {
        if (!isMyProfile) {
            btnFollowButton.setVisibility(View.VISIBLE);
            if (isFollowing && isFollower) {
                btnFollowButton.setText("맞팔로우");
                btnFollowButton.setBackgroundResource(R.drawable.button_style2);
                btnFollowButton.setTextColor(0xFF6366F1);
            } else if (isFollowing) {
                btnFollowButton.setText("팔로우 중");
                btnFollowButton.setBackgroundResource(R.drawable.button_style2);
                btnFollowButton.setTextColor(0xFF6366F1);
            } else if (isFollower) {
                btnFollowButton.setText("맞 팔로우");
                btnFollowButton.setBackgroundResource(R.drawable.button_style1);
                btnFollowButton.setTextColor(0xFFFFFFFF);
            } else {
                btnFollowButton.setText("팔로우");
                btnFollowButton.setBackgroundResource(R.drawable.button_style1);
                btnFollowButton.setTextColor(0xFFFFFFFF);
            }
        }
    }

    /**
     * ✅ 팔로워/팔로잉 수 UI 업데이트 (Firestore 값 사용)
     */
    private void updateFollowCounts() {
        tvFollowersCount.setText(String.valueOf(followerCount));
        tvFollowingCount.setText(String.valueOf(followingCount));
    }

    private void loadTravelHistory() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 3);
        rvMyTravels.setLayoutManager(gridLayoutManager);

        scheduleList = new ArrayList<>();

        scheduleAdapter = new MyPageScheduleAdapter(scheduleList, (schedule, position) -> {
            Toast.makeText(getContext(), "여행 상세보기", Toast.LENGTH_SHORT).show();
        });

        rvMyTravels.setAdapter(scheduleAdapter);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (scheduleList.isEmpty()) {
            rvMyTravels.setVisibility(View.GONE);
            layoutNoTravel.setVisibility(View.VISIBLE);
        } else {
            rvMyTravels.setVisibility(View.VISIBLE);
            layoutNoTravel.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        btnLogout.setOnClickListener(v -> showLogoutDialog());

        btnEdit.setOnClickListener(v -> {
            if (isEditMode) {
                saveProfileChanges();
            } else {
                enterEditMode();
            }
        });

        btnFollowButton.setOnClickListener(v -> toggleFollow());

        imgProfile.setOnClickListener(v -> {
            if (isMyProfile) {
                Toast.makeText(getContext(), "프로필 사진 변경", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "프로필 사진 보기", Toast.LENGTH_SHORT).show();
            }
        });

        imgBackground.setOnClickListener(v -> {
            if (isMyProfile) {
                Toast.makeText(getContext(), "배경 사진 변경", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "배경 사진 보기", Toast.LENGTH_SHORT).show();
            }
        });

        if (isMyProfile) {
            followerSection.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), FollowActivity.class);
                intent.putExtra("userId", targetUserId);
                intent.putExtra("userName", tvName.getText().toString());
                intent.putExtra("isFollowerMode", true);
                intent.putExtra("isMyProfile", true);
                followActivityLauncher.launch(intent);
            });

            followingSection.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), FollowActivity.class);
                intent.putExtra("userId", targetUserId);
                intent.putExtra("userName", tvName.getText().toString());
                intent.putExtra("isFollowerMode", false);
                intent.putExtra("isMyProfile", true);
                followActivityLauncher.launch(intent);
            });
        }
    }

    private void toggleFollow() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isFollowing) {
            performUnfollow();
        } else {
            performFollow();
        }
    }

    /**
     * ✅ 팔로우 실행 (양방향 처리)
     */
    private void performFollow() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || targetUserId == null) return;

        String myUid = currentUser.getUid();

        Map<String, Object> followData = new HashMap<>();
        followData.put("followedAt", System.currentTimeMillis());

        // ✅ 1. 내 following에 추가
        db.collection("user")
                .document(myUid)
                .collection("following")
                .document(targetUserId)
                .set(followData)
                .addOnSuccessListener(aVoid -> {
                    // ✅ 2. 상대방 follower에 추가
                    db.collection("user")
                            .document(targetUserId)
                            .collection("follower")
                            .document(myUid)
                            .set(followData)
                            .addOnSuccessListener(aVoid2 -> {
                                isFollowing = true;
                                updateFollowUI();

                                // 팔로워 수 즉시 증가
                                followerCount++;
                                updateFollowCounts();

                                String message = isFollower ? "맞팔로우했습니다" : "팔로우했습니다";
                                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                FollowActionHelper.sendFollowNotification(targetUserId, myUid);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ 상대방 follower 추가 실패", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 팔로우 실패", e);
                    Toast.makeText(getContext(), "팔로우 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * ✅ 언팔로우 실행 (양방향 처리)
     */
    private void performUnfollow() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || targetUserId == null) return;

        String myUid = currentUser.getUid();

        new AlertDialog.Builder(requireContext())
                .setTitle("언팔로우")
                .setMessage("정말 언팔로우하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> {
                    // ✅ 1. 내 following에서 삭제
                    db.collection("user")
                            .document(myUid)
                            .collection("following")
                            .document(targetUserId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // ✅ 2. 상대방 follower에서 삭제
                                db.collection("user")
                                        .document(targetUserId)
                                        .collection("follower")
                                        .document(myUid)
                                        .delete()
                                        .addOnSuccessListener(aVoid2 -> {
                                            isFollowing = false;
                                            updateFollowUI();

                                            // 팔로워 수 즉시 감소
                                            if (followerCount > 0) followerCount--;
                                            updateFollowCounts();

                                            Toast.makeText(getContext(), "언팔로우했습니다", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "❌ 상대방 follower 삭제 실패", e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ 언팔로우 실패", e);
                                Toast.makeText(getContext(), "언팔로우 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("아니오", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃 하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> performLogout())
                .setNegativeButton("아니오", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void performLogout() {
        mAuth.signOut();
        Toast.makeText(getContext(), "로그아웃되었습니다", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void enterEditMode() {
        if (!isMyProfile) return;

        isEditMode = true;
        btnEdit.setImageResource(R.drawable.ic_check);

        LinearLayout nameLayout = (LinearLayout) tvName.getParent();
        int nameIndex = nameLayout.indexOfChild(tvName);
        tvName.setVisibility(View.GONE);

        etNameEdit = new EditText(getContext());
        etNameEdit.setText(tvName.getText());
        etNameEdit.setTextSize(22);
        etNameEdit.setTextColor(0xFF000000);
        etNameEdit.setTypeface(tvName.getTypeface());
        etNameEdit.setBackground(null);
        etNameEdit.setPadding(0, 0, 0, 0);
        etNameEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        etNameEdit.setLayoutParams(nameParams);
        nameLayout.addView(etNameEdit, nameIndex);

        LinearLayout statusParent = (LinearLayout) tvStatusMessage.getParent();
        int statusIndex = statusParent.indexOfChild(tvStatusMessage);
        tvStatusMessage.setVisibility(View.GONE);

        etStatusEdit = new EditText(getContext());
        etStatusEdit.setText(tvStatusMessage.getText());
        etStatusEdit.setTextSize(14);
        etStatusEdit.setTextColor(0xFF6B7280);
        etStatusEdit.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        etStatusEdit.setBackground(null);
        etStatusEdit.setPadding(0, 0, 0, 0);
        etStatusEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        etStatusEdit.setHint("상태메시지를 입력하세요");
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        statusParams.topMargin = dpToPx(4);
        etStatusEdit.setLayoutParams(statusParams);
        statusParent.addView(etStatusEdit, statusIndex);

        LinearLayout locationLayout = (LinearLayout) tvLocation.getParent();
        int locationIndex = locationLayout.indexOfChild(tvLocation);
        tvLocation.setVisibility(View.GONE);

        etLocationEdit = new EditText(getContext());
        etLocationEdit.setText(tvLocation.getText());
        etLocationEdit.setTextSize(13);
        etLocationEdit.setTextColor(0xFF000000);
        etLocationEdit.setBackground(null);
        etLocationEdit.setPadding(0, 0, 0, 0);
        etLocationEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        etLocationEdit.setHint("위치를 입력하세요");
        LinearLayout.LayoutParams locationParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        locationParams.setMarginStart(dpToPx(4));
        etLocationEdit.setLayoutParams(locationParams);
        locationLayout.addView(etLocationEdit, locationIndex);

        Toast.makeText(getContext(), "프로필 편집 모드", Toast.LENGTH_SHORT).show();
    }

    private void saveProfileChanges() {
        if (etNameEdit == null || etStatusEdit == null || etLocationEdit == null) {
            return;
        }

        String newName = etNameEdit.getText().toString().trim();
        String newStatus = etStatusEdit.getText().toString().trim();
        String newLocation = etLocationEdit.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(getContext(), "이름을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        UserDTO updatedUser = new UserDTO();
        updatedUser.setNickname(newName);
        updatedUser.setComment(newStatus.isEmpty() ? "" : newStatus);
        updatedUser.setAddress(newLocation.isEmpty() ? "" : newLocation);

        db.collection("user")
                .document(uid)
                .update(
                        "nickname", updatedUser.getNickname(),
                        "comment", updatedUser.getComment(),
                        "address", updatedUser.getAddress()
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ 프로필 업데이트 성공");

                    tvName.setText(newName);
                    tvStatusMessage.setText(newStatus.isEmpty() ? "상태메시지" : newStatus);
                    tvLocation.setText(newLocation.isEmpty() ? "위치 정보 없음" : newLocation);

                    exitEditMode();

                    originalName = newName;
                    originalStatus = newStatus;
                    originalLocation = newLocation;

                    Toast.makeText(getContext(), "프로필이 저장되었습니다", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 프로필 업데이트 실패", e);
                    Toast.makeText(getContext(), "저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                });
    }

    private void exitEditMode() {
        LinearLayout nameLayout = (LinearLayout) tvName.getParent();
        nameLayout.removeView(etNameEdit);
        tvName.setVisibility(View.VISIBLE);

        LinearLayout statusParent = (LinearLayout) tvStatusMessage.getParent();
        statusParent.removeView(etStatusEdit);
        tvStatusMessage.setVisibility(View.VISIBLE);

        LinearLayout locationLayout = (LinearLayout) tvLocation.getParent();
        locationLayout.removeView(etLocationEdit);
        tvLocation.setVisibility(View.VISIBLE);

        btnEdit.setImageResource(R.drawable.ic_edit);
        isEditMode = false;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}