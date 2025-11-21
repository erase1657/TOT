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

import com.example.tot.Community.CommunityAdapter;
import com.example.tot.Community.CommunityPostDTO;
import com.example.tot.Community.CommunityViewModel;
import com.example.tot.Notification.NotificationActivity;
import com.example.tot.Notification.NotificationDTO;
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
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
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
                if (communityAdapter != null) communityAdapter.updateData(posts);
            }

            @Override
            public void onDataAdded(List<CommunityPostDTO> posts) {
                if (communityAdapter != null) communityAdapter.addData(posts);
            }
        });
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
                R.drawable.ic_schedule
        ));

        // 2. 팔로우 알림
        todayNotifs.add(NotificationDTO.createFollow(
                "2",
                "위찬우",
                "14분전",
                false,
                R.drawable.ic_user_add
        ));

        // 3. 댓글 알림
        todayNotifs.add(NotificationDTO.createComment(
                "3",
                "위찬우",
                "댓글을 확인해 주세요",
                "19분전",
                false,
                2,
                R.drawable.ic_comment
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
                R.drawable.ic_schedule
        ));

        // 5. 팔로우 알림
        recentNotifs.add(NotificationDTO.createFollow(
                "5",
                "이민섭",
                "14일",
                true,
                R.drawable.ic_user_add
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

    /** 지역별 게시글 필터 */
    private void filterAlbums() {
        if (viewModel == null) return;

        List<CommunityPostDTO> allPosts = viewModel.getAllPosts();
        List<CommunityPostDTO> filtered = new ArrayList<>();

        if (selectedProvinceCode.equals("ALL")) {
            filtered.addAll(allPosts);
        } else if (selectedCityCode.isEmpty()) {
            for (CommunityPostDTO post : allPosts)
                if (post.getProvinceCode().equals(selectedProvinceCode))
                    filtered.add(post);
        } else {
            for (CommunityPostDTO post : allPosts)
                if (post.getProvinceCode().equals(selectedProvinceCode)
                        && post.getCityCode().equals(selectedCityCode))
                    filtered.add(post);
        }

        if (communityAdapter != null) communityAdapter.updateData(filtered);
    }

    /** 메모리 RecyclerView */
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
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        List<CommunityPostDTO> initialData = new ArrayList<>();
        communityAdapter = new CommunityAdapter(initialData, (post, position) -> {
            if (post != null)
                Toast.makeText(getContext(), post.getTitle() + " 상세보기", Toast.LENGTH_SHORT).show();
        });
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
        loadUserAlarms(alarmList, alarmAdapter);
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