package com.example.tot.ViewPager;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;
import com.example.tot.ScheduleRecyclerView.ScheduleData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private List<ScheduleData> scheduleList;

    public MyPageFragment() {
        super(R.layout.fragment_mypage);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // View 초기화
        initViews(view);

        // 프로필 데이터 로드 (더미 데이터)
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
     * 프로필 데이터 로드 (더미 데이터)
     * TODO: 서버에서 사용자 정보 가져오기
     */
    private void loadProfileData() {
        // 더미 데이터 설정
        tvName.setText("위찬우");
        tvStatusMessage.setText("여행을 사랑하는 개발자");
        tvLocation.setText("전라북도, 익산시");
        tvFollowersCount.setText("205");
        tvFollowingCount.setText("178");
        tvPostsCount.setText("68");

        // 프로필 이미지는 기본값 사용 (실제로는 서버에서 이미지 로드)
        // Glide.with(this).load(profileImageUrl).into(imgProfile);
    }

    /**
     * 여행 기록 로드
     */
    private void loadTravelHistory() {
        // RecyclerView 설정 (3열 그리드)
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 3);
        rvMyTravels.setLayoutManager(gridLayoutManager);

        // 더미 스케줄 데이터 생성
        scheduleList = new ArrayList<>();

        scheduleList.add(new ScheduleData(
                "schedule_001",
                "익산",
                "23-27",
                "2020년 8월",
                R.drawable.sample3,
                "2020-08-23",
                "2020-08-27"
        ));

        scheduleList.add(new ScheduleData(
                "schedule_002",
                "광주",
                "14-25",
                "2022년 9월",
                R.drawable.sample4,
                "2022-09-14",
                "2022-09-25"
        ));

        scheduleList.add(new ScheduleData(
                "schedule_003",
                "춘천",
                "14-23",
                "2023년 3월",
                R.drawable.sample2,
                "2023-03-14",
                "2023-03-23"
        ));

        scheduleList.add(new ScheduleData(
                "schedule_004",
                "부산",
                "1-5",
                "2023년 7월",
                R.drawable.sample3,
                "2023-07-01",
                "2023-07-05"
        ));

        scheduleList.add(new ScheduleData(
                "schedule_005",
                "제주",
                "10-15",
                "2024년 4월",
                R.drawable.sample4,
                "2024-04-10",
                "2024-04-15"
        ));

        scheduleList.add(new ScheduleData(
                "schedule_006",
                "서울, 전주 외 2곳",
                "20-25",
                "2024년 12월",
                R.drawable.sample2,
                "2024-12-20",
                "2024-12-25"
        ));

        // 어댑터 설정
        scheduleAdapter = new MyPageScheduleAdapter(scheduleList, (schedule, position) -> {
            // 스케줄 클릭 시 동작
            Toast.makeText(getContext(),
                    schedule.getLocation() + " 여행 상세보기",
                    Toast.LENGTH_SHORT).show();

            // TODO: 스케줄 상세 화면으로 이동
        });

        rvMyTravels.setAdapter(scheduleAdapter);

        // 빈 상태 업데이트
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
        btnLogout.setOnClickListener(v -> {
            Toast.makeText(getContext(), "로그아웃", Toast.LENGTH_SHORT).show();
            // TODO: 로그아웃 처리
        });

        // 수정 버튼
        btnEdit.setOnClickListener(v -> {
            Toast.makeText(getContext(), "프로필 수정", Toast.LENGTH_SHORT).show();
            // TODO: 프로필 수정 화면으로 이동
        });

        // 프로필 이미지 클릭 (사진 변경)
        imgProfile.setOnClickListener(v -> {
            Toast.makeText(getContext(), "프로필 사진 변경", Toast.LENGTH_SHORT).show();
            // TODO: 이미지 선택 다이얼로그 표시
        });

        // 배경 이미지 클릭 (사진 변경)
        imgBackground.setOnClickListener(v -> {
            Toast.makeText(getContext(), "배경 사진 변경", Toast.LENGTH_SHORT).show();
            // TODO: 이미지 선택 다이얼로그 표시
        });
    }

    /**
     * 서버에서 프로필 정보 가져오기 (예시)
     */
    private void fetchProfileFromServer() {
        // TODO: Retrofit 등으로 서버 API 호출
        // ApiService.getProfile()
        //     .enqueue(new Callback<UserProfile>() {
        //         @Override
        //         public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
        //             if (response.isSuccessful() && response.body() != null) {
        //                 updateProfileUI(response.body());
        //             }
        //         }
        //
        //         @Override
        //         public void onFailure(Call<UserProfile> call, Throwable t) {
        //             Log.e("MyPageFragment", "API 호출 실패", t);
        //         }
        //     });
    }

    /**
     * 서버에서 여행 기록 가져오기 (예시)
     */
    private void fetchTravelHistoryFromServer() {
        // TODO: Retrofit 등으로 서버 API 호출
        // ApiService.getMySchedules()
        //     .enqueue(new Callback<List<ScheduleData>>() {
        //         @Override
        //         public void onResponse(Call<List<ScheduleData>> call, Response<List<ScheduleData>> response) {
        //             if (response.isSuccessful() && response.body() != null) {
        //                 scheduleAdapter.updateData(response.body());
        //                 updateEmptyState();
        //             }
        //         }
        //
        //         @Override
        //         public void onFailure(Call<List<ScheduleData>> call, Throwable t) {
        //             Log.e("MyPageFragment", "API 호출 실패", t);
        //         }
        //     });
    }
}