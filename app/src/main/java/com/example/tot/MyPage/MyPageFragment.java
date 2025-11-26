package com.example.tot.MyPage;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.Authentication.LoginActivity;
import com.example.tot.Authentication.UserDTO;
import com.example.tot.Community.CommunityPostDTO;
import com.example.tot.Community.PostDetailActivity;
import com.example.tot.Follow.FollowActivity;
import com.example.tot.Follow.FollowButtonHelper;
import com.example.tot.R;
import com.example.tot.Schedule.ScheduleDTO;
import com.example.tot.Schedule.ScheduleSetting.ScheduleSettingActivity;
import com.example.tot.User.ProfileImageHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

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
    private LinearLayout followerSection;
    private LinearLayout followingSection;
    private RecyclerView rvMyPosts;
    private RecyclerView rvMySchedule;
    private LinearLayout layoutNoPosts;
    private LinearLayout layoutNoSchedule;
    private LinearLayout layoutPosts;
    private LinearLayout layoutSchedule;
    private TextView btnPosts;
    private TextView btnSchedule;
    private View indicatorPosts;
    private View indicatorSchedule;

    private MyPageScheduleAdapter scheduleAdapter;
    private List<ScheduleDTO> scheduleList;
    private MyPagePostsAdapter postsAdapter;
    private List<CommunityPostDTO> postList;


    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private MyPageProfileManager profileManager;
    private ListenerRegistration postsCountListener;
    private ListenerRegistration travelHistoryListener;
    private ListenerRegistration postsListener;

    private boolean isEditMode = false;
    private EditText etNameEdit;
    private EditText etStatusEdit;
    private EditText etLocationEdit;

    private String originalName;
    private String originalStatus;
    private String originalLocation;
    private String originalProfileImageUrl;
    private String originalBackgroundImageUrl;

    private int followerCount = 0;
    private int followingCount = 0;
    private boolean isCountsLoaded = false;

    private ActivityResultLauncher<Intent> followActivityLauncher;
    private ActivityResultLauncher<String> profileImageLauncher;
    private ActivityResultLauncher<String> backgroundImageLauncher;

    private String targetUserId;
    private boolean isMyProfile = true;
    private boolean isFollowing = false;
    private boolean isFollower = false;

    private Uri tempProfileImageUri = null;
    private Uri tempBackgroundImageUri = null;

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

        setupActivityResultLaunchers();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        profileManager = new MyPageProfileManager();

        initViews(view);
        determineProfileMode();
        setupTabListeners();
        setupRecyclerViews();
        updateTabSelection(true);

        loadFollowCounts(() -> loadProfileData());
        setupClickListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        loadPostsCount();
        loadMyPosts();
        loadTravelHistory();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (postsCountListener != null) {
            postsCountListener.remove();
        }
        if (travelHistoryListener != null) {
            travelHistoryListener.remove();
        }
        if (postsListener != null) {
            postsListener.remove();
        }
    }

    private void setupActivityResultLaunchers() {
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

        profileImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && isEditMode) {
                        tempProfileImageUri = uri;
                        ProfileImageHelper.loadProfileImageFromUri(imgProfile, uri);
                        Log.d(TAG, "✅ 프로필 이미지 선택됨: " + uri.toString());
                        Toast.makeText(getContext(), "프로필 사진이 선택되었습니다", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        backgroundImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && isEditMode) {
                        tempBackgroundImageUri = uri;
                        ProfileImageHelper.loadBackgroundImageFromUri(imgBackground, uri, R.drawable.sample3);
                        Log.d(TAG, "✅ 배경 이미지 선택됨: " + uri.toString());
                        Toast.makeText(getContext(), "배경 사진이 선택되었습니다", Toast.LENGTH_SHORT).show();
                    }
                }
        );
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
        rvMyPosts = view.findViewById(R.id.rv_my_posts);
        rvMySchedule = view.findViewById(R.id.rv_my_schedule);
        layoutNoPosts = view.findViewById(R.id.layout_no_posts);
        layoutNoSchedule = view.findViewById(R.id.layout_no_schedule);
        layoutPosts = view.findViewById(R.id.layout_posts);
        layoutSchedule = view.findViewById(R.id.layout_schedule);
        btnPosts = view.findViewById(R.id.btn_posts);
        btnSchedule = view.findViewById(R.id.btn_schedule);
        indicatorPosts = view.findViewById(R.id.indicator_posts);
        indicatorSchedule = view.findViewById(R.id.indicator_schedule);

        followerSection = (LinearLayout) tvFollowersCount.getParent();
        followingSection = (LinearLayout) tvFollowingCount.getParent();
    }

    private void setupRecyclerViews() {
        rvMyPosts.setLayoutManager(new GridLayoutManager(getContext(), 3));
        postList = new ArrayList<>();
        postsAdapter = new MyPagePostsAdapter(postList, (post, position) -> {
            Intent intent = new Intent(getContext(), PostDetailActivity.class);
            intent.putExtra("postId", post.getPostId());
            intent.putExtra("authorUid", post.getAuthorUid());
            intent.putExtra("scheduleId", post.getScheduleId());
            startActivity(intent);
        });
        rvMyPosts.setAdapter(postsAdapter);

        rvMySchedule.setLayoutManager(new GridLayoutManager(getContext(), 3));
        scheduleList = new ArrayList<>();
        scheduleAdapter = new MyPageScheduleAdapter(scheduleList, (schedule, position) -> {
            if (schedule.getStartDate() != null && schedule.getEndDate() != null) {
                Intent intent = new Intent(getContext(), ScheduleSettingActivity.class);
                intent.putExtra("scheduleId", schedule.getScheduleId());
                intent.putExtra("startDate", schedule.getStartDate().toDate().getTime());
                intent.putExtra("endDate", schedule.getEndDate().toDate().getTime());
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "스케줄 날짜 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
        rvMySchedule.setAdapter(scheduleAdapter);
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
            followerSection.setEnabled(true);
            followingSection.setEnabled(true);
        } else {
            btnLogout.setVisibility(View.GONE);
            btnBack.setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.GONE);
            btnFollowButton.setVisibility(View.VISIBLE);
            followerSection.setEnabled(false);
            followingSection.setEnabled(false);
        }
    }

    private void setupTabListeners() {
        btnPosts.setOnClickListener(v -> updateTabSelection(true));
        btnSchedule.setOnClickListener(v -> updateTabSelection(false));
    }

    private void updateTabSelection(boolean isPostsSelected) {
        if (isPostsSelected) {
            btnPosts.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            btnSchedule.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
            indicatorPosts.setVisibility(View.VISIBLE);
            indicatorSchedule.setVisibility(View.INVISIBLE);
            layoutPosts.setVisibility(View.VISIBLE);
            layoutSchedule.setVisibility(View.GONE);
        } else {
            btnPosts.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
            btnSchedule.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            indicatorPosts.setVisibility(View.INVISIBLE);
            indicatorSchedule.setVisibility(View.VISIBLE);
            layoutPosts.setVisibility(View.GONE);
            layoutSchedule.setVisibility(View.VISIBLE);
        }
    }

    private void loadFollowCounts(Runnable onComplete) {
        if (targetUserId == null || targetUserId.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        db.collection("user").document(targetUserId).collection("follower").get()
                .addOnSuccessListener(querySnapshot -> {
                    followerCount = querySnapshot.size();
                    checkCountsLoadedAndUpdate(onComplete);
                })
                .addOnFailureListener(e -> checkCountsLoadedAndUpdate(onComplete));

        db.collection("user").document(targetUserId).collection("following").get()
                .addOnSuccessListener(querySnapshot -> {
                    followingCount = querySnapshot.size();
                    checkCountsLoadedAndUpdate(onComplete);
                })
                .addOnFailureListener(e -> checkCountsLoadedAndUpdate(onComplete));
    }

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

        db.collection("user").document(targetUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserDTO user = documentSnapshot.toObject(UserDTO.class);
                        if (user != null) {
                            displayUserProfile(user);
                            if (!isMyProfile) loadFollowStatus();
                        }
                    } else {
                        if (isMyProfile) setDefaultProfile();
                        else Toast.makeText(getContext(), "존재하지 않는 사용자입니다", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isMyProfile) setDefaultProfile();
                });
    }

    private void displayUserProfile(@NonNull UserDTO user) {
        tvName.setText(user.getNickname() != null && !user.getNickname().isEmpty() ? user.getNickname() : "사용자");
        tvStatusMessage.setText(user.getComment() != null && !user.getComment().isEmpty() ? user.getComment() : "상태메시지");
        tvLocation.setText(user.getAddress() != null && !user.getAddress().isEmpty() ? user.getAddress() : "위치 정보 없음");

        originalProfileImageUrl = user.getProfileImageUrl();
        ProfileImageHelper.loadProfileImage(imgProfile, originalProfileImageUrl);
        Log.d(TAG, "✅ 프로필 이미지 로드: " + originalProfileImageUrl);

        originalBackgroundImageUrl = user.getBackgroundImageUrl();
        ProfileImageHelper.loadBackgroundImage(imgBackground, originalBackgroundImageUrl, R.drawable.sample3);
        Log.d(TAG, "✅ 배경 이미지 로드: " + originalBackgroundImageUrl);

        originalName = tvName.getText().toString();
        originalStatus = tvStatusMessage.getText().toString();
        originalLocation = tvLocation.getText().toString();
    }

    private void setDefaultProfile() {
        tvName.setText("사용자");
        tvStatusMessage.setText("상태메시지");
        tvLocation.setText("위치 정보 없음");
        imgProfile.setImageResource(R.drawable.ic_profile_default);
        imgBackground.setImageResource(R.drawable.sample3);

        originalName = tvName.getText().toString();
        originalStatus = tvStatusMessage.getText().toString();
        originalLocation = tvLocation.getText().toString();
        originalProfileImageUrl = null;
        originalBackgroundImageUrl = null;

        updateFollowCounts();
    }

    private void loadFollowStatus() {
        FollowButtonHelper.checkFollowStatus(targetUserId, (following, follower) -> {
            isFollowing = following;
            isFollower = follower;
            FollowButtonHelper.updateFollowButton(btnFollowButton, isFollowing, isFollower);
        });
    }

    private void updateFollowCounts() {
        tvFollowersCount.setText(String.valueOf(followerCount));
        tvFollowingCount.setText(String.valueOf(followingCount));
    }

    private void loadTravelHistory() {
        if (targetUserId == null || targetUserId.isEmpty()) {
            updateScheduleEmptyState();
            return;
        }

        if (travelHistoryListener != null) {
            travelHistoryListener.remove();
        }

        travelHistoryListener = db.collection("user").document(targetUserId).collection("schedule")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Error getting documents: ", e);
                        Toast.makeText(getContext(), "여행 기록을 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                        updateScheduleEmptyState();
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        scheduleList.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            ScheduleDTO schedule = document.toObject(ScheduleDTO.class);
                            scheduleList.add(schedule);
                        }
                        scheduleAdapter.notifyDataSetChanged();
                        updateScheduleEmptyState();
                    }
                });
    }

    private void loadMyPosts() {
        if (targetUserId == null || targetUserId.isEmpty()) {
            updatePostsEmptyState();
            return;
        }

        if (postsListener != null) {
            postsListener.remove();
        }

        postsListener = db.collection("public").document("community").collection("posts")
                .whereEqualTo("authorUid", targetUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Error getting documents: ", e);
                        Toast.makeText(getContext(), "게시글을 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                        updatePostsEmptyState();
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        postList.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            CommunityPostDTO post = document.toObject(CommunityPostDTO.class);
                            postList.add(post);
                        }
                        postsAdapter.notifyDataSetChanged();
                        updatePostsEmptyState();
                    }
                });
    }

    private void loadPostsCount() {
        if (targetUserId == null || targetUserId.isEmpty()) {
            tvPostsCount.setText("0");
            return;
        }

        if (postsCountListener != null) {
            postsCountListener.remove();
        }

        postsCountListener = db.collection("public").document("community").collection("posts")
                .whereEqualTo("authorUid", targetUserId)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        tvPostsCount.setText("0");
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        int postsCount = queryDocumentSnapshots.size();
                        tvPostsCount.setText(String.valueOf(postsCount));
                    }
                });
    }

    private void updateScheduleEmptyState() {
        if (scheduleList.isEmpty()) {
            rvMySchedule.setVisibility(View.GONE);
            layoutNoSchedule.setVisibility(View.VISIBLE);
        } else {
            rvMySchedule.setVisibility(View.VISIBLE);
            layoutNoSchedule.setVisibility(View.GONE);
        }
    }

    private void updatePostsEmptyState() {
        if (postList.isEmpty()) {
            rvMyPosts.setVisibility(View.GONE);
            layoutNoPosts.setVisibility(View.VISIBLE);
        } else {
            rvMyPosts.setVisibility(View.VISIBLE);
            layoutNoPosts.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        btnLogout.setOnClickListener(v -> showLogoutDialog());

        btnEdit.setOnClickListener(v -> {
            if (isEditMode) saveProfileChanges();
            else enterEditMode();
        });

        btnFollowButton.setOnClickListener(v -> {
            FollowButtonHelper.handleFollowButtonClick(requireContext(), targetUserId, isFollowing, isFollower,
                    new FollowButtonHelper.FollowActionCallback() {
                        @Override
                        public void onSuccess(boolean nowFollowing) {
                            isFollowing = nowFollowing;
                            FollowButtonHelper.updateFollowButton(btnFollowButton, isFollowing, isFollower);
                            if (nowFollowing) followerCount++;
                            else if (followerCount > 0) followerCount--;
                            updateFollowCounts();
                        }

                        @Override
                        public void onFailure(String message) {
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        imgProfile.setOnClickListener(v -> {
            if (isMyProfile) {
                if (isEditMode) {
                    Log.d(TAG, "🖼️ 프로필 이미지 선택 시작");
                    profileImageLauncher.launch("image/*");
                } else {
                    Toast.makeText(getContext(), "편집 모드에서 변경 가능합니다", Toast.LENGTH_SHORT).show();
                }
            }
        });

        imgBackground.setOnClickListener(v -> {
            if (isMyProfile) {
                if (isEditMode) {
                    Log.d(TAG, "🖼️ 배경 이미지 선택 시작");
                    backgroundImageLauncher.launch("image/*");
                } else {
                    Toast.makeText(getContext(), "편집 모드에서 변경 가능합니다", Toast.LENGTH_SHORT).show();
                }
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

    private void showLogoutDialog() {
        new android.app.AlertDialog.Builder(requireContext())
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
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        locationParams.setMarginStart(dpToPx(4));
        etLocationEdit.setLayoutParams(locationParams);
        locationLayout.addView(etLocationEdit, locationIndex);

        Toast.makeText(getContext(), "프로필 편집 모드 (사진 클릭 시 변경 가능)", Toast.LENGTH_SHORT).show();
    }

    private void saveProfileChanges() {
        if (etNameEdit == null || etStatusEdit == null || etLocationEdit == null) return;

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

        Toast.makeText(getContext(), "저장 중...", Toast.LENGTH_SHORT).show();
        btnEdit.setEnabled(false);

        Log.d(TAG, "💾 프로필 저장 시작");
        Log.d(TAG, "- 프로필 이미지 URI: " + (tempProfileImageUri != null ? tempProfileImageUri.toString() : "null"));
        Log.d(TAG, "- 배경 이미지 URI: " + (tempBackgroundImageUri != null ? tempBackgroundImageUri.toString() : "null"));

        profileManager.uploadAndSaveProfile(
                uid, newName, newStatus, newLocation,
                tempProfileImageUri, tempBackgroundImageUri,
                originalProfileImageUrl, originalBackgroundImageUrl,
                new MyPageProfileManager.SaveCallback() {
                    @Override
                    public void onSuccess() {
                        tvName.setText(newName);
                        tvStatusMessage.setText(newStatus.isEmpty() ? "상태메시지" : newStatus);
                        tvLocation.setText(newLocation.isEmpty() ? "위치 정보 없음" : newLocation);

                        if (tempProfileImageUri != null) {
                            Log.d(TAG, "✅ 프로필 이미지 업데이트 완료");
                            tempProfileImageUri = null;
                        }

                        if (tempBackgroundImageUri != null) {
                            Log.d(TAG, "✅ 배경 이미지 업데이트 완료");
                            tempBackgroundImageUri = null;
                        }

                        exitEditMode();

                        originalName = newName;
                        originalStatus = newStatus;
                        originalLocation = newLocation;

                        // 프로필 데이터 재로드
                        loadProfileData();

                        Toast.makeText(getContext(), "프로필이 저장되었습니다", Toast.LENGTH_SHORT).show();
                        btnEdit.setEnabled(true);
                    }

                    @Override
                    public void onFailure(String message) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        btnEdit.setEnabled(true);
                    }
                }
        );
    }

    private void exitEditMode() {
        if (tempProfileImageUri != null) {
            ProfileImageHelper.loadProfileImage(imgProfile, originalProfileImageUrl);
            tempProfileImageUri = null;
        }

        if (tempBackgroundImageUri != null) {
            ProfileImageHelper.loadBackgroundImage(imgBackground, originalBackgroundImageUrl, R.drawable.sample3);
            tempBackgroundImageUri = null;
        }

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