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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.tot.Community.CommunityAdapter;
import com.example.tot.Community.CommunityPostDTO;
import com.example.tot.Community.CommunityViewModel;
import com.example.tot.Notification.NotificationActivity;
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
    private SwipeRefreshLayout swipeRefreshLayout;
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
        setupSwipeRefresh();
        setupProvinceButtons();
        setupMemoryRecyclerView(view.findViewById(R.id.re_memory));
        setupCommunityStyleRecyclerView(view.findViewById(R.id.re_album));
        setupProfileAndInbox();

        // 게시글 더미 로드 (테스트용)
        viewModel.loadDummyData();
        filterAlbums();

        // 🔥 Firestore 실시간 알림 사용 → 더미 알림 제거
        // loadDummyNotifications(); (삭제)
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_home);
        provinceButtonContainer = view.findViewById(R.id.provinceButtonContainer);
        cityButtonContainer = view.findViewById(R.id.cityButtonContainer);
        cityScrollView = view.findViewById(R.id.cityScrollView);
        profileImage = view.findViewById(R.id.profileImage);
        inboxContainer = view.findViewById(R.id.inbox_container);
        inboxBadge = view.findViewById(R.id.inbox_badge);
    }

    /** 새로고침 설정 */
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(android.R.color.holo_blue_bright),
                getResources().getColor(android.R.color.holo_green_light),
                getResources().getColor(android.R.color.holo_orange_light),
                getResources().getColor(android.R.color.holo_red_light)
        );

        swipeRefreshLayout.setOnRefreshListener(this::refreshHomeData);
    }

    /** 홈 데이터 새로고침 */
    private void refreshHomeData() {

        // 알림 새로고침
        if (notificationManager != null) {
            notificationManager.refresh();
        }

        // 게시글 더미 재로드
        if (viewModel != null) {
            viewModel.loadDummyData();
            filterAlbums();
        }

        // 애니메이션 후 완료
        swipeRefreshLayout.postDelayed(() -> {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getContext(), "새로고침 완료", Toast.LENGTH_SHORT).show();
        }, 1000);
    }

    private void initViewModel() {
        viewModel = new CommunityViewModel(new CommunityViewModel.DataCallback() {
            @Override
            public void onDataChanged(List<CommunityPostDTO> posts) {
                if (communityAdapter != null) {
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

        viewModel.setFilter(CommunityViewModel.FilterMode.POPULAR);
    }

    private void setupNotificationManager() {
        notificationManager = NotificationManager.getInstance();
        notificationManager.addListener(this::updateInboxBadge);
    }

    /** 프로필 및 수신함 */
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

        // 배지 배경
        GradientDrawable badgeBackground = new GradientDrawable();
        badgeBackground.setShape(GradientDrawable.RECTANGLE);
        badgeBackground.setColor(Color.parseColor("#FF4444"));
        badgeBackground.setCornerRadius(dpToPx(8));

        inboxBadge.setBackground(badgeBackground);
        inboxBadge.setClipToOutline(true);

        updateInboxBadge(notificationManager.getUnreadCount());
    }

    private void updateInboxBadge(int unreadCount) {
        if (inboxBadge == null) return;

        if (unreadCount > 0) {
            inboxBadge.setVisibility(View.VISIBLE);
            inboxBadge.setText(unreadCount > 10 ? "10+" : String.valueOf(unreadCount));

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

    /** 시·도 버튼 */
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

        Button allCityBtn = createRegionButton("전체", "", true);
        allCityBtn.setOnClickListener(v -> {
            selectedCityCode = "";
            updateCityButtonStates(allCityBtn);
            filterAlbums();
        });

        cityButtonContainer.addView(allCityBtn);
        currentSelectedCityButton = allCityBtn;

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

    private void updateProvinceButtonStates(Button selected) {
        if (currentSelectedProvinceButton != null)
            updateButtonAppearance(currentSelectedProvinceButton, false);

        updateButtonAppearance(selected, true);
        currentSelectedProvinceButton = selected;
    }

    private void updateCityButtonStates(Button selected) {
        if (currentSelectedCityButton != null)
            updateButtonAppearance(currentSelectedCityButton, false);

        updateButtonAppearance(selected, true);
        currentSelectedCityButton = selected;
    }

    /** 지역별 게시글 필터 */
    private void filterAlbums() {
        if (viewModel == null) return;

        List<CommunityPostDTO> allPosts = viewModel.getAllPosts();
        List<CommunityPostDTO> filtered = new ArrayList<>();

        if (selectedProvinceCode.equals("ALL")) {
            filtered.addAll(allPosts);
        } else if (selectedCityCode.isEmpty()) {
            for (CommunityPostDTO post : allPosts) {
                if (post.getProvinceCode().equals(selectedProvinceCode))
                    filtered.add(post);
            }
        } else {
            for (CommunityPostDTO post : allPosts) {
                if (post.getProvinceCode().equals(selectedProvinceCode)
                        && post.getCityCode().equals(selectedCityCode))
                    filtered.add(post);
            }
        }

        // 인기순 정렬
        filtered.sort((a, b) -> Integer.compare(b.getHeartCount(), a.getHeartCount()));

        if (communityAdapter != null) {
            communityAdapter.updateDataWithUsers(filtered, new ArrayList<>(), false, false);
        }
    }

    /** 메모리 일정 RecyclerView */
    private void setupMemoryRecyclerView(RecyclerView memoryView) {
        memoryView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        );

        List<HomeScheduleDTO> items = new ArrayList<>();
        items.add(new HomeScheduleDTO("회의", "2025.10.15", "Room 6-205", "12:00", "13:05", R.drawable.ic_location_point));
        items.add(new HomeScheduleDTO("스터디", "2025.10.20", "Room 3-102", "15:00", "17:00", R.drawable.ic_location_point));
        items.add(new HomeScheduleDTO("약속", "2025.10.22", "Room 7-301", "18:30", "20:00", R.drawable.ic_location_point));

        HomeScheduleAdapter adapter = new HomeScheduleAdapter(items);
        memoryView.setAdapter(adapter);

        androidx.recyclerview.widget.SnapHelper snapHelper =
                new androidx.recyclerview.widget.LinearSnapHelper();
        snapHelper.attachToRecyclerView(memoryView);
    }

    /** 커뮤니티 스타일 RecyclerView */
    private void setupCommunityStyleRecyclerView(RecyclerView albumView) {
        albumView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        );

        List<CommunityPostDTO> initial = new ArrayList<>();

        communityAdapter = new CommunityAdapter(
                initial,
                (post, pos) -> Toast.makeText(getContext(), post.getTitle() + " 상세보기", Toast.LENGTH_SHORT).show(),
                () -> {}
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
        updateInboxBadge(notificationManager.getUnreadCount());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (viewModel != null) viewModel.destroy();
        if (notificationManager != null)
            notificationManager.removeListener(this::updateInboxBadge);
    }
}
