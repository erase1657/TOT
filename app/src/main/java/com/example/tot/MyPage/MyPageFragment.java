package com.example.tot.MyPage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
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

import com.example.tot.Authentication.LoginActivity;
import com.example.tot.Follow.FollowActivity;
import com.example.tot.R;
import com.example.tot.Schedule.ScheduleDTO;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MyPageFragment extends Fragment {

    private CircleImageView imgProfile;
    private ImageView imgBackground;
    private ImageView btnLogout;
    private ImageView btnEdit;
    private TextView tvName;
    private TextView tvStatusMessage;
    private TextView tvLocation;
    private TextView tvFollowersCount;
    private TextView tvFollowingCount;
    private TextView tvPostsCount;
    private LinearLayout followerSection;
    private LinearLayout followingSection;
    private RecyclerView rvMyTravels;
    private LinearLayout layoutNoTravel;

    private MyPageScheduleAdapter scheduleAdapter;
    private List<ScheduleDTO> scheduleList;

    // Firebase Auth
    private FirebaseAuth mAuth;

    // 수정 모드 관련
    private boolean isEditMode = false;
    private EditText etNameEdit;
    private EditText etStatusEdit;
    private EditText etLocationEdit;

    // 원본 값 저장
    private String originalName;
    private String originalStatus;
    private String originalLocation;

    // ✅ 팔로워/팔로잉 수 (더미 데이터)
    private int followerCount = 5;
    private int followingCount = 4;

    // ✅ ActivityResultLauncher (팔로우 화면에서 돌아올 때)
    private ActivityResultLauncher<Intent> followActivityLauncher;

    public MyPageFragment() {
        super(R.layout.fragment_mypage);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ ActivityResultLauncher 등록
        followActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        // FollowActivity에서 반환된 데이터 받기
                        Intent data = result.getData();
                        followerCount = data.getIntExtra("followerCount", followerCount);
                        followingCount = data.getIntExtra("followingCount", followingCount);

                        // UI 업데이트
                        updateFollowCounts();
                    }
                }
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        initViews(view);
        loadProfileData();
        loadTravelHistory();
        setupClickListeners();
    }

    private void initViews(View view) {
        imgProfile = view.findViewById(R.id.img_profile);
        imgBackground = view.findViewById(R.id.img_background);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnEdit = view.findViewById(R.id.btn_edit);
        tvName = view.findViewById(R.id.tv_name);
        tvStatusMessage = view.findViewById(R.id.tv_status_message);
        tvLocation = view.findViewById(R.id.tv_location);
        tvFollowersCount = view.findViewById(R.id.tv_followers_count);
        tvFollowingCount = view.findViewById(R.id.tv_following_count);
        tvPostsCount = view.findViewById(R.id.tv_posts_count);
        rvMyTravels = view.findViewById(R.id.rv_my_travels);
        layoutNoTravel = view.findViewById(R.id.layout_no_travel);

        followerSection = (LinearLayout) tvFollowersCount.getParent();
        followingSection = (LinearLayout) tvFollowingCount.getParent();
    }

    private void loadProfileData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                tvName.setText(displayName);
            } else {
                tvName.setText("위찬우");
            }
        } else {
            tvName.setText("위찬우");
        }

        tvStatusMessage.setText("여행을 사랑하는 개발자");
        tvLocation.setText("전라북도, 익산시");

        // ✅ 팔로워/팔로잉 수 표시
        updateFollowCounts();

        tvPostsCount.setText("68");

        originalName = tvName.getText().toString();
        originalStatus = tvStatusMessage.getText().toString();
        originalLocation = tvLocation.getText().toString();
    }

    /**
     * ✅ 팔로워/팔로잉 수 업데이트
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
            Toast.makeText(getContext(),
                    "여행 상세보기",
                    Toast.LENGTH_SHORT).show();
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
        btnLogout.setOnClickListener(v -> showLogoutDialog());

        btnEdit.setOnClickListener(v -> {
            if (isEditMode) {
                saveProfileChanges();
            } else {
                enterEditMode();
            }
        });

        imgProfile.setOnClickListener(v -> {
            Toast.makeText(getContext(), "프로필 사진 변경", Toast.LENGTH_SHORT).show();
        });

        imgBackground.setOnClickListener(v -> {
            Toast.makeText(getContext(), "배경 사진 변경", Toast.LENGTH_SHORT).show();
        });

        // ✅ 팔로워 클릭 (ActivityResultLauncher 사용)
        followerSection.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), FollowActivity.class);
            intent.putExtra("userId", "my_user_id");
            intent.putExtra("userName", tvName.getText().toString());
            intent.putExtra("isFollowerMode", true);
            intent.putExtra("isMyProfile", true);
            followActivityLauncher.launch(intent);
        });

        // ✅ 팔로잉 클릭 (ActivityResultLauncher 사용)
        followingSection.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), FollowActivity.class);
            intent.putExtra("userId", "my_user_id");
            intent.putExtra("userName", tvName.getText().toString());
            intent.putExtra("isFollowerMode", false);
            intent.putExtra("isMyProfile", true);
            followActivityLauncher.launch(intent);
        });
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
        isEditMode = true;
        btnEdit.setImageResource(R.drawable.ic_check);

        // 이름 수정
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

        // 상태메시지 수정
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

        // 위치 수정
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

        tvName.setText(newName);
        tvStatusMessage.setText(newStatus.isEmpty() ? "상태메시지" : newStatus);
        tvLocation.setText(newLocation.isEmpty() ? "위치 정보 없음" : newLocation);

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

        originalName = newName;
        originalStatus = newStatus;
        originalLocation = newLocation;

        Toast.makeText(getContext(), "프로필이 저장되었습니다", Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}