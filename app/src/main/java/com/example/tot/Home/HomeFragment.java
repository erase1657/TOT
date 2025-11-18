package com.example.tot.Home;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.Community.CommunityAdapter;
import com.example.tot.Community.CommunityPostDTO;
import com.example.tot.Community.CommunityViewModel;
import com.example.tot.Notification.NotificationActivity;
import com.example.tot.Notification.NotificationDTO;
import com.example.tot.Notification.NotificationManager;
import com.example.tot.R;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeFragment extends Fragment {

    // 지역 코드
    private String selectedProvinceCode = "ALL";
    private String selectedCityCode = "";

    // UI
    private LinearLayout provinceButtonContainer;
    private LinearLayout cityButtonContainer;
    private HorizontalScrollView cityScrollView;
    private Button currentSelectedProvinceButton;
    private Button currentSelectedCityButton;
    private CircleImageView profileImage;
    private FrameLayout inboxContainer;
    private TextView inboxBadge;

    // 데이터
    private CommunityAdapter communityAdapter;
    private CommunityViewModel viewModel;

    // 알림 관리자
    private NotificationManager notificationManager;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initViewModel();
        setupNotificationManager();
        setupProvinceButtons();
        setupMemoryRecyclerView(view.findViewById(R.id.re_memory));
        setupCommunityStyleRecyclerView(view.findViewById(R.id.re_album));
        setupProfileAndInbox();

        viewModel.loadDummyData();
        filterAlbums();
        loadDummyNotifications();
    }

    private void initViews(View view) {
        provinceButtonContainer = view.findViewById(R.id.provinceButtonContainer);
        cityButtonContainer = view.findViewById(R.id.cityButtonContainer);
        cityScrollView = view.findViewById(R.id.cityScrollView);
        profileImage = view.findViewById(R.id.profileImage);
        inboxContainer = view.findViewById(R.id.inbox_container);
        inboxBadge = view.findViewById(R.id.inbox_badge);
    }

    private void initViewModel() {
        viewModel = new CommunityViewModel(new CommunityViewModel.DataCallback() {
            @Override
            public void onDataChanged(List<CommunityPostDTO> posts) {
                if (communityAdapter != null) {
                    // ✅ 홈화면에서는 사용자 검색 결과 없이 게시글만 표시
                    communityAdapter.updateDataWithUsers(posts, new ArrayList<>(), false, false);
                }
            }

            @Override
            public void onDataAdded(List<CommunityPostDTO> posts) {
                if (communityAdapter != null) {
                    communityAdapter.addData(posts);
                }
            }
        });
        // ✅ 홈화면에서는 인기순 필터 적용
        viewModel.setFilter(CommunityViewModel.FilterMode.POPULAR);
    }

    private void setupNotificationManager() {
        notificationManager = NotificationManager.getInstance();
        notificationManager.addListener(this::updateInboxBadge);
    }

    /** ✅ 프로필 및 수신함 (애니메이션 포함) */
    private void setupProfileAndInbox() {
        profileImage.setOnClickListener(v -> {
            androidx.viewpager2.widget.ViewPager2 viewPager =
                    requireActivity().findViewById(R.id.viewpager);
            if (viewPager != null) viewPager.setCurrentItem(3);
        });

        inboxContainer.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), NotificationActivity.class);
            startActivity(intent);
        });

        // 뱃지 배경 설정
        GradientDrawable badgeBackground = new GradientDrawable();
        badgeBackground.setShape(GradientDrawable.RECTANGLE);
        badgeBackground.setColor(Color.parseColor("#FF4444"));
        badgeBackground.setCornerRadius(dpToPx(8));
        inboxBadge.setBackground(badgeBackground);
        inboxBadge.setClipToOutline(true);

        // 초기 뱃지 업데이트
        updateInboxBadge(notificationManager.getUnreadCount());
    }

    private void updateInboxBadge(int unreadCount) {
        if (inboxBadge == null) return;

        if (unreadCount > 0) {
            inboxBadge.setVisibility(View.VISIBLE);
            inboxBadge.setText(unreadCount > 10 ? "10+" : String.valueOf(unreadCount));

            // 애니메이션은 처음 표시될 때만
            if (inboxBadge.getTag() == null) {
                Animation shakeAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.shake);
                inboxContainer.startAnimation(shakeAnim);
                inboxBadge.setTag("animated");
            }
        } else {
            inboxBadge.setVisibility(View.GONE);
            inboxBadge.setTag(null);
        }
    }

    // HomeFragment.java의 loadDummyNotifications() 메서드만 수정

    private void loadDummyNotifications() {
        List<NotificationDTO> todayNotifs = new ArrayList<>();

        // 1. 스케줄 초대 알림 (전주)
        todayNotifs.add(NotificationDTO.createScheduleInvite(
                "1",
                "전주",
                "여행 일정에 참여해 주세요",
                "9분전",
                false,
                2,
                R.drawable.ic_schedule,
                "dummy_user_1" // ✅ userId 추가
        ));

        // 2. 팔로우 알림
        todayNotifs.add(NotificationDTO.createFollow(
                "2",
                "위찬우",
                "14분전",
                false,
                R.drawable.ic_user_add,
                "dummy_user_2" // ✅ userId 추가 (실제로는 Firestore userId 사용)
        ));

        // 3. 댓글 알림
        todayNotifs.add(NotificationDTO.createComment(
                "3",
                "위찬우",
                "댓글을 확인해 주세요",
                "19분전",
                false,
                2,
                R.drawable.ic_comment,
                "dummy_user_3" // ✅ userId 추가
        ));

        List<NotificationDTO> recentNotifs = new ArrayList<>();

        // 4. 스케줄 초대 알림 (부산)
        recentNotifs.add(NotificationDTO.createScheduleInvite(
                "4",
                "부산",
                "여행 일정에 참여해 주세요",
                "9일",
                true,
                0,
                R.drawable.ic_schedule,
                "dummy_user_4" // ✅ userId 추가
        ));

        // 5. 팔로우 알림
        recentNotifs.add(NotificationDTO.createFollow(
                "5",
                "이민섭",
                "14일",
                true,
                R.drawable.ic_user_add,
                "dummy_user_5" // ✅ userId 추가
        ));

        notificationManager.setTodayNotifications(todayNotifs);
        notificationManager.setRecentNotifications(recentNotifs);
    }

    /** 시/도 버튼 */
    private void setupProvinceButtons() {
        Button allButton = createRegionButton("전체", "ALL", true);
        allButton.setOnClickListener(v -> {
            selectedProvinceCode = "ALL";
            selectedCityCode = "";
            updateProvinceButtonStates(allButton);
            cityScrollView.setVisibility(View.GONE);
            filterAlbums();
        });
        provinceButtonContainer.addView(allButton);
        currentSelectedProvinceButton = allButton;

        List<RegionDataProvider.Region> provinces = RegionDataProvider.getProvinces();
        for (RegionDataProvider.Region province : provinces) {
            Button button = createRegionButton(province.getName(), province.getCode(), false);
            button.setOnClickListener(v -> {
                selectedProvinceCode = province.getCode();
                selectedCityCode = "";
                updateProvinceButtonStates(button);
                setupCityButtons(province.getCode());
                filterAlbums();
            });
            provinceButtonContainer.addView(button);
        }
    }

    /** 시군구 버튼 */
    private void setupCityButtons(String provinceCode) {
        cityButtonContainer.removeAllViews();
        currentSelectedCityButton = null;

        List<RegionDataProvider.Region> cities = RegionDataProvider.getCities(provinceCode);
        if (cities == null || cities.isEmpty()) {
            cityScrollView.setVisibility(View.GONE);
            return;
        }
        cityScrollView.setVisibility(View.VISIBLE);

        Button allCityButton = createRegionButton("전체", "", true);
        allCityButton.setOnClickListener(v -> {
            selectedCityCode = "";
            updateCityButtonStates(allCityButton);
            filterAlbums();
        });
        cityButtonContainer.addView(allCityButton);
        currentSelectedCityButton = allCityButton;

        for (RegionDataProvider.Region city : cities) {
            Button button = createRegionButton(city.getName(), city.getCode(), false);
            button.setOnClickListener(v -> {
                selectedCityCode = city.getCode();
                updateCityButtonStates(button);
                filterAlbums();
            });
            cityButtonContainer.addView(button);
        }
    }

    /** 지역 버튼 생성 */
    private Button createRegionButton(String text, String regionCode, boolean isSelected) {
        Button button = new Button(getContext());
        button.setText(text);
        button.setTag(regionCode);
        button.setTextSize(14);
        button.setPadding(dpToPx(20), dpToPx(8), dpToPx(20), dpToPx(8));
        button.setAllCaps(false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(35)
        );
        params.setMargins(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));
        button.setLayoutParams(params);

        updateButtonAppearance(button, isSelected);
        return button;
    }

    /** 버튼 외형 업데이트 */
    private void updateButtonAppearance(Button button, boolean isSelected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dpToPx(18));
        if (isSelected) {
            drawable.setColor(Color.parseColor("#6366F1"));
            button.setTextColor(Color.WHITE);
        } else {
            drawable.setColor(Color.parseColor("#E5E7EB"));
            button.setTextColor(Color.parseColor("#6B7280"));
        }
        button.setBackground(drawable);
    }

    /** 시/도 버튼 상태 갱신 */
    private void updateProvinceButtonStates(Button selectedButton) {
        if (currentSelectedProvinceButton != null)
            updateButtonAppearance(currentSelectedProvinceButton, false);
        updateButtonAppearance(selectedButton, true);
        currentSelectedProvinceButton = selectedButton;
    }

    /** 시군구 버튼 상태 갱신 */
    private void updateCityButtonStates(Button selectedButton) {
        if (currentSelectedCityButton != null)
            updateButtonAppearance(currentSelectedCityButton, false);
        updateButtonAppearance(selectedButton, true);
        currentSelectedCityButton = selectedButton;
    }

    /** ✅ 지역별 게시글 필터 (인기순 적용) */
    private void filterAlbums() {
        if (viewModel == null) return;

        List<CommunityPostDTO> allPosts = viewModel.getAllPosts();
        List<CommunityPostDTO> filtered = new ArrayList<>();

        // 1단계: 지역 필터링
        if (selectedProvinceCode.equals("ALL")) {
            filtered.addAll(allPosts);
        } else if (selectedCityCode.isEmpty()) {
            for (CommunityPostDTO post : allPosts) {
                if (post.getProvinceCode().equals(selectedProvinceCode)) {
                    filtered.add(post);
                }
            }
        } else {
            for (CommunityPostDTO post : allPosts) {
                if (post.getProvinceCode().equals(selectedProvinceCode)
                        && post.getCityCode().equals(selectedCityCode)) {
                    filtered.add(post);
                }
            }
        }

        // 2단계: 인기순 정렬 (좋아요 수 내림차순)
        filtered.sort((o1, o2) -> Integer.compare(o2.getHeartCount(), o1.getHeartCount()));

        // 3단계: 어댑터 업데이트 (사용자 검색 결과 없이)
        if (communityAdapter != null) {
            communityAdapter.updateDataWithUsers(filtered, new ArrayList<>(), false, false);
        }
    }

    /** 메모리 RecyclerView */
    private void setupMemoryRecyclerView(RecyclerView memoryView) {
        memoryView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        List<HomeScheduleDTO> items = new ArrayList<>();
        items.add(new HomeScheduleDTO("회의", "2025.10.15", "Room 6-205", "12:00", "13:05", R.drawable.ic_location_point));
        items.add(new HomeScheduleDTO("스터디", "2025.10.20", "Room 3-102", "15:00", "17:00", R.drawable.ic_location_point));
        items.add(new HomeScheduleDTO("약속", "2025.10.22", "Room 7-301", "18:30", "20:00", R.drawable.ic_location_point));

        HomeScheduleAdapter scheduleAdapter = new HomeScheduleAdapter(items);
        memoryView.setAdapter(scheduleAdapter);

        androidx.recyclerview.widget.SnapHelper snapHelper = new androidx.recyclerview.widget.LinearSnapHelper();
        snapHelper.attachToRecyclerView(memoryView);
    }

    /** ✅ 커뮤니티 스타일 RecyclerView (수정됨) */
    private void setupCommunityStyleRecyclerView(RecyclerView albumView) {
        albumView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        List<CommunityPostDTO> initialData = new ArrayList<>();

        // ✅ CommunityAdapter 생성자 수정 (OnMoreUsersClickListener 추가)
        communityAdapter = new CommunityAdapter(
                initialData,
                (post, position) -> {
                    if (post != null) {
                        Toast.makeText(getContext(), post.getTitle() + " 상세보기", Toast.LENGTH_SHORT).show();
                    }
                },
                () -> {
                    // ✅ 홈화면에서는 더보기 버튼이 표시되지 않으므로 빈 구현
                    // 혹시 표시되더라도 아무 동작 안 함
                }
        );

        albumView.setAdapter(communityAdapter);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 화면으로 돌아올 때 뱃지 업데이트
        updateInboxBadge(notificationManager.getUnreadCount());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (viewModel != null) viewModel.destroy();
        if (notificationManager != null) {
            notificationManager.removeListener(this::updateInboxBadge);
        }
    }
}