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
import com.example.tot.Schedule.ScheduleDTO;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
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
    private RecyclerView reMemory;
    private LinearLayout noScheduleSection;

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
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_home);
        provinceButtonContainer = view.findViewById(R.id.provinceButtonContainer);
        cityButtonContainer = view.findViewById(R.id.cityButtonContainer);
        cityScrollView = view.findViewById(R.id.cityScrollView);
        profileImage = view.findViewById(R.id.profileImage);
        inboxContainer = view.findViewById(R.id.inbox_container);
        inboxBadge = view.findViewById(R.id.inbox_badge);
        reMemory = view.findViewById(R.id.re_memory);
        noScheduleSection = view.findViewById(R.id.noScheduleSection);
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

        // 다음 스케줄 새로고침
        loadNextScheduleWithAllItems();

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

        updateButtonAppearance(selectedButton, true);
        currentSelectedProvinceButton = selectedButton;
    }

    private void updateCityButtonStates(Button selectedButton) {
        if (currentSelectedCityButton != null)
            updateButtonAppearance(currentSelectedCityButton, false);

        updateButtonAppearance(selectedButton, true);
        currentSelectedCityButton = selectedButton;
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

    /**
     * ✅ 다음 스케줄 1개의 모든 일정 표시 (VERTICAL)
     */
    private void setupMemoryRecyclerView(RecyclerView memoryView) {
        alarmList = new ArrayList<>();
        alarmAdapter = new HomeAlarmAdapter(alarmList);

        // ✅ VERTICAL로 설정 (여러 일정을 세로로 나열)
        memoryView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        );
        memoryView.setAdapter(alarmAdapter);

        // 첫 로딩
        loadNextScheduleWithAllItems();
    }

    /**
     * ✅ 현재 시간 이후의 가장 가까운 스케줄 1개를 찾고,
     *    그 스케줄에 속한 모든 일정을 표시
     */
    private void loadNextScheduleWithAllItems() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            showNoSchedule();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Calendar now = Calendar.getInstance();
        Timestamp currentTime = new Timestamp(now.getTime());

        // 1️⃣ 사용자의 모든 스케줄 조회
        db.collection("user")
                .document(uid)
                .collection("schedule")
                .get()
                .addOnSuccessListener(scheduleSnapshot -> {
                    List<ScheduleDTO> allSchedules = new ArrayList<>();

                    for (DocumentSnapshot doc : scheduleSnapshot.getDocuments()) {
                        ScheduleDTO schedule = doc.toObject(ScheduleDTO.class);
                        if (schedule != null) {
                            allSchedules.add(schedule);
                        }
                    }

                    if (allSchedules.isEmpty()) {
                        showNoSchedule();
                        return;
                    }

                    // 2️⃣ 스케줄을 시작 날짜 기준으로 정렬
                    Collections.sort(allSchedules, (a, b) ->
                            a.getStartDate().compareTo(b.getStartDate())
                    );

                    // 3️⃣ 현재 시간 이후의 가장 가까운 스케줄 찾기
                    ScheduleDTO nextSchedule = null;
                    for (ScheduleDTO schedule : allSchedules) {
                        // 스케줄의 종료일이 현재 시간 이후인 경우
                        if (schedule.getEndDate().compareTo(currentTime) >= 0) {
                            nextSchedule = schedule;
                            break;
                        }
                    }

                    if (nextSchedule == null) {
                        showNoSchedule();
                        return;
                    }

                    // 4️⃣ 해당 스케줄의 모든 일정 로드
                    loadAllItemsFromSchedule(uid, nextSchedule);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "스케줄 불러오기 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    showNoSchedule();
                });
    }

    /**
     * ✅ 특정 스케줄의 모든 날짜에 있는 모든 일정을 로드
     */
    private void loadAllItemsFromSchedule(String uid, ScheduleDTO schedule) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 스케줄 기간 내 모든 날짜 생성
        List<String> dateList = generateDateList(schedule.getStartDate(), schedule.getEndDate());

        if (dateList.isEmpty()) {
            showNoSchedule();
            return;
        }

        List<HomeAlarmDTO> allItems = new ArrayList<>();
        final int[] remainingDates = {dateList.size()};

        // 각 날짜별로 일정 조회
        for (String dateKey : dateList) {
            db.collection("user")
                    .document(uid)
                    .collection("schedule")
                    .document(schedule.getScheduleId())
                    .collection("scheduleDate")
                    .document(dateKey)
                    .collection("scheduleItem")
                    .get()
                    .addOnSuccessListener(itemSnapshot -> {
                        for (DocumentSnapshot doc : itemSnapshot.getDocuments()) {
                            String title = doc.getString("title");
                            String placeName = doc.getString("placeName");
                            Timestamp startTime = doc.getTimestamp("startTime");
                            Timestamp endTime = doc.getTimestamp("endTime");

                            if (title != null && startTime != null && endTime != null) {
                                HomeAlarmDTO dto = new HomeAlarmDTO(
                                        schedule.getScheduleId(),
                                        doc.getId(),
                                        title,
                                        dateKey,
                                        placeName,
                                        startTime,
                                        endTime
                                );
                                allItems.add(dto);
                            }
                        }

                        remainingDates[0]--;

                        // 모든 날짜 조회 완료
                        if (remainingDates[0] == 0) {
                            displayScheduleItems(allItems);
                        }
                    })
                    .addOnFailureListener(e -> {
                        remainingDates[0]--;
                        if (remainingDates[0] == 0) {
                            displayScheduleItems(allItems);
                        }
                    });
        }
    }

    /**
     * ✅ 스케줄 기간 내 모든 날짜 생성 (yyyy-MM-dd)
     */
    private List<String> generateDateList(Timestamp start, Timestamp end) {
        List<String> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start.toDate());

        Date endDate = end.toDate();

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");

        while (!calendar.getTime().after(endDate)) {
            dates.add(sdf.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return dates;
    }

    /**
     * ✅ 일정 목록 표시 (날짜 + 시간 순 정렬)
     */
    private void displayScheduleItems(List<HomeAlarmDTO> items) {
        if (items.isEmpty()) {
            showNoSchedule();
            return;
        }

        // 날짜 + 시간 기준 정렬
        Collections.sort(items, (a, b) -> {
            int dateCompare = a.getDate().compareTo(b.getDate());
            if (dateCompare != 0) return dateCompare;
            return a.getStartTime().compareTo(b.getStartTime());
        });

        // UI 업데이트
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                alarmList.clear();
                alarmList.addAll(items);
                alarmAdapter.notifyDataSetChanged();

                reMemory.setVisibility(View.VISIBLE);
                noScheduleSection.setVisibility(View.GONE);
            });
        }
    }

    /**
     * ✅ 스케줄 없음 표시
     */
    private void showNoSchedule() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                alarmList.clear();
                alarmAdapter.notifyDataSetChanged();

                reMemory.setVisibility(View.GONE);
                noScheduleSection.setVisibility(View.VISIBLE);
            });
        }
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
        loadNextScheduleWithAllItems();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (viewModel != null) viewModel.destroy();
        if (notificationManager != null)
            notificationManager.removeListener(this::updateInboxBadge);
    }
}