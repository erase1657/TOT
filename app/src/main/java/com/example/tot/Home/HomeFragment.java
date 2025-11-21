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
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeFragment extends Fragment {

    // 지역 코드
    private String selectedProvinceCode = "ALL";
    private String selectedCityCode = "";
    private List<HomeAlarmDTO> alarmList;
    private HomeAlarmAdapter alarmAdapter;
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

    //알람 스케줄 리사이클러뷰
    private void setupMemoryRecyclerView(RecyclerView memoryView) {

        alarmList = new ArrayList<>();
        alarmAdapter = new HomeAlarmAdapter(alarmList);

        memoryView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        );
        memoryView.setAdapter(alarmAdapter);

        // 첫 로딩
        loadUserAlarms(alarmList, alarmAdapter);
    }
    private void loadUserAlarms(List<HomeAlarmDTO> items, HomeAlarmAdapter adapter) {

        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        db.collection("user")
                .document(uid)
                .collection("alarms")
                .get()
                .addOnSuccessListener(snapshot -> {

                    items.clear();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot) {

                        String scheduleId = doc.getString("scheduleId");
                        String scheduleItem = doc.getString("planId");   // 너가 저장할 때 planId로 저장하면 됨
                        String title = doc.getString("title");
                        String date = doc.getString("date");
                        String place = doc.getString("place");

                        Timestamp start = doc.getTimestamp("startTime");
                        Timestamp end = doc.getTimestamp("endTime");

                        HomeAlarmDTO dto = new HomeAlarmDTO(
                                scheduleId,
                                scheduleItem,
                                title,
                                date,
                                place,
                                start,
                                end
                        );

                        items.add(dto);
                    }
                    Collections.sort(items, (a, b) -> {

                        // 1️⃣ 날짜 먼저 비교
                        int dateCompare = a.getDate().compareTo(b.getDate());
                        if (dateCompare != 0) return dateCompare;

                        // 2️⃣ 날짜가 같으면 시간 비교
                        return a.getStartTime().compareTo(b.getStartTime());
                    });
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "알람 불러오기 실패: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
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
        loadUserAlarms(alarmList, alarmAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (viewModel != null) viewModel.destroy();
        if (notificationManager != null)
            notificationManager.removeListener(this::updateInboxBadge);
    }
}
