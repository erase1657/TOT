package com.example.tot.MyPage;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.Authentication.LoginActivity;
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

    // 원본 값 저장 (취소 시 복원용)
    private String originalName;
    private String originalStatus;
    private String originalLocation;

    public MyPageFragment() {
        super(R.layout.fragment_mypage);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Firebase 초기화
        mAuth = FirebaseAuth.getInstance();

        // View 초기화
        initViews(view);

        // 프로필 데이터 로드
        loadProfileData();

        // 여행 기록 로드
        loadTravelHistory();

        // 클릭 이벤트 설정
        setupClickListeners();
    }

    /**
     * View 초기화
     */
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
    }

    /**
     * 프로필 데이터 로드
     */
    private void loadProfileData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // TODO: 서버에서 사용자 정보 가져오기
            // 현재는 더미 데이터 사용
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                tvName.setText(displayName);
            } else {
                tvName.setText("위찬우"); // 기본값
            }
        } else {
            tvName.setText("위찬우"); // 기본값
        }

        // 더미 데이터
        tvStatusMessage.setText("여행을 사랑하는 개발자");
        tvLocation.setText("전라북도, 익산시");
        tvFollowersCount.setText("205");
        tvFollowingCount.setText("178");
        tvPostsCount.setText("68");

        // 원본 값 저장
        originalName = tvName.getText().toString();
        originalStatus = tvStatusMessage.getText().toString();
        originalLocation = tvLocation.getText().toString();
    }

    /**
     * 여행 기록 로드
     */
    private void loadTravelHistory() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 3);
        rvMyTravels.setLayoutManager(gridLayoutManager);

        scheduleList = new ArrayList<>();

       /* scheduleList.add(new ScheduleDTO(
                "schedule_001", "익산", "23-27", "2020년 8월",
                R.drawable.sample3, "2020-08-23", "2020-08-27"
        ));

        scheduleList.add(new ScheduleDTO(
                "schedule_002", "광주", "14-25", "2022년 9월",
                R.drawable.sample4, "2022-09-14", "2022-09-25"
        ));

        scheduleList.add(new ScheduleDTO(
                "schedule_003", "춘천", "14-23", "2023년 3월",
                R.drawable.sample2, "2023-03-14", "2023-03-23"
        ));

        scheduleList.add(new ScheduleDTO(
                "schedule_004", "부산", "1-5", "2023년 7월",
                R.drawable.sample3, "2023-07-01", "2023-07-05"
        ));

        scheduleList.add(new ScheduleDTO(
                "schedule_005", "제주", "10-15", "2024년 4월",
                R.drawable.sample4, "2024-04-10", "2024-04-15"
        ));

        scheduleList.add(new ScheduleDTO(
                "schedule_006", "서울, 전주 외 2곳", "20-25", "2024년 12월",
                R.drawable.sample2, "2024-12-20", "2024-12-25"
        ));

        scheduleAdapter = new MyPageScheduleAdapter(scheduleList, (schedule, position) -> {
            Toast.makeText(getContext(),
                    schedule.getLocation() + " 여행 상세보기",
                    Toast.LENGTH_SHORT).show();
        });
*/
        rvMyTravels.setAdapter(scheduleAdapter);
        updateEmptyState();
    }

    /**
     * 빈 상태 UI 업데이트
     */
    private void updateEmptyState() {
        if (scheduleList.isEmpty()) {
            rvMyTravels.setVisibility(View.GONE);
            layoutNoTravel.setVisibility(View.VISIBLE);
        } else {
            rvMyTravels.setVisibility(View.VISIBLE);
            layoutNoTravel.setVisibility(View.GONE);
        }
    }

    /**
     * 클릭 이벤트 설정
     */
    private void setupClickListeners() {
        // 로그아웃 버튼
        btnLogout.setOnClickListener(v -> showLogoutDialog());

        // 수정 버튼 (수정/저장 토글)
        btnEdit.setOnClickListener(v -> {
            if (isEditMode) {
                saveProfileChanges();
            } else {
                enterEditMode();
            }
        });

        // 프로필 이미지 클릭
        imgProfile.setOnClickListener(v -> {
            Toast.makeText(getContext(), "프로필 사진 변경", Toast.LENGTH_SHORT).show();
            // TODO: 이미지 선택 기능
        });

        // 배경 이미지 클릭
        imgBackground.setOnClickListener(v -> {
            Toast.makeText(getContext(), "배경 사진 변경", Toast.LENGTH_SHORT).show();
            // TODO: 이미지 선택 기능
        });
    }

    /**
     * 로그아웃 확인 다이얼로그
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃 하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> performLogout())
                .setNegativeButton("아니오", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * 로그아웃 실행
     */
    private void performLogout() {
        // Firebase 로그아웃
        mAuth.signOut();
        // ✅ Google Credential 로그아웃 (One Tap / CredentialManager 세션 초기화)
        String serverClientId = getString(R.string.default_web_client_id);
        Toast.makeText(getContext(), "로그아웃되었습니다", Toast.LENGTH_SHORT).show();

        // 로그인 화면으로 이동
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    /**
     * 수정 모드 진입
     */
    private void enterEditMode() {
        isEditMode = true;

        // 버튼 아이콘을 체크 표시로 변경
        btnEdit.setImageResource(R.drawable.ic_check);

        // === 1. 이름 TextView → EditText ===
        LinearLayout nameLayout = (LinearLayout) tvName.getParent();
        int nameIndex = nameLayout.indexOfChild(tvName);
        tvName.setVisibility(View.GONE);

        etNameEdit = new EditText(getContext());
        etNameEdit.setText(tvName.getText());
        etNameEdit.setTextSize(22);
        etNameEdit.setTextColor(0xFF000000);
        etNameEdit.setTypeface(tvName.getTypeface()); // 볼드체 유지
        etNameEdit.setBackground(null); // 배경 제거 (밑줄 없음)
        etNameEdit.setPadding(0, 0, 0, 0); // 패딩 제거
        etNameEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        etNameEdit.setLayoutParams(nameParams);
        nameLayout.addView(etNameEdit, nameIndex);

        // === 2. 상태메시지 TextView → EditText ===
        LinearLayout statusParent = (LinearLayout) tvStatusMessage.getParent();
        int statusIndex = statusParent.indexOfChild(tvStatusMessage);
        tvStatusMessage.setVisibility(View.GONE);

        etStatusEdit = new EditText(getContext());
        etStatusEdit.setText(tvStatusMessage.getText());
        etStatusEdit.setTextSize(14);
        etStatusEdit.setTextColor(0xFF6B7280);
        etStatusEdit.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        etStatusEdit.setBackground(null); // 배경 제거 (밑줄 없음)
        etStatusEdit.setPadding(0, 0, 0, 0); // 패딩 제거
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

        // === 3. 위치 TextView → EditText ===
        LinearLayout locationLayout = (LinearLayout) tvLocation.getParent();
        int locationIndex = locationLayout.indexOfChild(tvLocation);
        tvLocation.setVisibility(View.GONE);

        etLocationEdit = new EditText(getContext());
        etLocationEdit.setText(tvLocation.getText());
        etLocationEdit.setTextSize(13);
        etLocationEdit.setTextColor(0xFF000000);
        etLocationEdit.setBackground(null); // 배경 제거 (밑줄 없음)
        etLocationEdit.setPadding(0, 0, 0, 0); // 패딩 제거
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

    /**
     * 변경사항 저장
     */
    private void saveProfileChanges() {
        if (etNameEdit == null || etStatusEdit == null || etLocationEdit == null) {
            return;
        }

        // 입력값 가져오기
        String newName = etNameEdit.getText().toString().trim();
        String newStatus = etStatusEdit.getText().toString().trim();
        String newLocation = etLocationEdit.getText().toString().trim();

        // 유효성 검사
        if (newName.isEmpty()) {
            Toast.makeText(getContext(), "이름을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // EditText → TextView로 복원
        tvName.setText(newName);
        tvStatusMessage.setText(newStatus.isEmpty() ? "상태메시지" : newStatus);
        tvLocation.setText(newLocation.isEmpty() ? "위치 정보 없음" : newLocation);

        // EditText 제거
        LinearLayout nameLayout = (LinearLayout) tvName.getParent();
        nameLayout.removeView(etNameEdit);
        tvName.setVisibility(View.VISIBLE);

        LinearLayout statusParent = (LinearLayout) tvStatusMessage.getParent();
        statusParent.removeView(etStatusEdit);
        tvStatusMessage.setVisibility(View.VISIBLE);

        LinearLayout locationLayout = (LinearLayout) tvLocation.getParent();
        locationLayout.removeView(etLocationEdit);
        tvLocation.setVisibility(View.VISIBLE);

        // 버튼 아이콘 원래대로
        btnEdit.setImageResource(R.drawable.ic_edit);

        // 수정 모드 해제
        isEditMode = false;

        // 원본 값 업데이트
        originalName = newName;
        originalStatus = newStatus;
        originalLocation = newLocation;

        Toast.makeText(getContext(), "프로필이 저장되었습니다", Toast.LENGTH_SHORT).show();

        // TODO: 서버에 변경사항 전송
        // updateProfileToServer(newName, newStatus, newLocation);
    }

    /**
     * DP를 PX로 변환
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * 서버에 프로필 업데이트 (예시)
     */
    private void updateProfileToServer(String name, String status, String location) {
        // TODO: Retrofit으로 서버 API 호출
        // ApiService.updateProfile(name, status, location)
        //     .enqueue(new Callback<Response>() {
        //         @Override
        //         public void onResponse(Call<Response> call, Response<Response> response) {
        //             if (response.isSuccessful()) {
        //                 Toast.makeText(getContext(), "프로필 업데이트 성공", Toast.LENGTH_SHORT).show();
        //             }
        //         }
        //
        //         @Override
        //         public void onFailure(Call<Response> call, Throwable t) {
        //             Toast.makeText(getContext(), "프로필 업데이트 실패", Toast.LENGTH_SHORT).show();
        //         }
        //     });
    }
}